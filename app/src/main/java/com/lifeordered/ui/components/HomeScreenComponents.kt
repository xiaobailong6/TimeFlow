package com.lifeordered.ui.components

 import androidx.compose.animation.core.Spring
 import androidx.compose.animation.core.animateFloatAsState
 import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
 import androidx.compose.ui.graphics.Color
 import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.core.date.MomentSchedule
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.format.MomentPresentation
import com.lifeordered.ui.theme.Glass
import com.lifeordered.ui.theme.GlassChip
import com.lifeordered.ui.theme.GlassIconButton
import com.lifeordered.ui.theme.glassSurface
import com.lifeordered.ui.theme.liquidGlass
import com.lifeordered.ui.theme.parseHexColor
import com.lifeordered.ui.theme.pressableClickable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 首页与通用组件（液态玻璃版）。
 *
 * 改造原则：
 * - 卡片不再是「实色底 + 圆角」，而是 `liquidGlass` + 主题色**辉光**
 * - 天数成为视觉主角（displayMedium + 负字距），不再和标题抢注意力
 * - 所有小按钮统一为圆形玻璃，去掉原来的米黄/浅红底
 */

// -----------------------------------------------------------------------------
// 顶部
// -----------------------------------------------------------------------------

@Composable
fun TopHeaderSection(
    greeting: String,
    onProfileClick: () -> Unit,
    title: String = "拾光流年"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Glass.TextPrimary,
                letterSpacing = (-0.6).sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = greeting,
                fontSize = 12.sp,
                color = Glass.TextSecondary
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .liquidGlass(
                    shape = CircleShape,
                    fill = Glass.Fill,
                    elevation = 6.dp,
                    glow = Glass.GlowAmber
                )
                .pressableClickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🧁", fontSize = 20.sp)
        }
    }
}

/**
 * 顶部概览横幅。
 *
 * 玻璃质感的关键在于它**背后必须有颜色**，所以这里放了两颗低透明度的
 * 光球，而不是一整块渐变底色——后者会让横幅看起来像普通卡片。
 */
@Composable
fun OrganicBannerBlock(itemCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .height(104.dp)
            .liquidGlass(
                shape = Glass.panelShape,
                fill = Glass.Fill,
                elevation = 12.dp,
                glow = Glass.GlowAmber
            )
    ) {
        // 内部光球：让玻璃「透出」暖光
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp)
                .size(78.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Glass.GlowAmber.copy(alpha = 0.55f),
                            Glass.GlowRose.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 22.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "让每一天都有迹可循",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Glass.TextPrimary,
                letterSpacing = (-0.2).sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = itemCount.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Glass.Accent,
                    letterSpacing = (-1).sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (itemCount == 0) "今天没有待处理事项" else "件事项待处理",
                    fontSize = 12.sp,
                    color = Glass.TextSecondary
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
 // 通用小按钮
// -----------------------------------------------------------------------------

@Composable
fun GlassEditButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
     containerSize: Dp = 28.dp,
    iconSize: Dp = 13.dp
) {
    GlassIconButton(
        icon = Icons.Outlined.Edit,
        contentDescription = "编辑",
        onClick = onClick,
        modifier = modifier,
        tint = Glass.TextPrimary,
        size = containerSize,
        iconSize = iconSize
    )
}

@Composable
fun GlassDeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
     containerSize: Dp = 28.dp,
    iconSize: Dp = 13.dp
) {
    GlassIconButton(
        icon = Icons.Default.Delete,
        contentDescription = "删除",
        onClick = onClick,
        modifier = modifier,
        tint = Glass.AccentDanger,
        size = containerSize,
        iconSize = iconSize
    )
}

@Composable
fun GlassCheckButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
     containerSize: Dp = 28.dp,
    iconSize: Dp = 13.dp
) {
    GlassIconButton(
        icon = icon,
        contentDescription = "完成",
        onClick = onClick,
        modifier = modifier,
        tint = Glass.AccentSuccess,
        size = containerSize,
        iconSize = iconSize
    )
}

// -----------------------------------------------------------------------------
// 时刻卡片
// -----------------------------------------------------------------------------

