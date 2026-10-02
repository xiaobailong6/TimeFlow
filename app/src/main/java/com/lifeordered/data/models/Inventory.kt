package com.lifeordered.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

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
    val shelfLifeDays: Int? = null, // Shelf life in calendar days
    val purchaseDate: Long? = null, // Purchase date timestamp
    val expiryDate: Long? = null, // Computed expiry date
    val autoConsumePeriod: String = "none", // "none", "day", "week", "month", "year"
    val autoConsumeQuantity: Int = 0,
    val lastAutoConsumeDate: Long? = null
) {
    /** Determines whether stock warning alert is active. */
    fun isLow(): Boolean = currentQty <= safeQty

    /**
     * Applies all elapsed auto-consumption periods. Calendar.add is used for
     * month/year rules so their real lengths and DST transitions are honored.
     */
    fun performAutoConsumption(currentTimeMillis: Long): Inventory? {
        if (autoConsumePeriod == "none" || autoConsumeQuantity <= 0 || lastAutoConsumeDate == null) return null

        val cursor = Calendar.getInstance().apply { timeInMillis = lastAutoConsumeDate }
        val now = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        var newQty = currentQty
        var cycles = 0

        while (true) {
            val next = cursor.clone() as Calendar
            when (autoConsumePeriod) {
                "day" -> next.add(Calendar.DAY_OF_YEAR, 1)
                "week" -> next.add(Calendar.WEEK_OF_YEAR, 1)
                "month" -> next.add(Calendar.MONTH, 1)
                "year" -> next.add(Calendar.YEAR, 1)
                else -> return null
            }
            if (next.after(now)) break
            cursor.timeInMillis = next.timeInMillis
            newQty -= autoConsumeQuantity
            cycles++
            // Prevent a corrupt old timestamp from monopolizing the app thread.
            if (cycles >= 100_000) break
        }

        return if (cycles > 0) {
            copy(
                currentQty = newQty.coerceAtLeast(0),
                lastAutoConsumeDate = cursor.timeInMillis
            )
        } else {
            null
        }
    }
}
