package com.lifeordered.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "moments")
data class Moment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val isLunar: Boolean = false, // Whether it uses Chinese Lunar Calendar
    val isCompleted: Boolean = false, // Handled for once-off events
    val isYearly: Boolean = true, // Whether it repeats every year (e.g., Birthday, Anniversary)
    val cardColorIndex: Int = 0, // 0 for Peach, 1 for Blue, 2 for Pink, etc.
    val type: String = "countdown", // "countdown" or "countUp"
    val notes: String? = null,
    val repeatType: String = "none", // "none", "weekly", "monthly", "yearly"
    val themeColorHex: String = "#FF7E40",
    val autoArchive: Boolean = false,
    val isArchived: Boolean = false,
    val iconPath: String = "",
    val completionCount: Int = 0,
    val isDeleted: Int = 0,
    val deletedAt: Long? = null
)