/**
 * 首页时刻卡片。
 *
 * 液态玻璃下的层次安排：**辉光表达「这是哪个时刻」，天数表达「还有多久」**。
 * 因此卡片底色一律是 [Glass.Fill]，个性完全交给 `glow`（取 `themeColorHex`）。
 */
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

    val themeColor = parseHexColor(moment.themeColorHex)
    val days = remember(moment) { MomentSchedule.resolve(moment) }
    val dayColor = MomentPresentation.dayColor(days)

    Box(
        modifier = Modifier
            .width(182.dp)
            .fillMaxHeight()
            .liquidGlass(
                shape = Glass.cardShape,
                fill = Glass.Fill,
                elevation = 14.dp,
                glow = themeColor
            )
            .testTag("moment_item_${moment.id}")
            .combinedClickable(
                onClick = { if (moment.notes?.isNotBlank() == true) showNotesDialog = true },
                onLongClick = { onArchive() }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = moment.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Glass.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = MomentPresentation.cardSubtitle(moment),
                        fontSize = 10.sp,
                        color = Glass.TextTertiary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                     GlassEditButton(onClick = onEdit, containerSize = 28.dp, iconSize = 13.dp)
                     GlassDeleteButton(onClick = onDelete, containerSize = 28.dp, iconSize = 13.dp)
                }
            }

            // 天数主角
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                         .glassSurface(shape = Glass.controlShape, fill = Glass.FillControl),
                    contentAlignment = Alignment.Center
                ) {
                    MomentIcon(moment)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = MomentPresentation.dayValue(days),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = dayColor,
                        letterSpacing = (-1.2).sp,
                        maxLines = 1
                    )
                    Text(
                        text = MomentPresentation.dayCaption(days),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Glass.TextTertiary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // 日期 + 打卡
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = MomentPresentation.dateLabel(moment, withSolarConversion = true),
                    fontSize = 10.sp,
                    color = Glass.TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(7.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                         .glassSurface(shape = Glass.pillShape, fill = Glass.FillControl)
                        .pressableClickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onComplete()
                        }
                        .testTag("moment_complete_${moment.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = MomentPresentation.checkInLabel(moment),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Glass.Accent,
                        maxLines = 1
                    )
                }
            }
        }
    }

    if (showNotesDialog && !moment.notes.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            title = {
                Text(
                    text = moment.title,
                    fontWeight = FontWeight.Bold,
                    color = Glass.TextPrimary
                )
            },
            text = {
                Text(
                    text = moment.notes ?: "",
                    fontSize = 14.sp,
                    color = Glass.TextSecondary,
                    lineHeight = 21.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showNotesDialog = false }) {
                    Text("知道了", color = Glass.Accent, fontWeight = FontWeight.SemiBold)
                }
            },
            shape = Glass.cardShape,
            containerColor = Glass.SheetFill
        )
    }
}

/**
 * 时刻卡片的图标，槽位由 [MomentPresentation.iconSlot] 统一决定。
 *
 * 四个槽位对应四种玻璃图标；其余情况回落到用户自选的 emoji——
 * emoji 自带色彩且不透明，但它是用户明确选的，保留比强行替换更尊重意图。
 */
@Composable
fun MomentIcon(moment: Moment, size: Dp = 44.dp) {
    val iconModifier = Modifier.size(size)
    when (MomentPresentation.iconSlot(moment)) {
        0 -> GlassCakeIcon(modifier = iconModifier)
        1 -> GlassGiftIcon(modifier = iconModifier)
        2 -> GlassHeartsIcon(modifier = iconModifier)
        3 -> GlassPartyIcon(modifier = iconModifier)
        else -> Text(text = moment.iconPath.ifBlank { "🌟" }, fontSize = 24.sp)
    }
}

// -----------------------------------------------------------------------------
// 耗材卡片
// -----------------------------------------------------------------------------

