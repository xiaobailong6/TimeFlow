package com.lifeordered.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.core.date.LunarTable
import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.ui.theme.Glass
import com.lifeordered.ui.theme.glassSurface
import kotlinx.coroutines.flow.filter
import java.util.Calendar

/** 单个滚轮项高度；与三行视口共同保证上下各显示一行邻项。 */
private val WHEEL_ITEM_HEIGHT = 34.dp

/** 三行可见：上一项、当前项、下一项。 */
private val WHEEL_VIEWPORT_HEIGHT = WHEEL_ITEM_HEIGHT * 3

private val WHEEL_CONTENT_PADDING = WHEEL_ITEM_HEIGHT

/**
 * 滚轮日期选择器。
 *
 * ## 本轮的两处重做
 *
 * ### 1. 吸附（snap）
 *
 * 上一版滚动**完全不改变选中项**，只有点击才改。用户滑到 1 和 2 之间松手，
 * 轮盘停在半格上，高亮带里是「1 的下半截 + 2 的上半截」，看起来像选中了空白。
 *
 * 现在用 `snapshotFlow { isScrollInProgress }` 监听「手指离开」，
 * 停止时取**最靠近中线**的那一项，`animateScrollToItem` 吸过去。
 * 几何依据：`contentPadding = 63dp`、项高 34dp、视口 160dp，
 * 于是 `animateScrollToItem(i)` 恰好把第 i 项摆到视口正中，
 * 而滚动偏移 `offset >= 17px` 时中线已经更靠近下一项。
 *
 * ### 2. 农历直接选
 *
 * 上一版选农历要先选公历再换算，而换算函数有 bug（见 `LunarTable` 注释），
 * 结果永远提示「该日期在农历中不存在」。
 *
 * 现在农历模式下三个轮轮分别是「农历年 / 农历月（含闰月）/ 农历日」，
 * 月列表来自 [CalendarHelper.lunarMonthsOf]，日上限来自该月实际天数（29 或 30）。
 * **选出来的是什么就存什么**，公历只作为下方对照显示，不再参与选择。
 */
