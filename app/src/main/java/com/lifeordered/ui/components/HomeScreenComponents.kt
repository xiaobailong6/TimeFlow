package com.lifeordered.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.lifeordered.R
import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TopHeaderSection(greeting: String, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "拾光流年",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextDark
            )
            Text(
                text = greeting,
                fontSize = 12.sp,
                color = GrayTextMuted
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(WarmOrange.copy(alpha = 0.1f))
                .border(2.dp, WarmOrange.copy(alpha = 0.2f), CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🧁", fontSize = 22.sp)
        }
    }
}

@Composable
fun OrganicBannerBlock(itemCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(104.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x0A000000),
                spotColor = Color(0x14000000)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFFF1E6), Color(0xFFFFE4D1))
                )
            )
    ) {
        // Decorative warm circles
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 16.dp, y = 16.dp)
                .size(96.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF7E40).copy(alpha = 0.1f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-16).dp, y = (-16).dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF7E40).copy(alpha = 0.05f))
        )

        // Nice cozy overlay image
        Image(
            painter = painterResource(id = R.drawable.banner_cozy_clay),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(180.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.65f
        )

        // Text labels inside the banner
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "让每一天都有迹可循",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF7E40)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "今天还有 $itemCount 件事项待处理",
                fontSize = 12.sp,
                color = Color(0xFF8A7D73),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: (@Composable () -> Unit)? = null,
    onAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
        
        TextButton(
            onClick = onAllClick,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "全部 >",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GrayTextMuted
            )
        }
    }
}

