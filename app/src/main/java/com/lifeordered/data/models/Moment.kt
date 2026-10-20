package com.lifeordered.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lifeordered.core.date.MomentKind
import com.lifeordered.core.date.MomentRepeat

@Entity(tableName = "moments")
data class Moment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val isLunar: Boolean = false, // Whether it uses Chinese Lunar Calendar
    val isLeapMonth: Boolean = false, // Whether the lunar month is a leap month
    val isCompleted: Boolean = false, // Legacy: never written by any code path
    /**
     * Legacy 冗余字段：旧实现用 `isYearly = (repeatType == "yearly")` 双写，
     * 但读取方一半读它、一半读 [repeatType]，造成口径分叉。
     * 重构后**不再读取**该字段，周期判定统一走 [repeat]。
     * 保留列仅为避免数据库迁移导致用户数据丢失。
     */
    val isYearly: Boolean = true,
    val cardColorIndex: Int = 0, // 0 蛋糕 / 1 礼物 / 2 爱心 / 3 派对帽
    val type: String = "countdown", // "countdown" or "countUp"
    val notes: String? = null,
    val repeatType: String = "none", // "none", "weekly", "monthly", "yearly"
    val themeColorHex: String = "#FF7E40",
    val autoArchive: Boolean = false,
    val isArchived: Boolean = false,
    val iconPath: String = "", // 实为 Emoji 字符串（如 "🎂"），保留列名以兼容既有数据库
    val completionCount: Int = 0,
    val isDeleted: Int = 0,
    val deletedAt: Long? = null
) {
    /**
     * 方向（倒数 / 正数）。仅决定天数如何呈现。
     *
     * 计算属性不产生字节码字段，因此 Room 不会把它当作列，无需迁移。
     */
    val kind: MomentKind get() = MomentKind.fromRaw(type)

    /** 周期（一次性 / 每周 / 每月 / 每年）。仅决定何时重复出现。 */
    val repeat: MomentRepeat get() = MomentRepeat.fromRaw(repeatType)

    /** 该时刻是否已因日期非法而无法参与任何计算。 */
    val hasInvalidDate: Boolean
        get() = com.lifeordered.core.date.MomentSchedule.startDateMillisOf(this) == null
}
