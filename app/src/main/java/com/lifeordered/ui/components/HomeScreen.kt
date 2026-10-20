package com.lifeordered.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lifeordered.core.date.MomentKind
import com.lifeordered.core.date.MomentSchedule
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.format.InventoryFilter
import com.lifeordered.ui.navigation.AppDestination
import com.lifeordered.ui.navigation.NavigationState
import com.lifeordered.ui.theme.Glass
import com.lifeordered.ui.theme.GlassSectionHeader
import com.lifeordered.ui.theme.LiquidGlassBackground
import com.lifeordered.ui.theme.ScreenFadeIn
import com.lifeordered.ui.theme.accentGlow
import com.lifeordered.ui.theme.liquidGlass
import com.lifeordered.ui.theme.pressableClickable
import com.lifeordered.ui.viewmodel.LifeOrderedViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 应用骨架（液态玻璃版）。
 *
 * 结构要点：
 * - 整屏由 [LiquidGlassBackground] 提供「液体」，所有面板浮在其上；
 * - Scaffold 背景透明，避免 Material 的不透明背景把光斑盖住；
 * - 底部导航栏本身也是一块玻璃，而不是白色实心条；
 * - Snackbar 通过 [LocalSnackbarHostState] 下发，各页面无需层层透传回调。
 */
