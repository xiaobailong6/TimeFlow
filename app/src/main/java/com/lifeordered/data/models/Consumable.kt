package com.lifeordered.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

@Entity(tableName = "consumables")
data class Consumable(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val totalDays: Int, // Total usage span in days
    val startDateMillis: Long, // When the usage started (updates on Reset)
    val cardColorIndex: Int = 0, // Establishes visual skin
    val iconPath: String = "",
    val isDeleted: Int = 0,
    val deletedAt: Long? = null
) {
    /**
     * Compute remaining useful days: totalDays - usedDays
     */
    fun getRemainingDays(nowMillis: Long = System.currentTimeMillis()): Int {
        val diffMillis = nowMillis - startDateMillis
        if (diffMillis <= 0) return totalDays
        val usedDays = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()
        return (totalDays - usedDays).coerceAtLeast(0)
    }
}
