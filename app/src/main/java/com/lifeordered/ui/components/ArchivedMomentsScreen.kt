package com.lifeordered.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.format.MomentPresentation
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel

/**
 * 历史时刻归档。
 *
 * 反馈不再用 `android.widget.Toast`：Toast 是系统窗口，在深色玻璃界面上像补丁，
 * 而且 Compose 测试看不见。统一走 [rememberNotifier]（Snackbar 通道）。
 */
@Composable
fun ArchivedMomentsScreen(
    viewModel: LifeOrderedViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val archivedList by viewModel.archivedMoments.collectAsState()
    val notify = rememberNotifier()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassScreenHeader(
                title = "历史时刻归档",
                subtitle = if (archivedList.isEmpty()) {
                    "还没有归档"
                } else {
                    "共 ${archivedList.size} 条记录在这里休息"
                },
                onBack = onBack
            )

            if (archivedList.isEmpty()) {
                GlassEmptyState(
                    glyph = "📦",
                    title = "归档箱还是空的",
                    hint = "在首页长按一个已经结束的时刻并选择归档，它就会搬到这里；需要时随时可以恢复。",
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
                    item {
                        // 顶部提示条：玻璃浅色块，而不是实色 Surface
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassSurface(shape = Glass.fieldShape, fill = Glass.FillSubtle)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "💡 归档的时刻不会参与首页倒计时。点右侧的「恢复」就能放回首页继续计算天数。",
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                color = Glass.TextSecondary
                            )
                        }
                    }

                    items(archivedList, key = { it.id }) { moment ->
                        ArchivedMomentItemRow(
                            moment = moment,
                            onRestore = {
                                viewModel.updateMomentArchiveStatus(moment, false)
                                notify("🔄「${moment.title}」已恢复到首页")
                            },
                            onDelete = {
                                viewModel.moveToTrash(moment)
                                notify("🗑️「${moment.title}」已移入回收站")
                            }
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
                .testTag("archive_back_button")
        )
    }
}

/**
 * 归档列表行。
 *
 * 图标不再硬编码 📦：归档项同样是「某个时刻」，所以走 [MomentIcon]，
 * 与首页/全部时刻保持同一套图标槽位。
 */
@Composable
fun ArchivedMomentItemRow(
    moment: Moment,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val baseColor = parseHexColor(moment.themeColorHex)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = Glass.cardShape, glow = baseColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // 图标：玻璃圆角方块托住黏土图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .glassSurface(shape = Glass.fieldShape, fill = Glass.FillSubtle),
                contentAlignment = Alignment.Center
            ) {
                MomentIcon(moment = moment, size = 32.dp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = moment.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Glass.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = MomentPresentation.listSubtitle(moment),
                    fontSize = 11.sp,
                    color = Glass.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!moment.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "备注：${moment.notes}",
                        fontSize = 11.sp,
                        color = Glass.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 恢复：用玻璃图标按钮 + 成功色，语义比原来的主题色图标更明确
            GlassIconButton(
                icon = Icons.Default.Refresh,
                contentDescription = "恢复",
                onClick = onRestore,
                modifier = Modifier.testTag("archive_restore_${moment.id}"),
                tint = Glass.AccentSuccess,
                size = 32.dp,
                iconSize = 16.dp
            )

            GlassDeleteButton(
                onClick = onDelete,
                modifier = Modifier.testTag("archive_delete_${moment.id}"),
                containerSize = 32.dp,
                iconSize = 16.dp
            )
        }
    }
}
