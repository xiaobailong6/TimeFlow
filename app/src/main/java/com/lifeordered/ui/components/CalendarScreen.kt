package com.lifeordered.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.format.MomentPresentation
import com.lifeordered.ui.theme.Glass
import com.lifeordered.ui.theme.GlassCard
import com.lifeordered.ui.theme.GlassChip
import com.lifeordered.ui.theme.GlassEmptyState
import com.lifeordered.ui.theme.GlassIconButton
import com.lifeordered.ui.theme.GlassScreenHeader
import com.lifeordered.ui.theme.accentGlow
import com.lifeordered.ui.theme.glassSurface
import com.lifeordered.ui.theme.liquidGlass
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel
import java.util.Calendar
import java.util.Locale

/**
 * 日程页（液态玻璃版）。
 *
 * 月历是这里唯一需要「密集信息」的组件，所以刻意压低对比度：
 * 日期数字用次级文字色，只有**被选中的那天**用强调色实心块，
 * 农历文字降到 8sp 并进一步减淡——让网格读起来像一层纹理，
 * 而不是一堵数字墙。
 */
@Composable
fun CalendarScreen(
    viewModel: LifeOrderedViewModel,
    modifier: Modifier = Modifier
) {
    val notify = rememberNotifier()
    val moments by viewModel.allMoments.collectAsState()
    val consumables by viewModel.allConsumables.collectAsState()

    var filterType by remember { mutableStateOf<String?>(null) }
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var selectedDay by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    val eventsMap = remember(moments, consumables, currentYear, currentMonth, filterType) {
        viewModel.getEventsMapForMonth(currentYear, currentMonth, filterType)
    }

    val selectedDateKey = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, selectedDay)
    val displayedEvents = eventsMap[selectedDateKey] ?: emptyList()

    Column(modifier = modifier.fillMaxSize()) {
        GlassScreenHeader(
            title = "时光之历",
            subtitle = "$currentYear 年 $currentMonth 月"
        )

        // 筛选
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                null to "全部",
                "moment" to "重要时刻",
                "consumable" to "耗材到期"
            )
            filters.forEach { (type, label) ->
                GlassChip(
                    label = label,
                    selected = filterType == type,
                    onClick = { filterType = type }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp)
        ) {
            // 月份切换
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .glassSurface(shape = Glass.pillShape, fill = Glass.FillSubtle)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "上个月",
                        onClick = {
                            if (currentMonth == 1) {
                                currentMonth = 12
                                currentYear--
                            } else {
                                currentMonth--
                            }
                            selectedDay = 1
                        },
                        modifier = Modifier.testTag("prev_month_btn"),
                        tint = Glass.Accent,
                        size = 32.dp,
                        iconSize = 18.dp
                    )

                    Text(
                        text = "$currentYear 年 $currentMonth 月",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Glass.TextPrimary,
                        letterSpacing = (-0.2).sp
                    )

                    GlassIconButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "下个月",
                        onClick = {
                            if (currentMonth == 12) {
                                currentMonth = 1
                                currentYear++
                            } else {
                                currentMonth++
                            }
                            selectedDay = 1
                        },
                        modifier = Modifier.testTag("next_month_btn"),
                        tint = Glass.Accent,
                        size = 32.dp,
                        iconSize = 18.dp
                    )
                }
            }

            // 月历网格
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = Glass.panelShape,
                    fill = Glass.Fill,
                    elevation = 14.dp,
                    glow = Glass.GlowCyan,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            weekLabels.forEach { label ->
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (label == "六" || label == "日") {
                                        Glass.Accent.copy(alpha = 0.75f)
                                    } else {
                                        Glass.TextTertiary
                                    },
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, currentYear)
                            set(Calendar.MONTH, currentMonth - 1)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val totalRows = (firstDayOfWeek + totalDays + 6) / 7

                        var cellIndex = 0
                        for (r in 0 until totalRows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (c in 0..6) {
                                    val dayNum = cellIndex - firstDayOfWeek + 1
                                    if (dayNum in 1..totalDays) {
                                        DayCell(
                                            year = currentYear,
                                            month = currentMonth,
                                            day = dayNum,
                                            isSelected = selectedDay == dayNum,
                                            hasEvents = eventsMap.containsKey(
                                                String.format(
                                                    Locale.US,
                                                    "%04d-%02d-%02d",
                                                    currentYear,
                                                    currentMonth,
                                                    dayNum
                                                )
                                            ),
                                            onClick = { selectedDay = dayNum }
                                        )
                                    } else {
                                        Box(modifier = Modifier.weight(1f))
                                    }
                                    cellIndex++
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "这一天的安排",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Glass.TextPrimary,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                )
            }

            if (displayedEvents.isEmpty()) {
                item {
                    GlassEmptyState(
                        glyph = "🍵",
                        title = "这一天没有安排",
                        hint = "选另一天看看，或者新增一条时刻记录"
                    )
                }
            } else {
                items(displayedEvents, key = {
                    when (it) {
                        is Moment -> "m_${it.id}"
                        is com.lifeordered.data.models.Consumable -> "c_${it.id}"
                        else -> it.hashCode()
                    }
                }) { item ->
                    EventRowItem(
                        item = item,
                        onComplete = {
                            if (item is Moment) {
                                viewModel.completeMoment(item)
                                notify("已记录 ${item.title}")
                            } else if (item is com.lifeordered.data.models.Consumable) {
                                viewModel.resetConsumable(item)
                                notify("${item.name} 使用周期已重置")
                            }
                        },
                        onDelete = {
                            if (item is Moment) {
                                viewModel.moveToTrash(item)
                                notify("已移到回收站")
                            } else if (item is com.lifeordered.data.models.Consumable) {
                                viewModel.moveToTrash(item)
                                notify("已移到回收站")
                            }
                        }
                    )
                }
            }
        }
    }
}

