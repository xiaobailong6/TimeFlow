package com.lifeordered.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel

/**
 * 回收站。
 *
 * 反馈统一走 [rememberNotifier]（Snackbar），不再用 `android.widget.Toast`：
 * 快速连点恢复时 Toast 会排队堆叠，且不随页面切换消失。
 */
@Composable
fun TrashBinScreen(
    viewModel: LifeOrderedViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deletedMoments by viewModel.deletedMoments.collectAsState()
    val deletedConsumables by viewModel.deletedConsumables.collectAsState()
    val deletedInventories by viewModel.deletedInventories.collectAsState()

    val hasItems = deletedMoments.isNotEmpty() ||
        deletedConsumables.isNotEmpty() ||
        deletedInventories.isNotEmpty()
    var itemToPermanentDelete by remember { mutableStateOf<Any?>(null) }
    val notify = rememberNotifier()

    val totalCount = deletedMoments.size + deletedConsumables.size + deletedInventories.size

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassScreenHeader(
                title = "回收站",
                subtitle = if (hasItems) {
                    "共 ${totalCount} 项待清理"
                } else {
                    "已清空"
                },
                onBack = onBack
            )

            if (!hasItems) {
                GlassEmptyState(
                    glyph = "🍃",
                    title = "回收站已经清空",
                    hint = "删除的时刻、到期提醒和库存会先放进来，随时能恢复；只有点「彻底删除」才会真的消失。",
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
                    items(deletedMoments, key = { "m_${it.id}" }) { moment ->
                        TrashItemRow(
                            title = moment.title,
                            type = "重要时刻",
                            icon = "🧡",
                            onRestore = {
                                viewModel.restoreFromTrash(moment)
                                notify("✅「${moment.title}」已恢复")
                            },
                            onPermanentDelete = { itemToPermanentDelete = moment }
                        )
                    }

                    items(deletedConsumables, key = { "c_${it.id}" }) { consumable ->
                        TrashItemRow(
                            title = consumable.name,
                            type = "到期提醒",
                            icon = "🕒",
                            onRestore = {
                                viewModel.restoreFromTrash(consumable)
                                notify("✅「${consumable.name}」已恢复")
                            },
                            onPermanentDelete = { itemToPermanentDelete = consumable }
                        )
                    }

                    items(deletedInventories, key = { "i_${it.id}" }) { inventory ->
                        TrashItemRow(
                            title = inventory.name,
                            type = "库存物品",
                            icon = "🛍️",
                            onRestore = {
                                viewModel.restoreFromTrash(inventory)
                                notify("✅「${inventory.name}」已恢复")
                            },
                            onPermanentDelete = { itemToPermanentDelete = inventory }
                        )
                    }
                }
            }
        }

        // 彻底删除确认
        itemToPermanentDelete?.let { item ->
            ActionConfirmBottomSheet(
                title = "要彻底销毁它吗？",
                content = "彻底删除后将无法恢复哦～",
                confirmText = "彻底删除",
                isWarning = true,
                onConfirm = {
                    when (item) {
                        is Moment -> viewModel.permanentDeleteMoment(item)
                        is Consumable -> viewModel.permanentDeleteConsumable(item)
                        is Inventory -> viewModel.permanentDeleteInventory(item)
                    }
                    notify("🗑️ 已彻底删除")
                    itemToPermanentDelete = null
                },
                onCancel = { itemToPermanentDelete = null },
                onDismissRequest = { itemToPermanentDelete = null }
            )
        }
    }
}

/**
 * 回收站行。
 *
 * 两个动作都用 [GlassChip] 而不是实心 `Button`：
 * 玻璃体系里「可点的东西」统一是薄片 + 语义色，实心按钮会显得像贴上去的补丁。
 * `accent` 在未选中态不参与绘制，但保留语义色方便后续做「危险操作高亮」。
 */
@Composable
fun TrashItemRow(
    title: String,
    type: String,
    icon: String,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = Glass.cardShape, glow = Glass.AccentWarning)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标：玻璃小方块托住 Emoji
        Box(
            modifier = Modifier
                .size(40.dp)
                .glassSurface(shape = Glass.fieldShape, fill = Glass.FillSubtle),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Glass.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = type,
                fontSize = 11.sp,
                color = Glass.TextSecondary,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GlassChip(
                label = "恢复",
                selected = false,
                onClick = onRestore,
                accent = Glass.AccentSuccess
            )
            GlassChip(
                label = "彻底删除",
                selected = false,
                onClick = onPermanentDelete,
                accent = Glass.AccentDanger
            )
        }
    }
}