@Composable
fun EmptyStateBlock(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .height(110.dp)
            .border(2.dp, Color(0xFFE8E5E0), RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Text("📭", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = GrayTextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GlobalEmptyStateBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✨", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "一切都是全新的开始",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Text(
            text = "点击下方按钮记录下生活的第一份秩序吧",
            fontSize = 12.sp,
            color = GrayTextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun CreamEditButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerSize: Dp = 22.dp,
    iconSize: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .size(containerSize)
            .shadow(2.dp, CircleShape)
            .background(Color(0xFFFFF3E0), CircleShape)
            .border(1.dp, Color(0xFFFFE0B2), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = "编辑",
            tint = Color(0xFFF57C00),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun CreamDeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerSize: Dp = 22.dp,
    iconSize: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .size(containerSize)
            .shadow(2.dp, CircleShape)
            .background(Color(0xFFFEECEB), CircleShape)
            .border(1.dp, Color(0xFFFFCDD2), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "删除",
            tint = Color(0xFFE53935),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun CreamCheckButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    containerSize: Dp = 22.dp,
    iconSize: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .size(containerSize)
            .shadow(2.dp, CircleShape)
            .background(Color(0xFFF1F8E9), CircleShape)
            .border(1.dp, Color(0xFFDCEDC8), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "完成",
            tint = Color(0xFF689F38),
            modifier = Modifier.size(iconSize)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MomentCardItem(
    moment: Moment,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var showNotesDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }

    val baseColor = parseHexColor(moment.themeColorHex)
    val daysRemaining = CalendarHelper.calculateDays(moment)
    val isRecurring = moment.repeatType != "none"
    val isCountUp = moment.type == "countUp"

    AnimatedVisibility(
        visible = isVisible,
        exit = fadeOut(animationSpec = tween(durationMillis = 800)) + shrinkHorizontally()
    ) {
        Card(
            modifier = Modifier
                .width(180.dp)
                .fillMaxHeight()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = baseColor.copy(alpha = 0.2f),
                    spotColor = baseColor.copy(alpha = 0.3f)
                )
                .testTag("moment_item_${moment.id}")
                .combinedClickable(
                    onClick = {
                        if (moment.notes?.isNotBlank() == true) {
                            showNotesDialog = true
                        }
                    },
                    onLongClick = {
                        onArchive()
                    }
                ),
            colors = CardDefaults.cardColors(containerColor = baseColor),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Header with Title and Category Icon Backdrop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = moment.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1
                        )
                        Text(
                            text = if (isCountUp) "已安稳度过" else "正在加速到来",
                            fontSize = 9.sp,
                            color = GrayTextMuted
                        )
                    }

                    // Action Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CreamEditButton(onClick = onEdit)
                        CreamDeleteButton(onClick = onDelete)
                    }
                }

                // 2. Icon + Remaining Days Display (Central Visual)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Clay Icon Box
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(baseColor.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (moment.cardColorIndex) {
                            0 -> ClayCakeIcon()
                            1 -> ClayGiftIcon()
                            2 -> ClayHeartsIcon()
                            3 -> ClayPartyIcon()
                            else -> Text(text = moment.iconPath.ifBlank { "🌟" }, fontSize = 28.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Days Text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$daysRemaining",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isCountUp) TextDark else OrangePrimaryText
                        )
                        Text(
                            text = "DAYS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrayTextMuted,
                            letterSpacing = 1.sp
                        )
                    }
                }

                        // 3. Footer Action (Check-in)
                Column(modifier = Modifier.fillMaxWidth()) {
                    val dateLabel = if (moment.isLunar) {
                       val solar = CalendarHelper.l2s(moment.year, moment.month, moment.day, moment.isLeapMonth)
                       val solarText = solar?.let { " (公历 ${(it.get(Calendar.MONTH) + 1).toString().padStart(2, '0')}/${it.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')})" } ?: ""
                       "农历 ${if (moment.isLeapMonth) "闰" else ""}${moment.month}/${moment.day}$solarText"
                    } else {
                       "公历 ${moment.month}/${moment.day}"
                    }
                    Text(
                        text = dateLabel,
                        fontSize = 10.sp,
                        color = GrayTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onComplete()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .testTag("moment_complete_${moment.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                    ) {
                        Text(
                            text = if (moment.completionCount > 0) "✓ 已达成 (第 ${moment.completionCount} 次)" else "✓ 已达成",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimaryText
                        )
                    }
                }
            }
        }
    }

    // Modal popup event memo dialog
    if (showNotesDialog && !moment.notes.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💡 ", fontSize = 18.sp)
                    Text(text = moment.title, fontWeight = FontWeight.Bold, color = TextDark)
                }
            },
            text = {
                Column {
                    Text(
                        text = moment.notes ?: "",
                        fontSize = 14.sp,
                        color = TextDark,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNotesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmOrange)
                ) {
                    Text("知道了", color = Color.White)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun ConsumableCardItem(
    consumable: Consumable,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val remainingDays = consumable.getRemainingDays()
    val isCritical = remainingDays <= 3
    val baseColor = if (isCritical) Color(0xFFF44336) else Color(0xFF2196F3)

    // Expiry date calculation
    val expiryTime = CalendarHelper.addLocalDays(consumable.startDateMillis, consumable.totalDays)
    val sdf = SimpleDateFormat("M月d日", Locale.CHINA)
    val formattedExpiry = sdf.format(Date(expiryTime))

    Card(
        modifier = Modifier
            .width(176.dp)
            .fillMaxHeight()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .border(2.dp, baseColor.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .testTag("consumable_item_${consumable.id}"),
        colors = CardDefaults.cardColors(containerColor = if (isCritical) Color(0xFFFFF8F8) else Color(0xFFF8FBFF)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Edit/Delete small overlay
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CreamEditButton(onClick = onEdit)
                CreamDeleteButton(onClick = onDelete)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Title
                Text(
                    text = consumable.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                    maxLines = 1
                )

                // 2. Center Row: Icon + Remaining Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Icon Box
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = consumable.iconPath.ifBlank { "📦" }, fontSize = 34.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Days Pill (Side Box like image)
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, baseColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .padding(vertical = 4.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("剩余", fontSize = 9.sp, color = GrayTextMuted)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                            Text(
                                text = "$remainingDays",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = baseColor,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("天", fontSize = 9.sp, color = baseColor, modifier = Modifier.padding(bottom = 2.dp), maxLines = 1, softWrap = false)
                        }
                    }
                }

                // 3. Expiry Date Label
                Text(
                    text = "${formattedExpiry}到期",
                    fontSize = 11.sp,
                    color = GrayTextMuted,
                    textAlign = TextAlign.Center
                )

                // 4. Reset Button (High Fidelity Button)
                Button(
                    onClick = onReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(15.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (remainingDays <= 0) "已更换" else "重置",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = baseColor
                    )
                }
            }
        }
    }
}

@Composable
fun InventoryCardItem(
    inventory: Inventory,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.05f))
            .background(Color(0xFFFFFBF7), RoundedCornerShape(24.dp))
            .border(2.dp, Color(0xFFF9EAE1), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Icon (Left)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2F4F7)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = if (inventory.iconPath.isNotBlank()) inventory.iconPath else "🛍️", fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 2. Info (Middle)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = inventory.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C2C2C),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "库存 ${inventory.currentQty} ${inventory.unit} (安全: ${inventory.safeQty})",
                fontSize = 12.sp,
                fontWeight = if (inventory.isLow()) FontWeight.Bold else FontWeight.Normal,
                color = if (inventory.isLow()) Color(0xFFFF5A5A) else Color(0xFF8E8E8E)
            )
        }

        // 3. Actions (Right)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Minus button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, WarmOrange.copy(alpha = 0.5f), CircleShape)
                    .clickable { onDecrease() },
                contentAlignment = Alignment.Center
            ) {
                Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WarmOrange)
            }

            // Plus / Restock Button
            Button(
                onClick = onIncrease,
                modifier = Modifier.height(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmOrange),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("补货", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            CreamEditButton(onClick = onEdit)

            CreamDeleteButton(onClick = onDelete)
        }
    }
}

@Composable
fun BottomNavItem(
    index: Int,
    activeTab: Int,
    onClick: () -> Unit
) {
    val labels = listOf("首页", "日程", "物品", "我的")
    val icons = listOf(
        Icons.Default.Home,
        Icons.Default.DateRange,
        Icons.Default.ShoppingCart,
        Icons.Default.Person
    )
    val isSelected = activeTab == index

    val iconColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) WarmOrange else GrayTextMuted,
        animationSpec = androidx.compose.animation.core.tween(300)
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(64.dp)
                .background(
                    if (isSelected) WarmOrange.copy(alpha = 0.15f) else Color.Transparent,
                    RoundedCornerShape(percent = 50)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icons[index],
                contentDescription = labels[index],
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = labels[index],
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = iconColor
        )
    }
}

@Composable
fun OrganicPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
