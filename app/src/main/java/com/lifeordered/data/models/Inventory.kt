package com.lifeordered.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventories")
data class Inventory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val currentQty: Int, // Current stock amount
    val safeQty: Int, // Minimum threshold for warning state
    val unit: String = "袋", // E.g., 袋 (bags), 卷 (rolls), 盒 (boxes)
    val category: String = "居家",
    val iconPath: String = "",
    val isArchived: Boolean = false,
    val isDeleted: Int = 0,
    val deletedAt: Long? = null,
    val shelfLifeDays: Int? = null, // Shelf life in days
    val purchaseDate: Long? = null, // Purchase date timestamp
    val expiryDate: Long? = null, // Computed expiry date
    val autoConsumePeriod: String = "none", // "none", "day", "week", "month", "year"
    val autoConsumeQuantity: Int = 0,
    val lastAutoConsumeDate: Long? = null
) {
    /**
     * Determines whether stock warning alert is active
     */
    fun isLow(): Boolean = currentQty <= safeQty
    
    /**
     * Core consumption logic: applies the periodic auto-consumption rule
     */
    fun performAutoConsumption(currentTimeMillis: Long): Inventory? {
        if (autoConsumePeriod == "none" || autoConsumeQuantity <= 0 || lastAutoConsumeDate == null) return null
        
        val periodMillis = when (autoConsumePeriod) {
            "day" -> 24 * 60 * 60 * 1000L
            "week" -> 7 * 24 * 60 * 60 * 1000L
            "month" -> 30 * 24 * 60 * 60 * 1000L
            "year" -> 365 * 24 * 60 * 60 * 1000L
            else -> return null
        }
        
        var newQty = currentQty
        var newLastDate = lastAutoConsumeDate
        var cycles = 0
        
        while (currentTimeMillis - newLastDate >= periodMillis) {
            newQty -= autoConsumeQuantity
            newLastDate += periodMillis
            cycles++
        }
        
        if (cycles > 0) {
            return this.copy(
                currentQty = newQty.coerceAtLeast(0),
                lastAutoConsumeDate = newLastDate
            )
        }
        return null
    }
}
