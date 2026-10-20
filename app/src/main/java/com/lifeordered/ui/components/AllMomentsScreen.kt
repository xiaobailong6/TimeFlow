package com.lifeordered.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.core.date.MomentSchedule
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.format.MomentPresentation
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel

/**
 * 全部时刻。
 *
 * ## 液态玻璃下的结构
 *
 * 根容器**不再设置背景色**：整屏的「液体」由外层 `LiquidGlassBackground` 提供，
 * 这里任何不透明底色都会把背后的光斑整块盖住（重构前的奶油色底正是如此）。
 * 页面只负责把玻璃薄片浮上去。
 */
@Composable
fun AllMomentsScreen(
    viewModel: LifeOrderedViewModel,
    onBack: () -> Unit,
    onEdit: (Moment) -> Unit,
    onDelete: (Moment) -> Unit,
    modifier: Modifier = Modifier
) {
    val moments by viewModel.activeMoments.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassScreenHeader(
                title = "所有重要时刻",
                subtitle = if (moments.isEmpty()) {
                    "还没有记录"
                } else {
                    "共 ${moments.size} 个时刻在流动"
                },
                onBack = onBack
            )

            if (moments.isEmpty()) {
                GlassEmptyState(
                    glyph = "✨",
                    title = "还没有记录任何时刻",
                    hint = "点底部的「＋」记下第一个纪念日或倒数日，之后这里会按剩余天数帮你排好。",
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
                    items(moments, key = { it.id }) { moment ->
                        MomentListItem(
                            moment = moment,
                            onEdit = { onEdit(moment) },
                            onDelete = { onDelete(moment) }
                        )
                    }
                }
            }
        }

        // 测试钩子：`GlassScreenHeader` 内部的返回按钮不接受外部 modifier，
        // 因此在这里叠一个与返回按钮位置重合的语义锚点来承载历史 testTag。
        // 该 Box 没有 pointerInput，点击会穿透到下面的返回按钮，行为不变。
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 12.dp)
                .size(38.dp)
                .testTag("all_moments_back_button")
        )
    }
}

/**
 * 全部时刻列表行。
 *
 * 与首页卡片共享 [MomentPresentation]，因此日期标签、天数文案、颜色、
 * 图标槽位与首页完全一致（重构前这里是另一套规则，连图标分支都只有 3 个）。
 *
 * 视觉上：卡片底色统一用 [Glass.Fill]，条目之间的差异交给 `glow`（取 `themeColorHex`），
 * 这样既保留个性又不会把玻璃变成实心色块。
 */
@Composable
fun MomentListItem(
    moment: Moment,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val baseColor = parseHexColor(moment.themeColorHex)
    val days = remember(moment) { MomentSchedule.resolve(moment) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = Glass.cardShape, glow = baseColor)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标：玻璃小方块托住黏土图标，避免图标直接压在液体背景上显得脏
        Box(
            modifier = Modifier
                .size(48.dp)
                .glassSurface(shape = Glass.fieldShape, fill = Glass.FillSubtle),
            contentAlignment = Alignment.Center
        ) {
            MomentIcon(moment = moment, size = 44.dp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = moment.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Glass.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = MomentPresentation.listSubtitle(moment),
                fontSize = 11.sp,
                color = Glass.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = MomentPresentation.dayCaption(days),
                    fontSize = 11.sp,
                    color = Glass.TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = MomentPresentation.dayValue(days),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MomentPresentation.dayColor(days)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = "天", fontSize = 11.sp, color = Glass.TextSecondary)
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