@Composable
fun ConsumableCardItem(
    consumable: Consumable,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val remainingDays = consumable.getRemainingDays()
    val isCritical = remainingDays <= 3
    val accent = if (isCritical) Glass.AccentDanger else Glass.AccentCool

    val expiryTime = com.lifeordered.core.utils.CalendarHelper.addLocalDays(
        consumable.startDateMillis,
        consumable.totalDays
    )
    val formattedExpiry = SimpleDateFormat("M月d日", Locale.CHINA).format(Date(expiryTime))

    // 剩余比例：0..1
    val ratio = if (consumable.totalDays > 0) {
        (remainingDays.toFloat() / consumable.totalDays.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .width(178.dp)
            .fillMaxHeight()
            .liquidGlass(
                shape = Glass.cardShape,
                fill = Glass.Fill,
                elevation = 12.dp,
                glow = accent
            )
            .testTag("consumable_item_${consumable.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = consumable.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Glass.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                     GlassEditButton(onClick = onEdit, containerSize = 28.dp, iconSize = 13.dp)
                     GlassDeleteButton(onClick = onDelete, containerSize = 28.dp, iconSize = 13.dp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = consumable.iconPath.ifBlank { "📦" }, fontSize = 30.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = remainingDays.toString(),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = accent,
                        letterSpacing = (-1.2).sp,
                        maxLines = 1
                    )
                    Text(
                        text = "天",
                        fontSize = 10.sp,
                        color = Glass.TextTertiary
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // 剩余比例条：玻璃上的细条，比原来的粗实色条更克制
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(Glass.pillShape)
                        .background(Glass.FillStrong)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(ratio)
                            .clip(Glass.pillShape)
                            .background(accent)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${formattedExpiry}到期",
                    fontSize = 10.sp,
                    color = Glass.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(7.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                         .glassSurface(shape = Glass.pillShape, fill = Glass.FillControl)
                        .pressableClickable(onClick = onReset),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (remainingDays <= 0) "已更换，重新开始" else "重置使用周期",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 库存行
// -----------------------------------------------------------------------------

@Composable
fun InventoryCardItem(
    inventory: Inventory,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    val isLow = inventory.isLow()
    val accent = if (isLow) Glass.AccentDanger else Glass.AccentSuccess

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .liquidGlass(
                shape = Glass.cardShape,
                fill = Glass.Fill,
                elevation = 8.dp,
                glow = accent
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                     .glassSurface(shape = CircleShape, fill = Glass.FillControl),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = inventory.iconPath.ifBlank { "🛍️" },
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = inventory.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Glass.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${inventory.currentQty} ${inventory.unit}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    Text(
                        text = "  安全线 ${inventory.safeQty}",
                        fontSize = 11.sp,
                        color = Glass.TextTertiary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GlassChip(
                    label = "−",
                    selected = false,
                    onClick = onDecrease,
                    accent = accent
                )
                GlassChip(
                    label = "补货",
                    selected = false,
                    onClick = onIncrease,
                    accent = accent
                )
                GlassEditButton(onClick = onEdit, containerSize = 28.dp, iconSize = 13.dp)
                GlassDeleteButton(onClick = onDelete, containerSize = 28.dp, iconSize = 13.dp)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 空状态
// -----------------------------------------------------------------------------

@Composable
fun EmptyStateBlock(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .liquidGlass(shape = Glass.cardShape, fill = Glass.FillSubtle, elevation = 4.dp)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 12.sp,
            color = Glass.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun GlobalEmptyStateBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .liquidGlass(shape = Glass.panelShape, fill = Glass.Fill, elevation = 10.dp)
            .padding(horizontal = 24.dp, vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "✨", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "一切都是全新的开始",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Glass.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "点击下方 ＋ 记录下生活的第一份秩序",
                fontSize = 12.sp,
                color = Glass.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 底部导航
// -----------------------------------------------------------------------------

 /**
  * 底部导航项。
  *
  * 选中态是「液态玻璃胶囊」：半透明白胶囊（`Glass.FillControl`）
  * 从中心弹性浮现包住图标；配合弹簧参数，切换时有「液体涌起」的观感，
  * 而不是单纯换色。
  */
 @Composable
 fun BottomNavItem(
     label: String,
     icon: ImageVector,
     selected: Boolean,
     onClick: () -> Unit
 ) {
     // 选中胶囊的出现/消失动画：弹簧驱动，产生液态的弹性涌起。
     val pillProgress by animateFloatAsState(
         targetValue = if (selected) 1f else 0f,
         animationSpec = spring(
             dampingRatio = Spring.DampingRatioMediumBouncy,
             stiffness = Spring.StiffnessMediumLow
         ),
         label = "bottomNavPill"
     )
     // 图标微移：选中时被玻璃胶囊「托起」一点点，强化液面浮起的错觉。
     val iconLift by animateFloatAsState(
         targetValue = if (selected) -2f else 0f,
         animationSpec = spring(
             dampingRatio = Spring.DampingRatioMediumBouncy,
             stiffness = Spring.StiffnessMedium
         ),
         label = "bottomNavIconLift"
     )
 
     Column(
         modifier = Modifier
             .clip(Glass.pillShape)
             // 底部 tab 的点击面积大，用更深的回弹（0.94）才有「按下去了」的实感
             .pressableClickable(pressedScale = 0.94f, onClick = onClick)
             .padding(horizontal = 6.dp, vertical = 6.dp),
         horizontalAlignment = Alignment.CenterHorizontally,
         verticalArrangement = Arrangement.Center
     ) {
         Box(
             modifier = Modifier
                 .height(32.dp)
                 .width(52.dp),
             contentAlignment = Alignment.Center
         ) {
             // 液态玻璃胶囊：progress 低于阈值时不挂载，避免常驻采样开销。
             if (pillProgress > 0.01f) {
                 Box(
                     modifier = Modifier
                         .matchParentSize()
                         .graphicsLayer {
                             // 从中心弹性缩放出现，progress 直接驱动，保留回弹细节
                             scaleX = 0.6f + 0.4f * pillProgress
                             scaleY = 0.6f + 0.4f * pillProgress
                             alpha = pillProgress.coerceIn(0f, 1f)
                         }
                         .glassSurface(shape = Glass.pillShape, fill = Glass.FillControl)
                 )
             }
             Icon(
                 imageVector = icon,
                 contentDescription = label,
                 tint = if (selected) Glass.Accent else Glass.TextTertiary,
                 modifier = Modifier
                     .size(21.dp)
                     .graphicsLayer { translationY = iconLift }
             )
         }
         Spacer(modifier = Modifier.height(3.dp))
         Text(
             text = label,
             fontSize = 10.sp,
             fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
             color = if (selected) Glass.Accent else Glass.TextTertiary
         )
     }
 }

/** 圆形小标签，用于状态提示。 */
@Composable
fun OrganicPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(Glass.pillShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