@Composable
fun WheelDatePicker(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onDateSelected: (year: Int, month: Int, day: Int, isLeapMonth: Boolean) -> Unit,
    isLunar: Boolean = false,
    initialLeapMonth: Boolean = false
) {
    var year by remember { mutableStateOf(initialYear) }
    var month by remember { mutableStateOf(initialMonth) }
    var day by remember { mutableStateOf(initialDay) }
    var isLeapMonth by remember { mutableStateOf(initialLeapMonth) }

    // 农历月份表：含闰月，按时间顺序。非农历模式不构建，避免无谓的 ICU 扫描。
    val lunarMonths = remember(isLunar, year) {
        if (isLunar) CalendarHelper.lunarMonthsOf(year) else emptyList()
    }

    // 年份切换后原来的闰月可能不存在（例如闰二月只在部分年份有），回落到同号平月
    LaunchedEffect(lunarMonths) {
        if (!isLunar || lunarMonths.isEmpty()) return@LaunchedEffect
        val exists = lunarMonths.any { it.month == month && it.isLeapMonth == isLeapMonth }
        if (!exists) {
            val fallbackIndex = lunarMonths.indexOfFirst { it.month == month }.takeIf { it >= 0 } ?: 0
            month = lunarMonths[fallbackIndex].month
            isLeapMonth = lunarMonths[fallbackIndex].isLeapMonth
            onDateSelected(year, month, day, isLeapMonth)
        }
    }

    val maxDay = if (isLunar) {
        remember(year, month, isLeapMonth) {
            CalendarHelper.lunarMonthDays(year, month, isLeapMonth)
        }
    } else {
        remember(year, month) {
            Calendar.getInstance().apply {
                clear()
                set(year, month - 1, 1)
            }.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
    }

    // 从大月切到小月时收敛 day：滚轮已按 maxDay 截断显示，
    // 若不同步状态，回调会把「看不见的 31 号」原样报给表单
    LaunchedEffect(maxDay) {
        if (day > maxDay) {
            day = maxDay
            onDateSelected(year, month, day, isLeapMonth)
        }
    }

    val yearItems = remember { (LunarTable.MIN_YEAR..LunarTable.MAX_YEAR).toList() }
    val gregorianMonthItems = remember { (1..12).toList() }
    val dayItems = remember(maxDay) { (1..maxDay).toList() }
    val lunarMonthIndexes = remember(lunarMonths) { lunarMonths.indices.toList() }

    Column(modifier = Modifier.fillMaxWidth()) {
        WheelSelectionSummary(
            year = year,
            month = month,
            day = day,
            isLeapMonth = isLeapMonth,
            isLunar = isLunar
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(WHEEL_VIEWPORT_HEIGHT)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WheelItem(
                modifier = Modifier.weight(1.2f),
                items = yearItems,
                selectedItem = year,
                labelOf = { "$it 年" },
                onItemSelected = { picked ->
                    year = picked
                    onDateSelected(year, month, day, isLeapMonth)
                }
            )

            if (isLunar) {
                // 月份轮盘给的是「索引」，标签才是有意义的「正月 / 闰二月」
                val selectedIndex = lunarMonths
                    .indexOfFirst { it.month == month && it.isLeapMonth == isLeapMonth }
                    .takeIf { it >= 0 } ?: 0
                WheelItem(
                    modifier = Modifier.weight(1f),
                    items = lunarMonthIndexes,
                    selectedItem = selectedIndex,
                    labelOf = { index ->
                        lunarMonths.getOrNull(index)?.let {
                            CalendarHelper.getLunarMonthName(it.month, it.isLeapMonth)
                        } ?: ""
                    },
                    onItemSelected = { index ->
                        lunarMonths.getOrNull(index)?.let { entry ->
                            month = entry.month
                            isLeapMonth = entry.isLeapMonth
                            onDateSelected(year, month, day, isLeapMonth)
                        }
                    }
                )
            } else {
                WheelItem(
                    modifier = Modifier.weight(1f),
                    items = gregorianMonthItems,
                    selectedItem = month,
                    labelOf = { "$it 月" },
                    onItemSelected = { picked ->
                        month = picked
                        onDateSelected(year, month, day, isLeapMonth)
                    }
                )
            }

            WheelItem(
                modifier = Modifier.weight(1f),
                items = dayItems,
                selectedItem = day.coerceAtMost(maxDay),
                labelOf = { value ->
                    if (isLunar) CalendarHelper.getLunarDayLabel(value) else "$value 日"
                },
                onItemSelected = { picked ->
                    day = picked
                    onDateSelected(year, month, day, isLeapMonth)
                }
            )
        }
    }
}

/**
 * 顶部对照条。
 *
 * 农历模式下这里是「农历 X 月 X 日 → 对应公历 Y 年 M 月 D 日」，
 * 公历模式下反过来。**不再出现「该日期在农历中不存在」**：
 * 农历轮盘只提供真实存在的月与日，换算不可能失败。
 */
@Composable
private fun WheelSelectionSummary(
    year: Int,
    month: Int,
    day: Int,
    isLeapMonth: Boolean,
    isLunar: Boolean
) {
    val solarFromLunar = remember(year, month, day, isLeapMonth, isLunar) {
        if (isLunar) CalendarHelper.l2s(year, month, day, isLeapMonth) else null
    }
    val lunarFromSolar = remember(year, month, day, isLunar) {
        if (isLunar) null else CalendarHelper.getLunarDate(year, month, day)
    }

    val title: String
    val subtitle: String
    val subtitleIsError: Boolean

    if (isLunar) {
        title = "农历 $year 年 ${CalendarHelper.getLunarMonthName(month, isLeapMonth)}" +
            "${CalendarHelper.getLunarDayLabel(day)}"
        subtitle = solarFromLunar?.let { solar ->
            "对应公历 ${solar.get(Calendar.YEAR)} 年 ${solar.get(Calendar.MONTH) + 1} 月 " +
                "${solar.get(Calendar.DAY_OF_MONTH)} 日"
        } ?: "正在换算…"
        subtitleIsError = solarFromLunar == null
    } else {
        title = "$year 年 $month 月 $day 日"
        subtitle = lunarFromSolar?.let { lunar ->
            val festival = CalendarHelper.getLunarFestivalOrDayName(lunar.month, lunar.day)
            "对应农历 $festival"
        } ?: "正在换算…"
        subtitleIsError = lunarFromSolar == null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .glassSurface(shape = Glass.fieldShape, fill = Glass.FillSubtle)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Glass.TextPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = if (subtitleIsError) Glass.AccentDanger else Glass.TextSecondary
            )
        }
    }
}

