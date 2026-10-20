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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.ui.theme.Glass
import com.lifeordered.ui.theme.glassSurface

/**
 * 滚轮日期选择器（含农历辅助）。
 *
 * ## 重构补上的缺口
 *
 * 原实现只有三个纯数字滚轮，选农历生日时用户必须**自己知道**「八月十五」
 * 对应 8 和 15——但农历月份本身有大月（30 天）小月（29 天），
 * 盲选 31 号会得到一个不存在的日期。
 *
 * 现在做了两件事：
 * 1. 顶部实时显示当前滚轮位置的**公历 ↔ 农历对照**
 * 2. 日滚轮按日历实际长度收敛上限，非法组合会提示「该日期不存在」
 */
@Composable
fun WheelDatePicker(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onDateSelected: (Int, Int, Int) -> Unit,
    isLunar: Boolean = false
) {
    var year by remember { mutableStateOf(initialYear) }
    var month by remember { mutableStateOf(initialMonth) }
    var day by remember { mutableStateOf(initialDay) }

    // 农历月最多 30 天；公历按实际月份长度
    val maxDay = if (isLunar) {
        30
    } else {
        remember(year, month) {
            java.util.Calendar.getInstance().apply {
                clear()
                set(year, month - 1, 1)
            }.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        }
    }

    // 从大月切到小月时收敛 day：滚轮已按 maxDay 截断显示，
    // 若不同步状态，回调会把「看不见的 31 号」原样报给表单
    LaunchedEffect(maxDay) {
        if (day > maxDay) {
            day = maxDay
            onDateSelected(year, month, day)
        }
    }

    // 选中日期对应的农历，用于对照显示
    val lunarOfSelection = if (isLunar) {
        CalendarHelper.l2s(year, month, day, isLeapMonth = false)?.let { solar ->
            CalendarHelper.getLunarDate(
                solar.get(java.util.Calendar.YEAR),
                solar.get(java.util.Calendar.MONTH) + 1,
                solar.get(java.util.Calendar.DAY_OF_MONTH)
            )
        }
    } else {
        remember(year, month, day) { CalendarHelper.getLunarDate(year, month, day) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 对照提示
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .glassSurface(shape = Glass.fieldShape, fill = Glass.FillSubtle)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = if (isLunar) {
                        "农历 $year 年 $month 月 $day 日"
                    } else {
                        "$year 年 $month 月 $day 日"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Glass.TextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = lunarOfSelection?.let { lunar ->
                        val monthName = CalendarHelper.getLunarDayName(lunar.month, lunar.day)
                        val festival = CalendarHelper.getLunarFestivalOrDayName(lunar.month, lunar.day)
                        if (festival != monthName) {
                            "对应农历 $festival"
                        } else {
                            "对应农历 ${lunar.month} 月 $monthName"
                        }
                    } ?: "该日期在农历中不存在，请调整",
                    fontSize = 11.sp,
                    color = if (lunarOfSelection == null) Glass.AccentDanger else Glass.TextSecondary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WheelItem(
                modifier = Modifier.weight(1.2f),
                items = (1900..2100).toList(),
                selectedItem = year,
                suffix = "年",
                onItemSelected = {
                    year = it
                    onDateSelected(year, month, day)
                }
            )
            WheelItem(
                modifier = Modifier.weight(1f),
                items = (1..12).toList(),
                selectedItem = month,
                suffix = "月",
                onItemSelected = {
                    month = it
                    onDateSelected(year, month, day)
                }
            )
            WheelItem(
                modifier = Modifier.weight(1f),
                items = (1..maxDay).toList(),
                selectedItem = day.coerceAtMost(maxDay),
                suffix = "日",
                onItemSelected = {
                    day = it
                    onDateSelected(year, month, day)
                }
            )
        }
    }
}

/**
 * 单个滚轮。
 *
 * 用 LazyColumn 而不是一次性 Column：年份滚轮有 200 项，
 * 全量组合会在弹层弹出瞬间掉帧。
 */
@Composable
fun WheelItem(
    items: List<Int>,
    selectedItem: Int,
    modifier: Modifier = Modifier,
    suffix: String = "",
    onItemSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    )

    Box(modifier = modifier.height(160.dp)) {
        // 中间选中高亮带
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(34.dp)
                .glassSurface(shape = Glass.controlShape, fill = Glass.FillStrong)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 63.dp)
        ) {
            items(items) { item ->
                val isSelected = item == selectedItem
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clickable { onItemSelected(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$item$suffix",
                        fontSize = if (isSelected) 15.sp else 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Glass.Accent else Glass.TextTertiary
                    )
                }
            }
        }
    }
}
