package com.lifeordered.ui.format

/**
 * 物品页筛选项的**唯一常量来源**。
 *
 * ## 重构背景
 *
 * 重构前这个值以字符串字面量散落在两个文件里，且**拼写不一致**：
 * - `HomeScreen` 点「库存物品 → 全部 >」时写入 `"low"`
 * - `ItemsScreen` 判定低库存时比较 `"低库存"`
 *
 * 两者永远不相等，因此从首页跳到物品页后「低库存」筛选静默失效，
 * 用户看到的是全部物品，而不是他刚刚点击的那一批。
 */
object InventoryFilter {
    const val ALL = "全部"
    const val LOW_STOCK = "低库存"
}
