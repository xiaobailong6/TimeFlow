package com.lifeordered.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import com.lifeordered.data.models.Inventory
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel

@Composable
fun ItemsScreen(
    viewModel: LifeOrderedViewModel,
    onEditRequest: (Inventory) -> Unit,
    onDeleteRequest: (Inventory) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inventories by viewModel.allInventories.collectAsState()
    val tags by viewModel.allTags.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    
    // Manage filtering state from VM for inter-tab navigation
    val selectedCategoryTab by viewModel.itemCategoryFilter
    var showManageTags by remember { mutableStateOf(false) }

    val categories = remember(tags) {
        listOf("全部", "低库存") + tags.map { it.name }
    }

    val filteredInventories = remember(inventories, searchQuery, selectedCategoryTab) {
        inventories.filter { item ->
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategoryTab) {
                "全部" -> true
                "低库存" -> item.isLow()
                else -> item.category == selectedCategoryTab
            }
            matchesSearch && matchesCategory
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBg)
    ) {
        // 1. Cozy Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "物品",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            
            Text(
                text = "${inventories.size} 款家居物件 🏡",
                fontSize = 13.sp,
                color = GrayTextMuted,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // 2. Beautiful rounded Search Bar with Clay Feeling
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜索我的生活物件...", color = GrayTextMuted, fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = OrangePrimaryText
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = WarmOrange,
                unfocusedBorderColor = Color.White,
                cursorColor = WarmOrange
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .shadow(4.dp, RoundedCornerShape(24.dp))
                .testTag("inventory_search_input")
        )

        // 3. Category horizontal scroll row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategoryTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) WarmOrange else Color.White)
                        .border(
                            1.dp,
                            if (isSelected) WarmOrange else LightCream,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            viewModel.itemCategoryFilter.value = category
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("category_tab_$category")
                ) {
                    Text(
                        text = if (category == "低库存") "⚠️ $category" else category,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else TextDark
                    )
                }
            }
            
            // Manage Tags Entry
            item {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, LightCream, CircleShape)
                        .clickable { showManageTags = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "管理标签",
                        tint = GrayTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 4. Inventories Center Lists List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (filteredInventories.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔍",
                            fontSize = 36.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "没有找到符合条件的储备物品哦",
                            fontSize = 14.sp,
                            color = GrayTextMuted,
                            textAlign = TextAlign.Center
                        )
                        if (selectedCategoryTab != "全部") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.itemCategoryFilter.value = "全部" },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmOrange.copy(alpha = 0.1f)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("清除筛选", color = WarmOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredInventories, key = { it.id }) { item ->
                    InventoryGridCard(
                        inventory = item,
                        onIncrement = {
                            viewModel.updateInventoryQuantity(item, 1)
                        },
                        onDecrement = {
                            viewModel.updateInventoryQuantity(item, -1)
                        },
                        onEdit = { onEditRequest(item) },
                        onDelete = { onDeleteRequest(item) }
                    )
                }
            }
        }
    }

    // Tag Management Dialog
    if (showManageTags) {
        ManageTagsDialog(
            viewModel = viewModel,
            onDismiss = { showManageTags = false }
        )
    }
}

@Composable
fun ManageTagsDialog(viewModel: LifeOrderedViewModel, onDismiss: () -> Unit) {
    val tags by viewModel.allTags.collectAsState()
    var newTagName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成", fontWeight = FontWeight.Bold, color = WarmOrange) }
        },
        title = { Text("管理品类标签", fontWeight = FontWeight.Bold, color = TextDark) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("品类标签可以帮你更好地组织物品", fontSize = 12.sp, color = GrayTextMuted)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Add tag input
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { if (it.length <= 6) newTagName = it },
                        placeholder = { Text("新标签(最多6字)", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newTagName.isNotBlank()) {
                                viewModel.addTag(newTagName)
                                newTagName = ""
                            }
                        },
                        modifier = Modifier
                            .background(WarmOrange, RoundedCornerShape(12.dp))
                            .size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Existing tags list
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp)) {
                    tags.forEach { tag ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tag.name, fontWeight = FontWeight.Medium, color = TextDark)
                            IconButton(onClick = { viewModel.deleteTag(tag) }, modifier = Modifier.size(24.dp)) {
                                Text("✕", color = RedWarningText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = WarmCreamBg,
        modifier = Modifier.border(2.dp, Color(0xFFE8E5E0), RoundedCornerShape(24.dp))
    )
}

/**
 * Beautiful detail of inventory item displaying stock ratio progress with tactile adjusters
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
    
    // Calculate stock ratio
    val maxProgress = 1.0f
    val currentRatio = if (inventory.safeQty > 0) {
        inventory.currentQty.toFloat() / inventory.safeQty.toFloat()
    } else {
        1.0f
    }
    
    val progressValue = currentRatio.coerceIn(0.0f, maxProgress)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Row 1: Top section with Icon, Name and Edit/Delete Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon Box
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(LightCream)
                            .border(1.dp, Color(0xFFF5EDE6), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = inventory.iconPath.ifBlank { "🏡" }, fontSize = 24.sp)
                    }
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Text(
                        text = inventory.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                
                // Edit/Delete buttons (Moved to top right)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CreamEditButton(
                        onClick = onEdit,
                        containerSize = 32.dp,
                        iconSize = 16.dp
                    )
                    CreamDeleteButton(
                        onClick = onDelete,
                        containerSize = 32.dp,
                        iconSize = 16.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            // Row 2: Badges and Rapid Click Adjusters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pill badge has been moved down here
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isLow) Color(0xFFFF5A5A).copy(alpha = 0.15f) else Color(0xFF4CAF50).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isLow) "库存不足 ⚠️" else "储备充足 ✅",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLow) Color(0xFFFF5A5A) else Color(0xFF4CAF50)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    Text(
                        text = "安全线: ${inventory.safeQty}",
                        fontSize = 12.sp,
                        color = GrayTextMuted
                    )
                }

                // Rapid Click Adjusters
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(LightCream)
                        .padding(2.dp)
                ) {
                    // Minus Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { onDecrement() }
                            .testTag("item_dec_${inventory.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "−",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }

                    Text(
                        text = "${inventory.currentQty} ${inventory.unit}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        // Feature 4: Color quantity red when low stock
                        color = if (isLow) Color(0xFFFF5A5A) else TextDark,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Plus Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isLow) Color(0xFFFF5A5A) else WarmOrange)
                            .clickable { onIncrement() }
                            .testTag("item_inc_${inventory.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Beautiful Thick Stock Bar Ratio
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "储备进度",
                        fontSize = 11.sp,
                        color = GrayTextMuted
                    )
                    
                    Text(
                        text = "${(currentRatio * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLow) RedWarningText else OrangePrimaryText
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Custom Rounded Progress Row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(LightCream)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressValue)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (isLow) RedWarningText else WarmOrange
                            )
                    )
                }
            }
        }
    }
}
