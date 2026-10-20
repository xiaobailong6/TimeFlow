package com.lifeordered.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.data.models.Inventory
import com.lifeordered.ui.format.InventoryFilter
import com.lifeordered.ui.theme.Glass
import com.lifeordered.ui.theme.GlassChip
import com.lifeordered.ui.theme.GlassEmptyState
import com.lifeordered.ui.theme.GlassIconButton
import com.lifeordered.ui.theme.GlassScreenHeader
import com.lifeordered.ui.theme.GlassTextField
import com.lifeordered.ui.theme.accentGlow
import com.lifeordered.ui.theme.glassSurface
import com.lifeordered.ui.theme.liquidGlass
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel

/**
 * 物品页（液态玻璃版）。
 *
 * 库存的核心信息是「还剩多少」与「够不够」。因此进度条用
 * **相对安全线的比例**而非相对某个最大值——用户关心的是「离红线还有多远」，
 * 不是「占满仓位的百分比」。
 */
@Composable
fun ItemsScreen(
    viewModel: LifeOrderedViewModel,
    onEditRequest: (Inventory) -> Unit,
    onDeleteRequest: (Inventory) -> Unit,
    modifier: Modifier = Modifier,
    initialFilter: String = InventoryFilter.ALL
) {
    val notify = rememberNotifier()
    val inventories by viewModel.allInventories.collectAsState()
    val tags by viewModel.allTags.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryTab by remember(initialFilter) { mutableStateOf(initialFilter) }
    var showManageTags by remember { mutableStateOf(false) }

    val categories = remember(tags) {
        listOf(InventoryFilter.ALL, InventoryFilter.LOW_STOCK) + tags.map { it.name }
    }

    val filteredInventories = remember(inventories, searchQuery, selectedCategoryTab) {
        inventories.filter { item ->
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategoryTab) {
                InventoryFilter.ALL -> true
                InventoryFilter.LOW_STOCK -> item.isLow()
                else -> item.category == selectedCategoryTab
            }
            matchesSearch && matchesCategory
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        GlassScreenHeader(
            title = "物品",
            subtitle = if (inventories.isEmpty()) {
                "还没有登记任何物件"
            } else {
                "${inventories.size} 件物件在管"
            }
        )

        GlassTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "搜索物件名称",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .testTag("inventory_search_input"),
            leading = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Glass.TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(categories) { category ->
                GlassChip(
                    label = if (category == InventoryFilter.LOW_STOCK) "⚠ $category" else category,
                    selected = category == selectedCategoryTab,
                    onClick = { selectedCategoryTab = category },
                    modifier = Modifier.testTag("category_tab_$category")
                )
            }

            item {
                GlassIconButton(
                    icon = Icons.Default.Add,
                    contentDescription = "管理品类标签",
                    onClick = { showManageTags = true },
                    size = 34.dp,
                    iconSize = 16.dp,
                    tint = Glass.TextSecondary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (filteredInventories.isEmpty()) {
                item {
                    GlassEmptyState(
                        glyph = "🔍",
                        title = if (searchQuery.isBlank()) "这一栏还没有物件" else "没有匹配的物件",
                        hint = if (selectedCategoryTab != InventoryFilter.ALL) {
                            "当前筛选是「$selectedCategoryTab」，换个分类看看"
                        } else {
                            "换个关键词，或者点下方 ＋ 新增一件"
                        },
                        actionText = if (selectedCategoryTab != InventoryFilter.ALL) "清除筛选" else null,
                        onAction = { selectedCategoryTab = InventoryFilter.ALL }
                    )
                }
            } else {
                items(filteredInventories, key = { it.id }) { item ->
                    InventoryGridCard(
                        inventory = item,
                        onIncrement = {
                            viewModel.updateInventoryQuantity(item, 1)
                            notify("已补货 1 ${item.unit}")
                        },
                        onDecrement = {
                            if (item.currentQty > 0) {
                                viewModel.updateInventoryQuantity(item, -1)
                            }
                        },
                        onEdit = { onEditRequest(item) },
                        onDelete = { onDeleteRequest(item) }
                    )
                }
            }
        }
    }

    if (showManageTags) {
        ManageTagsDialog(viewModel = viewModel, onDismiss = { showManageTags = false })
    }
}

/** 品类标签管理。 */
@Composable
fun ManageTagsDialog(viewModel: LifeOrderedViewModel, onDismiss: () -> Unit) {
    val notify = rememberNotifier()
    val tags by viewModel.allTags.collectAsState()
    var newTagName by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("完成", fontWeight = FontWeight.SemiBold, color = Glass.Accent)
            }
        },
        title = {
            Text(
                text = "管理品类标签",
                fontWeight = FontWeight.Bold,
                color = Glass.TextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "标签用来给物件分组，比如「食品」「洗护」。",
                    fontSize = 12.sp,
                    color = Glass.TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassTextField(
                        value = newTagName,
                        onValueChange = { if (it.length <= 6) newTagName = it },
                        placeholder = "新标签，最多 6 字",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "添加标签",
                        onClick = {
                            if (newTagName.isNotBlank()) {
                                viewModel.addTag(newTagName)
                                notify("已添加标签「${newTagName.trim()}」")
                                newTagName = ""
                            }
                        },
                        size = 44.dp,
                        iconSize = 18.dp,
                        tint = Glass.Accent
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .glassSurface(shape = Glass.fieldShape, fill = Glass.FillSubtle)
                        .padding(10.dp)
                ) {
                    if (tags.isEmpty()) {
                        Text(
                            text = "还没有任何标签",
                            fontSize = 12.sp,
                            color = Glass.TextTertiary,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                    tags.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tag.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Glass.TextPrimary
                            )
                            GlassIconButton(
                                icon = Icons.Default.Add,
                                contentDescription = "删除标签",
                                onClick = {
                                    viewModel.deleteTag(tag)
                                    notify("已删除标签「${tag.name}」")
                                },
                                size = 26.dp,
                                iconSize = 12.dp,
                                tint = Glass.AccentDanger
                            )
                        }
                    }
                }
            }
        },
        shape = Glass.cardShape,
        containerColor = Glass.SheetFill
    )
}

