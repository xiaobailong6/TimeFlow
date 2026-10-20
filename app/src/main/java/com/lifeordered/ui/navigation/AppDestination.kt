package com.lifeordered.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import com.lifeordered.ui.format.InventoryFilter

/**
 * 应用内所有可到达的界面。
 *
 * ## 重构背景
 *
 * 重构前没有任何导航抽象，界面切换靠三种互相独立的机制拼装：
 * - `ViewModel.activeTabState: Int`（0~3 对应四个 Tab）
 * - `ViewModel.detailScreenState: String?`（`"all_moments"` / `"all_upcoming"`）
 * - `ProfileScreen` 内三个 `remember { mutableStateOf(false) }` 布尔
 *
 * 结果是：没有返回栈（系统返回键只在 `detailScreenState` 有值时生效）、
 * Profile 子页进入后底部导航栏仍然显示、深链无法表达、拼写错误无法被编译器发现。
 *
 * 现在统一为一个类型化的目的地栈，返回栈、底部栏显隐、系统返回键都由栈驱动。
 */
sealed interface AppDestination {

    /** 是否为底部导航的一级页面（一级页面不显示返回箭头）。 */
    val isRootTab: Boolean get() = false

    object Home : AppDestination {
        override val isRootTab: Boolean get() = true
    }

    object Calendar : AppDestination {
        override val isRootTab: Boolean get() = true
    }

    /**
     * 物品页。
     *
     * @param initialFilter 进入时的默认筛选。首页「库存物品 → 全部 >」用它
     *        直接落到「低库存」，不再需要跨页写 ViewModel 字段。
     */
    data class Items(val initialFilter: String = InventoryFilter.ALL) : AppDestination {
        override val isRootTab: Boolean get() = true
    }

    object Profile : AppDestination {
        override val isRootTab: Boolean get() = true
    }

    object AllMoments : AppDestination
    object AllUpcoming : AppDestination
    object ArchivedMoments : AppDestination
    object TrashBin : AppDestination
    object SectionOrdering : AppDestination
}

/**
 * 简单的目的地栈。
 *
 * 作为 Compose 状态持有在 `HomeScreen` 内，而不是放进 ViewModel——
 * 导航是纯粹的 UI 关注点，放进 ViewModel 会让「返回」变成一次业务调用，
 * 也无法被 `rememberSaveable` 之类的手段自然接管。
 */
class NavigationState(start: AppDestination = AppDestination.Home) {

    private val stack = mutableStateListOf(start)

    val current: AppDestination get() = stack.last()

    val canGoBack: Boolean get() = stack.size > 1

    /** 只有一级页面才显示底部导航栏。 */
    val showsBottomBar: Boolean get() = current.isRootTab

    /** 切换一级 Tab：清空栈，避免返回键退回到上一个 Tab 的历史里。 */
    fun selectTab(destination: AppDestination) {
        stack.clear()
        stack.add(destination)
    }

    fun push(destination: AppDestination) {
        stack.add(destination)
    }

    /** @return 是否消费了本次返回。 */
    fun pop(): Boolean {
        if (!canGoBack) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}
