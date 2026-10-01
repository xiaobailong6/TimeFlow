package com.lifeordered.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory
import com.lifeordered.data.models.Moment
import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: LifeOrderedViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val moments by viewModel.activeMoments.collectAsState()
    val consumables by viewModel.allConsumables.collectAsState()
    val inventories by viewModel.allInventories.collectAsState()

    val activeTab by viewModel.activeTabState
    val detailScreen by viewModel.detailScreenState
    var showAddSheet by remember { mutableStateOf(false) }

    // State for editing items
    var editMoment by remember { mutableStateOf<Moment?>(null) }
    var editConsumable by remember { mutableStateOf<Consumable?>(null) }
    var editInventory by remember { mutableStateOf<Inventory?>(null) }

    // State for delete confirmation
    var itemToDelete by remember { mutableStateOf<Any?>(null) }
    var momentToArchive by remember { mutableStateOf<Moment?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmCreamBg,
        bottomBar = {
            if (detailScreen == null) {
                BottomAppBar(
                    modifier = Modifier
                        .height(80.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .shadow(16.dp),
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(2) { index ->
                            BottomNavItem(
                                index = index,
                                activeTab = activeTab,
                                onClick = { viewModel.onTabSelected(index) }
                            )
                        }
                        
                        // Aligned Add Button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = CircleShape,
                                    ambientColor = Color(0x33FF7E40),
                                    spotColor = Color(0x66FF7E40)
                                )
                                .clip(CircleShape)
                                .background(WarmOrange)
                                .clickable {
                                    editMoment = null
                                    editConsumable = null
                                    editInventory = null
                                    showAddSheet = true
                                }
                                .testTag("main_add_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        repeat(2) { index ->
                            val actualIndex = index + 2
                            BottomNavItem(
                                index = actualIndex,
                                activeTab = activeTab,
                                onClick = { viewModel.onTabSelected(actualIndex) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BackHandler(enabled = detailScreen != null) {
            viewModel.navigateToDetail(null)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (detailScreen != null) {
                when (detailScreen) {
                    "all_moments" -> AllMomentsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateToDetail(null) },
                        onEdit = { 
                            editMoment = it
                            showAddSheet = true
                        },
                        onDelete = { itemToDelete = it }
                    )
                    "all_upcoming" -> AllUpcomingScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateToDetail(null) },
                        onEdit = { 
                            editConsumable = it
                            showAddSheet = true
                        },
                        onDelete = { itemToDelete = it }
                    )
                }
            } else {
                val tYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
                val tMonth = remember { Calendar.getInstance().get(Calendar.MONTH) + 1 }
                val tDay = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
                
                val activeCount = remember(moments, consumables, inventories, tYear, tMonth, tDay) {
                    moments.count {
                        CalendarHelper.isMomentOnDate(it, tYear, tMonth, tDay)
                    } + consumables.count {
                        it.getRemainingDays() <= 0
                    } + inventories.count { it.isLow() }
                }

                when (activeTab) {
                    0 -> HomeScreenContent(
                        viewModel = viewModel,
                        moments = moments,
                        consumables = consumables,
                        inventories = inventories,
                        activeCount = activeCount,
                        onEditMoment = {
                            editMoment = it
                            showAddSheet = true
                        },
                        onEditConsumable = {
                            editConsumable = it
                            showAddSheet = true
                        },
                        onEditInventory = {
                            editInventory = it
                            showAddSheet = true
                        },
                        onDeleteRequest = { itemToDelete = it },
                        onArchiveRequest = { momentToArchive = it }
                    )
                    1 -> CalendarScreen(viewModel = viewModel)
                    2 -> ItemsScreen(
                        viewModel = viewModel,
                        onEditRequest = {
                            editInventory = it
                            showAddSheet = true
                        },
                        onDeleteRequest = { itemToDelete = it }
                    )
                    3 -> ProfileScreen(viewModel = viewModel)
                }
            }

            // Delete Confirm Dialog
            itemToDelete?.let { item ->
                val titleText = if (item is Moment) "确认删除这个时刻吗？" else "确认删除吗？"
                val bodyText = if (item is Moment) "该时刻将被移入“我的-回收站”中，你随时可以在回收站中将其恢复。" else "该记录将被移入回收站中。"
                
                ActionConfirmBottomSheet(
                    title = titleText,
                    content = bodyText,
                    confirmText = "确认删除",
                    isWarning = true,
                    onConfirm = {
                        when (item) {
                            is Moment -> viewModel.moveToTrash(item)
                            is Consumable -> viewModel.moveToTrash(item)
                            is Inventory -> viewModel.moveToTrash(item)
                        }
                        itemToDelete = null
                    },
                    onCancel = { itemToDelete = null },
                    onDismissRequest = { itemToDelete = null }
                )
            }

            // Archive Confirm Dialog
            momentToArchive?.let { moment ->
                ActionConfirmBottomSheet(
                    title = "要归档这条时刻吗？",
                    content = "归档后将不再显示于首页，可前往「我的 - 历史时刻归档」中查看或恢复。",
                    confirmText = "归档",
                    isWarning = false,
                    onConfirm = {
                        viewModel.updateMomentArchiveStatus(moment, true)
                        momentToArchive = null
                    },
                    onCancel = { momentToArchive = null },
                    onDismissRequest = { momentToArchive = null }
                )
            }

            if (showAddSheet) {
                AddEntryBottomSheet(
                    onDismissRequest = { 
                        showAddSheet = false
                        editMoment = null
                        editConsumable = null
                        editInventory = null
                    },
                    viewModel = viewModel,
                    editMoment = editMoment,
                    editConsumable = editConsumable,
                    editInventory = editInventory
                )
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    viewModel: LifeOrderedViewModel,
    moments: List<com.lifeordered.data.models.Moment>,
    consumables: List<com.lifeordered.data.models.Consumable>,
    inventories: List<com.lifeordered.data.models.Inventory>,
    activeCount: Int,
    onEditMoment: (Moment) -> Unit,
    onEditConsumable: (Consumable) -> Unit,
    onEditInventory: (Inventory) -> Unit,
    onDeleteRequest: (Any) -> Unit,
    onArchiveRequest: (Moment) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Header & Banner
        item {
            TopHeaderSection(
                greeting = viewModel.getGreeting(),
                onProfileClick = {
                Toast.makeText(context, "生活的主人：shyx@gmail.com 🌸", Toast.LENGTH_SHORT).show()
            })
            OrganicBannerBlock(itemCount = activeCount)
        }

        if (moments.isEmpty() && consumables.isEmpty() && inventories.isEmpty()) {
            item { GlobalEmptyStateBlock() }
        } else {
            val orderList = viewModel.homeSectionOrder
            for (sectionKey in orderList) {
                when (sectionKey) {
                    "moments" -> {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeader(
                                title = "重要时刻",
                                onAllClick = { viewModel.navigateToDetail("all_moments") }
                            )
                        }

                        item {
                            if (moments.isEmpty()) {
                                EmptyStateBlock("暂无记录时刻，点击下方 '+' 按钮添加吧~")
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth().height(210.dp)
                                ) {
                                    items(moments) { moment ->
                                        MomentCardItem(
                                            moment = moment,
                                            onComplete = {
                                                viewModel.completeMoment(moment)
                                                Toast.makeText(context, "🎉 打卡成功！", Toast.LENGTH_SHORT).show()
                                            },
                                            onDelete = { onDeleteRequest(moment) },
                                            onEdit = { onEditMoment(moment) },
                                            onArchive = { onArchiveRequest(moment) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "alerts" -> {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            SectionHeader(
                                title = "到期提醒",
                                onAllClick = { viewModel.navigateToDetail("all_upcoming") }
                            )
                        }

                        item {
                            if (consumables.isEmpty()) {
                                EmptyStateBlock("暂无寿命追踪，点击下方 '+' 记录重要耗材")
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth().height(180.dp)
                                ) {
                                    items(consumables) { consumable ->
                                        ConsumableCardItem(
                                            consumable = consumable,
                                            onReset = {
                                                viewModel.resetConsumable(consumable)
                                                Toast.makeText(context, "🔄 已重置！", Toast.LENGTH_SHORT).show()
                                            },
                                            onDelete = { onDeleteRequest(consumable) },
                                            onEdit = { onEditConsumable(consumable) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "inventory" -> {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            SectionHeader(
                                title = "库存物品",
                                onAllClick = {
                                    viewModel.itemCategoryFilter.value = "low"
                                    viewModel.onTabSelected(2)
                                }
                            )
                        }

                        val lowInventories = inventories.filter { it.isLow() }
                        if (lowInventories.isEmpty()) {
                            item { EmptyStateBlock("全屋库存充足。太好啦！") }
                        } else {
                            items(lowInventories) { inventory ->
                                InventoryCardItem(
                                    inventory = inventory,
                                    onEdit = { onEditInventory(inventory) },
                                    onDelete = { onDeleteRequest(inventory) },
                                    onIncrease = { 
                                        viewModel.updateInventoryQuantity(inventory, 1)
                                    },
                                    onDecrease = {
                                        if (inventory.currentQty > 0) {
                                            viewModel.updateInventoryQuantity(inventory, -1)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