/**
 * 单个滚轮。
 *
 * ## 吸附是怎么算的
 *
 * 视口 160dp、项高 34dp、`contentPadding` 上下各 63dp。
 * 第 i 项的顶边在滚动偏移为 0 时位于 `63 + 34i`，中心是 `80 + 34i`，
 * 而视口中心恒为 80 —— 所以 **`animateScrollToItem(i)` 恰好让第 i 项居中**。
 *
 * 拖动过程中 `firstVisibleItemIndex = k`、`firstVisibleItemScrollOffset = o`，
 * 第 k 项的中心落在 `80 - o`；当 `o > 17px` 时中线已经更靠近第 k+1 项。
 * 这就是吸附目标的判据，也是「不会停在半格/空白行」的原因。
 */
@Composable
fun WheelItem(
    items: List<Int>,
    selectedItem: Int,
    modifier: Modifier = Modifier,
    suffix: String = "",
    labelOf: (Int) -> String = { value -> "$value$suffix" },
    onItemSelected: (Int) -> Unit
) {
    val itemHeightPx = with(LocalDensity.current) { WHEEL_ITEM_HEIGHT.toPx() }
    val initialIndex = remember(items) { items.indexOf(selectedItem).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    // 滚动中也要实时高亮：用中线判据算「当前哪一项在正中间」
    val centeredIndex by remember(items, itemHeightPx) {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val raw = if (offset > itemHeightPx / 2f) first + 1 else first
            raw.coerceIn(0, maxOf(items.lastIndex, 0))
        }
    }

    // 回调与数据都可能随重组变化，用 rememberUpdatedState 避免 LaunchedEffect 里闭包陈旧
    val currentOnItemSelected by rememberUpdatedState(onItemSelected)
    val currentItems by rememberUpdatedState(items)
    val currentSelectedItem by rememberUpdatedState(selectedItem)

    // 手指离开后吸附到最近的一项
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { inProgress -> !inProgress }
            .collect {
                val first = listState.firstVisibleItemIndex
                val offset = listState.firstVisibleItemScrollOffset
                val itemsNow = currentItems
                if (itemsNow.isEmpty()) return@collect
                val raw = if (offset > itemHeightPx / 2f) first + 1 else first
                val target = raw.coerceIn(0, itemsNow.lastIndex)

                // 只有真的没对齐才滚。
                // snapshotFlow 的**首帧就是 false**，若在这里无条件 animateScrollToItem，
                // 它自己又会把 isScrollInProgress 翻成 true→false，反过来再触发本收集器，
                // 形成自激循环（空转 + 每帧回调一次 onItemSelected）。
                if (target != first || offset != 0) {
                    listState.animateScrollToItem(target)
                }

                // 值没变就不回调，避免把「同一次吸附」上报成多次状态变更
                val picked = itemsNow[target]
                if (picked != currentSelectedItem) {
                    currentOnItemSelected(picked)
                }
            }
    }

    // 外部状态变化时同步滚轮位置（点击、收敛日上限、切换年份都会走这里）
    LaunchedEffect(selectedItem, items) {
        val index = items.indexOf(selectedItem)
        if (index < 0) return@LaunchedEffect
        val alreadyThere = listState.firstVisibleItemIndex == index &&
            listState.firstVisibleItemScrollOffset == 0
        if (alreadyThere) return@LaunchedEffect
        listState.animateScrollToItem(index)
    }

    Box(modifier = modifier.height(WHEEL_VIEWPORT_HEIGHT)) {
        // 中间选中高亮带
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(WHEEL_ITEM_HEIGHT)
                 .glassSurface(shape = Glass.pillShape, fill = Glass.FillControl)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = WHEEL_CONTENT_PADDING)
        ) {
            itemsIndexed(items) { index, item ->
                val isSelected = index == centeredIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WHEEL_ITEM_HEIGHT)
                        // 点击状态层必须裁剪到与中间选中栏相同的胶囊轮廓。
                        .clip(Glass.pillShape)
                        .clickable { currentOnItemSelected(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelOf(item),
                        fontSize = if (isSelected) 15.sp else 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Glass.Accent else Glass.TextTertiary
                    )
                }
            }
        }
    }
}