@Composable
fun HomeScreen(
    viewModel: LifeOrderedViewModel,
    modifier: Modifier = Modifier
) {
    val nav = remember { NavigationState() }
    val destination = nav.current
    val snackbarHostState = remember { SnackbarHostState() }
    val notify = rememberNotifierOutsideHost(snackbarHostState)

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

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        LiquidGlassBackground(modifier = modifier) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                // 关键：透明背景，否则会把液体光斑整块盖住
                containerColor = Color.Transparent,
                snackbarHost = { GlassSnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (nav.showsBottomBar) {
                        GlassBottomBar(
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
                    ScreenFadeIn(key = destination) {
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
                    }

                    // 页面切换淡入：key 用导航目的地，同一目的地重组不会重播
                    // （注意：上面 when 的内容缩进比这里浅一层，是为避免整段重排；
                    //  Kotlin 不依赖缩进，包进 ScreenFadeIn 只是为了拿到入场动画）

                    // ---------------- 删除确认 ----------------
                    itemToDelete?.let { item ->
                        ActionConfirmBottomSheet(
                            title = if (item is Moment) "删除这个时刻？" else "确认删除？",
                            content = if (item is Moment) {
                                "会移到「我的 · 回收站」，之后仍可恢复。"
                            } else {
                                "会移到回收站，之后仍可恢复。"
                            },
                            confirmText = "删除",
                            isWarning = true,
                            onConfirm = {
                                when (item) {
                                    is Moment -> viewModel.moveToTrash(item)
                                    is Consumable -> viewModel.moveToTrash(item)
                                    is Inventory -> viewModel.moveToTrash(item)
                                }
                                itemToDelete = null
                                notify("已移到回收站")
                            },
                            onCancel = { itemToDelete = null },
                            onDismissRequest = { itemToDelete = null }
                        )
                    }

                    // ---------------- 归档确认 ----------------
                    momentToArchive?.let { moment ->
                        ActionConfirmBottomSheet(
                            title = "归档这个时刻？",
                            content = "归档后不再出现在首页，可在「我的 · 历史时刻归档」里找回。",
                            confirmText = "归档",
                            isWarning = false,
                            onConfirm = {
                                viewModel.updateMomentArchiveStatus(moment, true)
                                momentToArchive = null
                                notify("已归档")
                            },
                            onCancel = { momentToArchive = null },
                            onDismissRequest = { momentToArchive = null }
                        )
                    }

                    // ---------------- 录入 / 编辑 ----------------
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
    }
}

/**
 * 在 Host 之外构造 notifier。
 *
 * [rememberNotifier] 依赖 CompositionLocal，而 Host 状态此刻就在手边，
 * 所以首页自己直接构造一份，避免多一层 Provider 才能拿到。
 */
@Composable
private fun rememberNotifierOutsideHost(
    hostState: SnackbarHostState
): (String) -> Unit {
    val scope = rememberCoroutineScope()
    return remember(hostState, scope) {
        { message: String ->
            scope.launch {
                hostState.currentSnackbarData?.dismiss()
                hostState.showSnackbar(message)
            }
        }
    }
}

/** 玻璃底部导航栏。 */
@Composable
private fun GlassBottomBar(
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .height(66.dp)
            .liquidGlass(
                shape = Glass.panelShape,
                fill = Color.Transparent,
                elevation = 18.dp,
                glow = Color.Transparent
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
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

            // 中央添加按钮：强调色 + 辉光，是整个界面唯一的高饱和元素
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .accentGlow(Glass.Accent, shape = CircleShape, elevation = 12.dp)
                    .clip(CircleShape)
                    .background(Glass.Accent)
                    .pressableClickable(pressedScale = 0.92f, onClick = onAddClick)
                    .testTag("main_add_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加",
                    tint = Glass.OnAccent,
                    modifier = Modifier.size(23.dp)
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

/** `Items` 带参数，不能直接用 `==` 判断是否同一 Tab。 */
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
    val notify = rememberNotifier()

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
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            TopHeaderSection(
                greeting = viewModel.getGreeting(),
                onProfileClick = { notify("生活的主人：shyx@gmail.com") }
            )
            OrganicBannerBlock(itemCount = activeCount)
        }

        if (moments.isEmpty() && consumables.isEmpty() && inventories.isEmpty()) {
            item { GlobalEmptyStateBlock() }
        } else {
            for (sectionKey in viewModel.homeSectionOrder) {
                when (sectionKey) {
                    "moments" -> {
                        item {
                            GlassSectionHeader(
                                title = "重要时刻",
                                trailingText = "全部",
                                onTrailingClick = onOpenAllMoments
                            )
                        }
                        item {
                            if (moments.isEmpty()) {
                                EmptyStateBlock("还没有记录任何时刻，点下方 ＋ 开始吧")
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(206.dp)
                                ) {
                                    items(moments) { moment ->
                                        MomentCardItem(
                                            moment = moment,
                                            onComplete = {
                                                viewModel.completeMoment(moment)
                                                notify(
                                                    if (moment.kind == MomentKind.COUNT_UP) {
                                                        "已记录一次"
                                                    } else {
                                                        "打卡成功"
                                                    }
                                                )
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
                            GlassSectionHeader(
                                title = "到期提醒",
                                trailingText = "全部",
                                onTrailingClick = onOpenAllUpcoming
                            )
                        }
                        item {
                            if (consumables.isEmpty()) {
                                EmptyStateBlock("还没有追踪任何耗材，点下方 ＋ 记录一件")
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(178.dp)
                                ) {
                                    items(consumables) { consumable ->
                                        ConsumableCardItem(
                                            consumable = consumable,
                                            onReset = {
                                                viewModel.resetConsumable(consumable)
                                                notify("已重置使用周期")
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
                            GlassSectionHeader(
                                title = "库存物品",
                                trailingText = "全部",
                                onTrailingClick = onOpenLowStockItems
                            )
                        }

                        val lowInventories = inventories.filter { it.isLow() }
                        if (lowInventories.isEmpty()) {
                            item { EmptyStateBlock("全屋库存都在安全线以上") }
                        } else {
                            items(lowInventories) { inventory ->
                                InventoryCardItem(
                                    inventory = inventory,
                                    onEdit = { onEditInventory(inventory) },
                                    onDelete = { onDeleteRequest(inventory) },
                                    onIncrease = {
                                        viewModel.updateInventoryQuantity(inventory, 1)
                                        notify("已补货 1 ${inventory.unit}")
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