/** 单个日期格。 */
@Composable
private fun RowScope.DayCell(
    year: Int,
    month: Int,
    day: Int,
    isSelected: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit
) {
    val lunarDate = CalendarHelper.getLunarDate(year, month, day)
    val lunarText = lunarDate?.let {
        CalendarHelper.getLunarFestivalOrDayName(it.month, it.day)
    } ?: ""

    Column(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(0.82f)
            .then(
                if (isSelected) {
                    Modifier
                        .accentGlow(Glass.Accent, shape = Glass.controlShape, elevation = 6.dp)
                        .clip(Glass.controlShape)
                        .background(Glass.Accent)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.toString(),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Glass.OnAccent else Glass.TextPrimary
        )
        if (lunarText.isNotEmpty()) {
            Text(
                text = lunarText,
                fontSize = 8.sp,
                color = if (isSelected) {
                    Glass.OnAccent.copy(alpha = 0.7f)
                } else {
                    Glass.TextTertiary
                },
                maxLines = 1
            )
        }
        if (hasEvents) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Glass.OnAccent else Glass.Accent)
            )
        }
    }
}

/** 单条日程事件行。 */
@Composable
fun EventRowItem(
    item: Any,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val title = when (item) {
        is Moment -> item.title
        is com.lifeordered.data.models.Consumable -> item.name
        else -> "未知项"
    }

    val subtitle = when (item) {
        is Moment -> MomentPresentation.listSubtitle(item)
        is com.lifeordered.data.models.Consumable -> "耗材寿命到期"
        else -> ""
    }

    val accent = when (item) {
        is Moment -> parseThemeAccent(item)
        is com.lifeordered.data.models.Consumable -> Glass.AccentCool
        else -> Glass.Accent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .liquidGlass(
                shape = Glass.cardShape,
                fill = Glass.Fill,
                elevation = 8.dp,
                glow = accent
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .glassSurface(shape = CircleShape, fill = Glass.FillSubtle),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (item) {
                    is Moment -> MomentPresentation.calendarGlyph(item)
                    else -> "🕒"
                },
                fontSize = 17.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Glass.TextPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Glass.TextSecondary,
                maxLines = 1
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlassCheckButton(
                onClick = onComplete,
                icon = if (item is Moment) Icons.Default.Check else Icons.Default.Refresh,
                containerSize = 30.dp,
                iconSize = 14.dp
            )
            GlassDeleteButton(
                onClick = onDelete,
                containerSize = 30.dp,
                iconSize = 14.dp
            )
        }
    }
}

/** 取时刻的主题色作为辉光；异常值回落到强调色。 */
private fun parseThemeAccent(moment: Moment): Color =
    com.lifeordered.ui.theme.parseHexColor(moment.themeColorHex)
