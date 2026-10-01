package com.lifeordered.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "⚙️ 首页板块排序",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "返回",
                            tint = TextDark,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.updateHomeSectionOrder(orderList)
                        onBack()
                    }) {
                        Text("保存", fontWeight = FontWeight.Bold, color = WarmOrange, fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmCreamBg)
            )
        },
        containerColor = WarmCreamBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            Text(
                text = "通过上下箭头调整首页三大模块的展示顺序，定制最适合你的生活习惯卡片布局。",
                color = GrayTextMuted,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(orderList) { index, key ->
                    val (title, icon) = when (key) {
                        "moments" -> "重要时刻" to "🧡"
                        "alerts" -> "到期提醒" to "🕒"
                        "inventory" -> "库存物品" to "🛍️"
                        else -> "其他" to "❓"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .border(1.5.dp, Color(0xFFE6D7C3), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = icon, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                modifier = Modifier.weight(1f)
                            )

                            // Actions Column
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Up Action
                                IconButton(
                                    onClick = { moveItemCursor(index, -1) },
                                    enabled = index > 0,
                                    modifier = Modifier
                                        .size(48.dp) // Enlarged hot area
                                        .clip(CircleShape)
                                        .background(if (index > 0) LightCream else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "上移",
                                        tint = if (index > 0) DarkOrange else GrayTextMuted.copy(alpha = 0.5f)
                                    )
                                }

                                // Down Action
                                IconButton(
                                    onClick = { moveItemCursor(index, 1) },
                                    enabled = index < orderList.size - 1,
                                    modifier = Modifier
                                        .size(48.dp) // Enlarged hot area
                                        .clip(CircleShape)
                                        .background(if (index < orderList.size - 1) LightCream else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "下移",
                                        tint = if (index < orderList.size - 1) DarkOrange else GrayTextMuted.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
