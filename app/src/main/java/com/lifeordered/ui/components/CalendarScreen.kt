package com.lifeordered.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: LifeOrderedViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val moments by viewModel.allMoments.collectAsState()
    val consumables by viewModel.allConsumables.collectAsState()
    val filterType by viewModel.calendarFilterType

    // Keep track of the month currently displayed
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) } // 1-indexed (1-12)

    // Currently selected day in Gregorian (1-indexed)
    var selectedDay by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    // Re-evaluate events map for the selected month dynamically when moments/month changes
    val eventsMap = remember(moments, consumables, currentYear, currentMonth, filterType) {
        viewModel.getEventsMapForMonth(currentYear, currentMonth, filterType)
    }

    val selectedDateKey = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, selectedDay)
    val displayedEvents = eventsMap[selectedDateKey] ?: emptyList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBg)
    ) {
        // 1. Cozy Soft Header Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "时光之历",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            
            // Subtitle indicating the current selected info
            Text(
                text = "今日拾光流年 ⏳",
                fontSize = 13.sp,
                color = GrayTextMuted,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(null to "全部", "moment" to "重要时刻", "consumable" to "耗材到期")
            filters.forEach { (type, label) ->
                val isSelected = filterType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) WarmOrange else Color.White)
                        .border(1.dp, if (isSelected) WarmOrange else LightCream, RoundedCornerShape(12.dp))
                        .clickable { viewModel.calendarFilterType.value = type }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else TextDark
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 2. Month Selector Navigation Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentMonth == 1) {
                                currentMonth = 12
                                currentYear--
                            } else {
                                currentMonth--
                            }
                            selectedDay = 1 // default to 1st of month on change
                        },
                        modifier = Modifier.testTag("prev_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "上个月",
                            tint = OrangePrimaryText
                        )
                    }

                    Text(
                        text = "$currentYear 年 $currentMonth 月",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    IconButton(
                        onClick = {
                            if (currentMonth == 12) {
                                currentMonth = 1
                                currentYear++
                            } else {
                                currentMonth++
                            }
                            selectedDay = 1 // default to 1st of month on change
                        },
                        modifier = Modifier.testTag("next_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "下个月",
                            tint = OrangePrimaryText
                        )
                    }
                }
            }

            // 3. Calendar Monthly Grid View
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = WarmOrange.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Weekday labels: Mo, Tu, We, Th, Fr, Sa, Su
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
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (label == "六" || label == "日") OrangePrimaryText else GrayTextMuted,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = WarmCreamBg,
                            thickness = 1.dp
                        )

                        // Compute calendar days
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, currentYear)
                            set(Calendar.MONTH, currentMonth - 1)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        // Monday=0, Tuesday=1 ... Sunday=6
                        val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                        var cellIndex = 0
                        val totalRows = (firstDayOfWeek + totalDays + 6) / 7

                        for (r in 0 until totalRows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (c in 0..6) {
                                    val dayNum = cellIndex - firstDayOfWeek + 1
                                    if (dayNum in 1..totalDays) {
                                        val isSelected = selectedDay == dayNum
                                        
                                        // Lunar date processing
                                        val lunarPair = CalendarHelper.getLunarDate(currentYear, currentMonth, dayNum)
                                        val lunarText = if (lunarPair != null) {
                                            CalendarHelper.getLunarFestivalOrDayName(lunarPair.first, lunarPair.second)
                                        } else {
                                            ""
                                        }

                                        // Has events indicator
                                        val dateKey = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, dayNum)
                                        val hasEvents = eventsMap.containsKey(dateKey)

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(0.9f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) WarmOrange else Color.Transparent
                                                )
                                                .clickable {
                                                    selectedDay = dayNum
                                                }
                                                .padding(2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else TextDark
                                            )
                                            
                                            Text(
                                                text = lunarText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isSelected) Color.White.copy(alpha = 0.85f) else GrayTextMuted,
                                                maxLines = 1
                                            )

                                            if (hasEvents) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) Color.White else WarmOrange)
                                                )
                                            }
                                        }
                                    } else {
                                        // Empty cell for calendar margins
                                        Box(modifier = Modifier.weight(1f))
                                    }
                                    cellIndex++
                                }
                            }
                        }
                    }
                }
            }

            // 4. Section Header for Daily events
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(WarmOrange.copy(alpha = 0.15f))
                            .wrapContentSize(Alignment.Center)
                    ) {
                        Text("🗒️", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "这一天的安排",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }

            // 5. Events List Content
            if (displayedEvents.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                            .shadow(4.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🍵",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "这里目前很安静哦~",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = GrayTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
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
                                Toast.makeText(context, "🎉 已同步记录 [${item.title}]！", Toast.LENGTH_SHORT).show()
                            } else if (item is com.lifeordered.data.models.Consumable) {
                                viewModel.resetConsumable(item)
                                Toast.makeText(context, "🔄 ${item.name} 已重置！", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDelete = {
                            if (item is Moment) {
                                viewModel.moveToTrash(item)
                                Toast.makeText(context, "🗑️ 已移入回收站 [${item.title}]", Toast.LENGTH_SHORT).show()
                            } else if (item is com.lifeordered.data.models.Consumable) {
                                viewModel.moveToTrash(item)
                                Toast.makeText(context, "🗑️ 已移入回收站 [${item.name}]", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Custom High-fidelity event row item styled for Cream Theme
 */
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
        is Moment -> if (item.isLunar) "农历 ${item.month}月${item.day}日" else "公历 ${item.month}月${item.day}日"
        is com.lifeordered.data.models.Consumable -> "耗材寿命到期 🕒"
        else -> ""
    }

    val cardColor = if (item is Moment) {
        when (item.cardColorIndex) {
            0 -> PastelPeach
            1 -> PastelBlue
            2 -> PastelPink
            else -> PastelYellow
        }
    } else {
        PastelBlue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .border(2.dp, Color(0xFFE6D7C3), RoundedCornerShape(16.dp))
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .wrapContentSize(Alignment.Center)
            ) {
                Text(
                    text = if (item is Moment) (if (item.isLunar) "🏮" else "📅") else "🕒",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = GrayTextMuted
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CreamCheckButton(
                    onClick = onComplete,
                    icon = if (item is Moment) Icons.Default.Check else Icons.Default.Refresh
                )

                CreamDeleteButton(onClick = onDelete)
            }
        }
    }
}
