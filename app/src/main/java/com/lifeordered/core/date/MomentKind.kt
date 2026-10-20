package com.lifeordered.core.date

/**
 * 时刻的「方向」：只决定天数如何呈现，不决定出现规律。
 *
 * 旧实现用裸 `String`（"countdown" / "countUp"）在 6 个文件里各自比较，
 * 拼写错误不会被编译器发现。这里收敛成枚举，原始值仍按原样落库，
 * 因此**不需要数据库迁移**。
 */
enum class MomentKind(val raw: String) {
    /** 倒数日：距目标日还有几天。 */
    COUNTDOWN("countdown"),

    /** 正数日：从起始日起已经过去几天。 */
    COUNT_UP("countUp");

    companion object {
        fun fromRaw(raw: String?): MomentKind =
            values().firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: COUNTDOWN
    }
}

/**
 * 时刻的「周期」：决定它在日历上何时重复出现。
 *
 * 与 [MomentKind] 正交——「方向」和「周期」是两个独立维度。
 * 旧实现把两者混在一个二段/四段选择器里且互不约束，导致
 * 「正数日 + 每年」这类组合静默失效。
 */
enum class MomentRepeat(val raw: String) {
    NONE("none"),
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    YEARLY("yearly");

    /** 是否为循环事件（非一次性）。 */
    val isRecurring: Boolean get() = this != NONE

    companion object {
        fun fromRaw(raw: String?): MomentRepeat =
            values().firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: NONE
    }
}
