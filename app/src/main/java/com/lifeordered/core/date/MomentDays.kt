package com.lifeordered.core.date

/**
 * 时刻天数计算的**唯一结果类型**。
 *
 * 旧实现用 `Int` 返回，并用 `0` 同时表示「就是今天」和「日期非法」，
 * 导致错误状态被伪装成正常值（UI 显示「0 天」而无任何提示）。
 * 这里把「算不出来」提升为独立的 [Invalid] 分支，UI 必须显式处理。
 */
sealed interface MomentDays {

    /**
     * 倒数日结果。
     *
     * @param days 非负的天数绝对值。逾期时表示「已经过了几天」。
     * @param isOverdue 目标日已经过去（仅 `repeat = NONE` 的一次性事件可能为 true）。
     */
    data class Countdown(val days: Int, val isOverdue: Boolean) : MomentDays

    /**
     * 正数日结果。
     *
     * @param elapsedDays 从起始日到今天已经过去的天数，恒 >= 0（起始日在未来时为 0）。
     */
    data class CountUp(val elapsedDays: Int) : MomentDays

    /** 日期无法解析（公历 2 月 30 日、农历不存在的闰月、超出搜索窗口等）。 */
    object Invalid : MomentDays

    /** 是否为可正常展示的状态。 */
    val isValid: Boolean get() = this !== Invalid
}
