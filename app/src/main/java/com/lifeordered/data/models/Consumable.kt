package com.lifeordered.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lifeordered.core.utils.CalendarHelper

@Entity(tableName = "consumables")
data class Consumable(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val totalDays: Int, // Total usage span in calendar days
    val startDateMillis: Long, // When the usage started (updates on Reset)
    val cardColorIndex: Int = 0, // Establishes visual skin
    val iconPath: String = "",
    val isDeleted: Int = 0,
    val deletedAt: Long? = null
) {
    /**
     * Compute remaining useful days using local calendar dates. A DST change
     * must not make one usage day appear as 23 or 25 hours.
     */
    fun getRemainingDays(nowMillis: Long = System.currentTimeMillis()): Int {
        if (nowMillis <= startDateMillis) return totalDays
        val usedDays = CalendarHelper.localDaysBetween(startDateMillis, nowMillis)
        return (totalDays - usedDays).coerceAtLeast(0)
    }
}