/**
 * 库存卡片。
 *
 * 进度条比例 = `currentQty / safeQty`：1.0 表示刚好压线，
 * 因此**满条不等于健康**，而是「刚到安全线」。低于 1 就是告警区。
 */
@Composable
fun InventoryGridCard(
    inventory: Inventory,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLow = inventory.isLow()
    val accent = if (isLow) Glass.AccentDanger else Glass.AccentSuccess

    val ratio = if (inventory.safeQty > 0) {
        (inventory.currentQty.toFloat() / inventory.safeQty.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .liquidGlass(
                shape = Glass.cardShape,
                fill = Glass.Fill,
                elevation = 10.dp,
                glow = accent
            )
            .padding(horizontal = 15.dp, vertical = 13.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .glassSurface(shape = Glass.controlShape, fill = Glass.FillSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = inventory.iconPath.ifBlank { "🏡" },
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(11.dp))
                    Column {
                        Text(
                            text = inventory.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Glass.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = inventory.category,
                            fontSize = 11.sp,
                            color = Glass.TextTertiary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassEditButton(onClick = onEdit, containerSize = 30.dp, iconSize = 14.dp)
                    GlassDeleteButton(onClick = onDelete, containerSize = 30.dp, iconSize = 14.dp)
                }
            }

            Spacer(modifier = Modifier.height(11.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = inventory.currentQty.toString(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = accent,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = inventory.unit,
                        fontSize = 12.sp,
                        color = Glass.TextSecondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "安全线 ${inventory.safeQty}",
                        fontSize = 11.sp,
                        color = Glass.TextTertiary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 减一
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .glassSurface(shape = CircleShape, fill = Glass.FillSubtle)
                            .clickable(onClick = onDecrement)
                            .testTag("item_dec_${inventory.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "−",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Glass.TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 补货
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .accentGlow(accent, shape = Glass.pillShape, elevation = 6.dp)
                            .clip(Glass.pillShape)
                            .background(accent)
                            .clickable(onClick = onIncrement)
                            .padding(horizontal = 14.dp)
                            .testTag("item_inc_${inventory.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "补货",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Glass.OnAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(11.dp))

            // 相对安全线的进度
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
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
                text = if (isLow) "已低于安全线，建议尽快补货" else "库存充足",
                fontSize = 10.sp,
                color = if (isLow) accent else Glass.TextTertiary,
                textAlign = TextAlign.Start
            )
        }
    }
}
