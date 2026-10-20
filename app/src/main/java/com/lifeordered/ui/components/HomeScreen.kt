package com.lifeordered.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lifeordered.core.date.MomentSchedule
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.format.InventoryFilter
import com.lifeordered.ui.navigation.AppDestination
import com.lifeordered.ui.navigation.NavigationState
import com.lifeordered.ui.theme.*
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel
import java.util.Calendar

/**
 * 应用骨架：底部导航 + 目的地路由 + 全局弹层宿主。
 *
 * 导航状态由 [NavigationState] 持有，不再依赖 ViewModel 里的 `Int` / `String?`。
 */
@Composable
fun HomeScreen(
    viewModel: LifeOrderedViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val nav = remember { NavigationState() }
    val destination = nav.current

    val moments by viewModel.activeMoments.collectAsState()
    val consumables by viewModel.allConsumables.collectAsState()
    val inventories by viewModel.allInventories.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }

    var editMoment by remember { mutableStateOf<Moment?>(null) }
    var editConsumable by remember { mutableStateOf<Consumable?>(null) }
    var editInventory by remember { mutableStateOf<Inventory?>(null) }

    var itemToDelete by remember { mutableStateOf<Any?>(null) }
    var momentToArchive by remember { mutableStateOf<Moment?>(null) }

    fun openEditSheet(
        moment: Moment? = null,
        consumable: Consumable? = null,
        inventory: Inventory? = null
    ) {
        editMoment = moment
        editConsumable = consumable
        editInventory = inventory
        showAddSheet = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmCreamBg,
        bottomBar = {
            if (nav.showsBottomBar) {
                BottomNavigationBar(
                    current = destination,
                    onSelect = { nav.selectTab(it) },
                    onAddClick = { openEditSheet() }
                )
            }
        }
    ) { innerPadding ->
        BackHandler(enabled = nav.canGoBack) { nav.pop() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (destination) {
                AppDestination.Home -> HomeScreenContent(
                    viewModel = viewModel,
                    moments = moments,
                    consumables = consumables,
                    inventories = inventories,
                    onOpenAllMoments = { nav.push(AppDestination.AllMoments) },
                    onOpenAllUpcoming = { nav.push(AppDestination.AllUpcoming) },
                    onOpenLowStockItems = {
                        nav.selectTab(AppDestination.Items(InventoryFilter.LOW_STOCK))
                    },
                    onEditMoment = { openEditSheet(moment = it) },
                    onEditConsumable = { openEditSheet(consumable = it) },
                    onEditInventory = { openEditSheet(inventory = it) },
                    onDeleteRequest = { itemToDelete = it },
                    onArchiveRequest = { momentToArchive = it }
                )

                AppDestination.Calendar -> CalendarScreen(viewModel = viewModel)

                is AppDestination.Items -> ItemsScreen(
                    viewModel = viewModel,
                    initialFilter = destination.initialFilter,
                    onEditRequest = { openEditSheet(inventory = it) },
                    onDeleteRequest = { itemToDelete = it }
                )

                AppDestination.Profile -> ProfileScreen(
                    viewModel = viewModel,
                    onNavigate = { nav.push(it) }
                )

                AppDestination.AllMoments -> AllMomentsScreen(
                    viewModel = viewModel,
                    onBack = { nav.pop() },
                    onEdit = { openEditSheet(moment = it) },
                    onDelete = { itemToDelete = it }
                )

                AppDestination.AllUpcoming -> AllUpcomingScreen(
                    viewModel = viewModel,
                    onBack = { nav.pop() },
                    onEdit = { openEditSheet(consumable = it) },
                    onDelete = { itemToDelete = it }
                )

                AppDestination.ArchivedMoments -> ArchivedMomentsScreen(
                    viewModel = viewModel,
                    onBack = { nav.pop() }
                )

                AppDestination.TrashBin -> TrashBinScreen(
                    viewModel = viewModel,
                    onBack = { nav.pop() }
                )

                AppDestination.SectionOrdering -> HomeSectionOrderingScreen(
                    viewModel = viewModel,
                    onBack = { nav.pop() }
                )
            }

            // ---------------- 删除确认 ----------------
            itemToDelete?.let { item ->
                val titleText = if (item is Moment) "确认删除这个时刻吗？" else "确认删除吗？"
                val bodyText = if (item is Moment) {
                    "该时刻将被移入「我的 - 回收站」中，你随时可以在回收站中将其恢复。"
                } else {
                    "该记录将被移入回收站中。"
                }

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

            // ---------------- 归档确认 ----------------
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

            // ---------------- 录入 / 编辑面板 ----------------
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

/** 底部导航栏：4 个一级 Tab + 中央添加按钮。 */
@Composable
private fun BottomNavigationBar(
    current: AppDestination,
    onSelect: (AppDestination) -> Unit,
    onAddClick: () -> Unit
) {
    val tabs: List<Triple<AppDestination, String, ImageVector>> = listOf(
        Triple(AppDestination.Home, "首页", Icons.Default.Home),
        Triple(AppDestination.Calendar, "日程", Icons.Default.DateRange),
        Triple(AppDestination.Items(), "物品", Icons.Default.ShoppingCart),
        Triple(AppDestination.Profile, "我的", Icons.Default.Person)
    )

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
            tabs.take(2).forEach { (tabDestination, label, icon) ->
                BottomNavItem(
                    label = label,
                    icon = icon,
                    selected = isSameRootTab(current, tabDestination),
                    onClick = { onSelect(tabDestination) }
                )
            }

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
                    .clickable(onClick = onAddClick)
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

            tabs.drop(2).forEach { (tabDestination, label, icon) ->
                BottomNavItem(
                    label = label,
                    icon = icon,
                    selected = isSameRootTab(current, tabDestination),
                    onClick = { onSelect(tabDestination) }
                )
            }
        }
    }
}

/**
 * 判断两个目的地是否指向同一个一级 Tab。
 *
 * `Items` 带有 `initialFilter` 参数，因此不能直接用 `==` 比较——
 * 用户从首页带着「低库存」筛选进来时，物品 Tab 仍应高亮。
 */
private fun isSameRootTab(current: AppDestination, tab: AppDestination): Boolean {
    if (current is AppDestination.Items && tab is AppDestination.Items) return true
    return current == tab
}

@Composable
fun HomeScreenContent(
    viewModel: LifeOrderedViewModel,
    moments: List<Moment>,
    consumables: List<Consumable>,
    inventories: List<Inventory>,
    onOpenAllMoments: () -> Unit,
    onOpenAllUpcoming: () -> Unit,
    onOpenLowStockItems: () -> Unit,
    onEditMoment: (Moment) -> Unit,
    onEditConsumable: (Consumable) -> Unit,
    onEditInventory: (Inventory) -> Unit,
    onDeleteRequest: (Any) -> Unit,
    onArchiveRequest: (Moment) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 今日待处理数量：用统一的 MomentSchedule 口径，避免与卡片天数算出不同结果
    val activeCount = remember(moments, consumables, inventories) {
        val today = Calendar.getInstance()
        val year = today.get(Calendar.YEAR)
        val month = today.get(Calendar.MONTH) + 1
        val day = today.get(Calendar.DAY_OF_MONTH)

        moments.count { MomentSchedule.occursOn(it, year, month, day) } +
            consumables.count { it.getRemainingDays() <= 0 } +
            inventories.count { it.isLow() }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            TopHeaderSection(
                greeting = viewModel.getGreeting(),
                onProfileClick = {
                    Toast.makeText(context, "生活的主人：shyx@gmail.com 🌸", Toast.LENGTH_SHORT).show()
                }
            )
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
                            SectionHeader(title = "重要时刻", onAllClick = onOpenAllMoments)
                        }

                        item {
                            if (moments.isEmpty()) {
                                EmptyStateBlock("暂无记录时刻，点击下方 '+' 按钮添加吧~")
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
                                ) {
                                    items(moments) { moment ->
                                        MomentCardItem(
                                            moment = moment,
                                            onComplete = {
                                                viewModel.completeMoment(moment)
                                                Toast.makeText(
                                                    context,
                                                    if (moment.kind == com.lifeordered.core.date.MomentKind.COUNT_UP) {
                                                        "🌱 已记录一次！"
                                                    } else {
                                                        "🎉 打卡成功！"
                                                    },
                                                    Toast.LENGTH_SHORT
                                                ).show()
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
                            SectionHeader(title = "到期提醒", onAllClick = onOpenAllUpcoming)
                        }

                        item {
                            if (consumables.isEmpty()) {
                                EmptyStateBlock("暂无寿命追踪，点击下方 '+' 记录重要耗材")
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
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
                            SectionHeader(title = "库存物品", onAllClick = onOpenLowStockItems)
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
                                    onIncrease = { viewModel.updateInventoryQuantity(inventory, 1) },
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
