package com.lifeordered.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel

/**
 * 首页板块排序。
 *
 * 「保存」从原来的 TopAppBar `TextButton` 移到 [GlassScreenHeader] 的 actions 槽，
 * 用 [GlassChip] 表达——玻璃体系里没有实心文字按钮这个层级。
 *
 * 上移/下移箭头保留原有逻辑（[moveItemCursor] 自带边界判断）；
 * [GlassIconButton] 没有 `enabled` 参数，所以「不可用」靠 `tint` 变暗 + 回调内部空转表达。
 */
@Composable
fun HomeSectionOrderingScreen(
    viewModel: LifeOrderedViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var orderList by remember { mutableStateOf(viewModel.homeSectionOrder) }

    fun moveItemCursor(index: Int, direction: Int) {
        if (index + direction in orderList.indices) {
            val newList = orderList.toMutableList()
            val temp = newList[index]
            newList[index] = newList[index + direction]
            newList[index + direction] = temp
            orderList = newList
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        GlassScreenHeader(
            title = "首页板块排序",
            subtitle = "调整顺序，把最常看的放在最上面",
            onBack = onBack,
            actions = {
                GlassChip(
                    label = "保存",
                    selected = true,
                    onClick = {
                        viewModel.updateHomeSectionOrder(orderList)
                        onBack()
                    }
                )
            }
        )

        Text(
            text = "通过上下箭头调整首页三大模块的展示顺序，定制最适合你的生活习惯卡片布局。",
            color = Glass.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

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
            itemsIndexed(orderList) { index, key ->
                val (title, icon) = when (key) {
                    "moments" -> "重要时刻" to "🧡"
                    "alerts" -> "到期提醒" to "🕒"
                    "inventory" -> "库存物品" to "🛍️"
                    else -> "其他" to "❓"
                }

                // 每个板块一个主题色：辉光而不是底色，避免玻璃被实色填满
                val glowColor = when (key) {
                    "moments" -> Glass.Accent
                    "alerts" -> Glass.AccentCool
                    "inventory" -> Glass.AccentSuccess
                    else -> Glass.AccentWarning
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glow = glowColor,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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

                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Glass.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GlassIconButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                contentDescription = "上移",
                                onClick = { moveItemCursor(index, -1) },
                                tint = if (index > 0) Glass.Accent else Glass.TextTertiary,
                                size = 34.dp,
                                iconSize = 20.dp
                            )

                            GlassIconButton(
                                icon = Icons.Default.KeyboardArrowDown,
                                contentDescription = "下移",
                                onClick = { moveItemCursor(index, 1) },
                                tint = if (index < orderList.size - 1) {
                                    Glass.Accent
                                } else {
                                    Glass.TextTertiary
                                },
                                size = 34.dp,
                                iconSize = 20.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
