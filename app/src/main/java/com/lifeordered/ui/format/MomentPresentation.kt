package com.lifeordered.ui.format

import androidx.compose.ui.graphics.Color
import com.lifeordered.core.date.MomentDays
import com.lifeordered.core.date.MomentKind
import com.lifeordered.core.date.MomentRepeat
import com.lifeordered.core.date.MomentSchedule
import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.data.models.Moment
import com.lifeordered.ui.theme.OrangePrimaryText
import com.lifeordered.ui.theme.RedWarningText
import com.lifeordered.ui.theme.TextDark
import java.util.Calendar

/**
 * 时刻在 UI 上的**唯一呈现规则**。
 *
 * ## 为什么需要这个文件
 *
 * 重构前同一份数据在 4 个界面用 4 套规则渲染，用户会看到互相矛盾的结果：
 *
 * | 位置 | 公历标签 | 逾期是否变红 | 图标分支 |
 * |---|---|---|---|
 * | 首页卡片 | `公历 M/D` | 否（显示负数仍用橙色） | 0~3 四种黏土图标 |
 * | 全部时刻 | `M月D日`（无前缀） | 是 | 0/1/其它 三种 |
 * | 归档列表 | `M月D日`（无前缀） | 不显示天数 | 硬编码 📦 |
 * | 日历事件行 | `公历 M月D日` | 不显示天数 | 走 cardColorIndex 的 4 种 Pastel 底色 |
 *
 * 现在全部收敛到本文件：文案、颜色、图标槽位、按钮文案都从这里取。
 * 新增界面只需调用这里的方法，就不会再产生第 5 套规则。
 */
object MomentPresentation {

    /** 黏土手绘图标的槽位数量（蛋糕 / 礼物 / 爱心 / 派对帽）。 */
    const val ICON_SLOT_COUNT = 4

    // ---------------------------------------------------------------------
    // 日期标签
    // ---------------------------------------------------------------------

    /**
     * 统一的日期标签。
     *
     * @param withSolarConversion 农历时是否追加「（公历 …）」。首页卡片需要，
     *        列表行不需要（太长会挤掉标题）。
     */
    fun dateLabel(moment: Moment, withSolarConversion: Boolean = false): String {
        if (!moment.isLunar) {
            return "公历 ${moment.month}月${moment.day}日"
        }
        val lunarLabel = "农历 ${if (moment.isLeapMonth) "闰" else ""}${moment.month}月${moment.day}日"
        if (!withSolarConversion) return lunarLabel

        val solar = CalendarHelper.solarDateInGregorianYear(
            lunarMonth = moment.month,
            lunarDay = moment.day,
            isLeapMonth = moment.isLeapMonth
        ) ?: return lunarLabel

        val year = solar.get(Calendar.YEAR)
        val month = solar.get(Calendar.MONTH) + 1
        val day = solar.get(Calendar.DAY_OF_MONTH)
        return "$lunarLabel（公历 $year年${month}月${day}日）"
    }

    /** 类型标签：倒数日 / 正数日。 */
    fun kindLabel(moment: Moment): String = when (moment.kind) {
        MomentKind.COUNTDOWN -> "倒数日"
        MomentKind.COUNT_UP -> "正数日"
    }

    /** 周期标签：不重复 / 每周 / 每月 / 每年。 */
    fun repeatLabel(moment: Moment): String = when (moment.repeat) {
        MomentRepeat.NONE -> "不重复"
        MomentRepeat.WEEKLY -> "每周"
        MomentRepeat.MONTHLY -> "每月"
        MomentRepeat.YEARLY -> "每年"
    }

    /** 列表行副标题：日期 · 类型 · 周期。 */
    fun listSubtitle(moment: Moment): String {
        val parts = mutableListOf(dateLabel(moment), kindLabel(moment))
        if (moment.repeat.isRecurring) parts.add(repeatLabel(moment))
        return parts.joinToString(" · ")
    }

    // ---------------------------------------------------------------------
    // 天数呈现
    // ---------------------------------------------------------------------

    /** 天数大数字。无法解析时返回占位符，而不是误导性的 0。 */
    fun dayValue(days: MomentDays): String = when (days) {
        is MomentDays.Countdown -> days.days.toString()
        is MomentDays.CountUp -> days.elapsedDays.toString()
        MomentDays.Invalid -> "—"
    }

    /** 天数说明文案（大数字下方 / 前缀）。 */
    fun dayCaption(days: MomentDays): String = when (days) {
        is MomentDays.Countdown -> if (days.isOverdue) "逾期" else "剩余"
        is MomentDays.CountUp -> "已过"
        MomentDays.Invalid -> "日期无效"
    }

    /**
     * 天数颜色。
     *
     * 修复的缺陷：重构前首页卡片只按 `isCountUp` 二选一，逾期的一次性倒数日
     * 会显示「-3」却仍是正常橙色；红色只在「全部时刻」列表里出现。
     */
    fun dayColor(days: MomentDays): Color = when (days) {
        is MomentDays.Countdown -> if (days.isOverdue) RedWarningText else OrangePrimaryText
        is MomentDays.CountUp -> TextDark
        MomentDays.Invalid -> RedWarningText
    }

    /** 首页卡片副标题。 */
    fun cardSubtitle(moment: Moment): String = when (moment.kind) {
        MomentKind.COUNTDOWN -> "正在加速到来"
        MomentKind.COUNT_UP -> "已安稳度过"
    }

    /**
     * 打卡按钮文案。
     *
     * 正数日的语义是「记录一次」，不是「达成」——它的天数不会因打卡重置。
     */
    fun checkInLabel(moment: Moment): String {
        val times = moment.completionCount
        return when (moment.kind) {
            MomentKind.COUNT_UP ->
                if (times > 0) "✓ 已记录 (第 $times 次)" else "✓ 记录一次"
            MomentKind.COUNTDOWN ->
                if (times > 0) "✓ 已达成 (第 $times 次)" else "✓ 已达成"
        }
    }

    // ---------------------------------------------------------------------
    // 图标
    // ---------------------------------------------------------------------

    /**
     * 黏土图标槽位，恒在 `0 until ICON_SLOT_COUNT`。
     *
     * 修复的缺陷：重构前 `cardColorIndex` 由 `(0..2).random()` 生成，
     * 第 4 种（派对帽）永远不可达，而「全部时刻」列表只识别到 index 2。
     */
    fun iconSlot(moment: Moment): Int = moment.cardColorIndex.coerceIn(0, ICON_SLOT_COUNT - 1)

    /** 日历事件行使用的日历来源图标。 */
    fun calendarGlyph(moment: Moment): String = if (moment.isLunar) "🏮" else "📅"

    // ---------------------------------------------------------------------
    // 便捷判定
    // ---------------------------------------------------------------------

    /** 一次性倒数日是否已逾期。 */
    fun isOverdue(moment: Moment, nowMillis: Long = System.currentTimeMillis()): Boolean =
        (MomentSchedule.resolve(moment, nowMillis) as? MomentDays.Countdown)?.isOverdue == true
}
