package com.lifeordered.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.data.models.Consumable
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 所有到期提醒。
 *
 * 根容器不再设置背景色，液体背景由外层 `LiquidGlassBackground` 提供。
 */
@Composable
fun AllUpcomingScreen(
    viewModel: LifeOrderedViewModel,
    onBack: () -> Unit,
    onEdit: (Consumable) -> Unit,
    onDelete: (Consumable) -> Unit,
    modifier: Modifier = Modifier
) {
    val consumables by viewModel.allConsumables.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassScreenHeader(
                title = "所有到期提醒",
                subtitle = if (consumables.isEmpty()) {
                    "还没有记录"
                } else {
                    "共 ${consumables.size} 件耗材在倒计时"
                },
                onBack = onBack
            )

            if (consumables.isEmpty()) {
                GlassEmptyState(
                    glyph = "🕒",
                    title = "还没有记录任何耗材",
                    hint = "点底部的「＋」记录一瓶面霜或一袋咖啡，填上开封日和能用几天，这里就会开始倒计时。",
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 4.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(consumables, key = { it.id }) { consumable ->
                        ConsumableListItem(
                            consumable = consumable,
                            onEdit = { onEdit(consumable) },
                            onDelete = { onDelete(consumable) }
                        )
                    }
                }
            }
        }

        // 测试钩子：`GlassScreenHeader` 内部的返回按钮不接受外部 modifier，
        // 这里叠一个位置重合的语义锚点承载历史 testTag；它没有 pointerInput，
        // 点击会穿透到下面的返回按钮，交互行为不变。
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 12.dp)
                .size(38.dp)
                .testTag("all_upcoming_back_button")
        )
    }
}

/**
 * 到期提醒列表行。
 *
 * 语义色规则：剩余 ≤ 3 天视为临界，用 [Glass.AccentDanger]；其余用 [Glass.AccentCool]。
 * 颜色既作用于天数文字，也通过 `glow` 让整张卡片「亮起来」——这是液态玻璃里
 * 表达紧迫度的方式，而不是把底色换成红色。
 */
@Composable
fun ConsumableListItem(
    consumable: Consumable,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val remainingDays = consumable.getRemainingDays()
    val isCritical = remainingDays <= 3
    val baseColor = if (isCritical) Glass.AccentDanger else Glass.AccentCool

    val expiryTime = CalendarHelper.addLocalDays(consumable.startDateMillis, consumable.totalDays)
    val sdf = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
    val formattedExpiry = sdf.format(Date(expiryTime))

    val progress = (
        remainingDays.toFloat() / consumable.totalDays.coerceAtLeast(1).toFloat()
        ).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = Glass.cardShape, glow = baseColor)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标：玻璃小方块托住 Emoji，避免它直接压在液体背景上
        Box(
            modifier = Modifier
                .size(48.dp)
                .glassSurface(shape = Glass.fieldShape, fill = Glass.FillSubtle),
            contentAlignment = Alignment.Center
        ) {
            Text(text = consumable.iconPath.ifBlank { "📦" }, fontSize = 26.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = consumable.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Glass.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "${formattedExpiry} 到期",
                fontSize = 11.sp,
                color = Glass.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "剩余", fontSize = 11.sp, color = Glass.TextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${remainingDays}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = baseColor
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = "天", fontSize = 11.sp, color = Glass.TextSecondary)

                Spacer(modifier = Modifier.width(10.dp))

                // 剩余进度：胶囊底槽 + 强调色填充
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(6.dp)
                        .glassPill(fill = Glass.FillSubtle)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(Glass.pillShape)
                            .background(baseColor)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GlassEditButton(onClick = onEdit, containerSize = 32.dp, iconSize = 15.dp)
            GlassDeleteButton(onClick = onDelete, containerSize = 32.dp, iconSize = 15.dp)
        }
    }
}
