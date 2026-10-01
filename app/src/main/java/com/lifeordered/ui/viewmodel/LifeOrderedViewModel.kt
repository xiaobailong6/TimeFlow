package com.lifeordered.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeordered.data.database.LifeOrderedDatabase
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory
import com.lifeordered.data.models.Moment
import com.lifeordered.data.models.TagModel
import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.core.utils.NotificationHelper
import com.lifeordered.data.repository.LifeOrderedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class LifeOrderedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LifeOrderedRepository
    private val notificationHelper = NotificationHelper.getInstance(application)
    private val context = application.applicationContext

    private fun getMomentNotificationId(id: Int): Int = id + 100000
    private fun getConsumableNotificationId(id: Int): Int = id + 200000
    private fun getInventoryNotificationId(id: Int): Int = id + 300000

    val allMoments: StateFlow<List<Moment>>
    val activeMoments: StateFlow<List<Moment>>
    val archivedMoments: StateFlow<List<Moment>>
    val deletedMoments: StateFlow<List<Moment>>

    val allConsumables: StateFlow<List<Consumable>>
    val deletedConsumables: StateFlow<List<Consumable>>

    val allInventories: StateFlow<List<Inventory>>
    val deletedInventories: StateFlow<List<Inventory>>
    
    val allTags: StateFlow<List<TagModel>>

    // Global Tab and Filtering States for Navigation Interlinking
    val activeTabState = androidx.compose.runtime.mutableStateOf(0)
    val detailScreenState = androidx.compose.runtime.mutableStateOf<String?>(null) // null, "all_moments", "all_upcoming"
    
    // Home Section Order
    private val prefs = application.getSharedPreferences("life_ordered_prefs", Context.MODE_PRIVATE)
    private val _homeSectionOrder = androidx.compose.runtime.mutableStateOf(
        prefs.getString("home_section_order", "moments,alerts,inventory")?.split(",") ?: listOf("moments", "alerts", "inventory")
    )
    val homeSectionOrder: List<String> get() = _homeSectionOrder.value

    fun updateHomeSectionOrder(newOrder: List<String>) {
        _homeSectionOrder.value = newOrder
        prefs.edit().putString("home_section_order", newOrder.joinToString(",")).apply()
    }

    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> "🌅 早安，今天也是有序的一天！"
            in 11..12 -> "🍱 中午好，记得好好吃午饭哦~"
            in 13..17 -> "☕ 下午好，伸个懒腰，喝杯水吧！"
            in 18..22 -> "🌙 晚上好，享受轻松的晚间时光。"
            else -> "✨ 夜深了，早点休息，晚安好梦..."
        }
    }

    fun navigateToDetail(screen: String?) {
        detailScreenState.value = screen
    }
    
    fun getInstallDays(): Long {
        return try {
            val installTime = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
            val currentTime = System.currentTimeMillis()
            val diff = currentTime - installTime
            (diff / (24 * 60 * 60 * 1000L)).coerceAtLeast(1)
        } catch (e: Exception) {
            1
        }
    }

    fun onTabSelected(index: Int) {
        activeTabState.value = index
        // Requirement 4: Reset item filter to '全部' when switching to Items tab (index 2)
        if (index == 2) {
            itemCategoryFilter.value = "全部"
        }
    }

    val calendarFilterType = androidx.compose.runtime.mutableStateOf<String?>(null) // null, "moment", "consumable"
    val itemCategoryFilter = androidx.compose.runtime.mutableStateOf<String?>("全部") // "全部", "低库存"

    init {
        val database = LifeOrderedDatabase.getDatabase(application)
        repository = LifeOrderedRepository(
            database.momentDao(),
            database.consumableDao(),
            database.inventoryDao(),
            database.tagDao()
        )

        allMoments = repository.allMoments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activeMoments = repository.allMoments.map { list ->
            list.filter { !it.isArchived }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        archivedMoments = repository.allMoments.map { list ->
            list.filter { it.isArchived }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        deletedMoments = repository.deletedMoments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allConsumables = repository.allConsumables.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        deletedConsumables = repository.deletedConsumables.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allInventories = repository.allInventories.map { list ->
            val now = System.currentTimeMillis()
            for (inv in list) {
                val updated = inv.performAutoConsumption(now)
                if (updated != null) {
                    repository.updateInventory(updated)
                }
            }
            list
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        deletedInventories = repository.deletedInventories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allTags = repository.allTags.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed with high-fidelity Mock data on first launch to match design draft
        viewModelScope.launch(Dispatchers.IO) {
            seedDatabaseIfEmpty()
            checkAndArchiveExpiredMoments()
        }
    }

    private suspend fun seedDatabaseIfEmpty() {
        val currentMoments = repository.allMoments.first()
        if (currentMoments.isEmpty()) {
            val today = Calendar.getInstance()
            val year = today.get(Calendar.YEAR)

            // Wife birthday (May 27) - compute dynamic month/day so that countdown matches 3 days or displays beautifully
            val wifeBirthday = Calendar.getInstance()
            wifeBirthday.add(Calendar.DAY_OF_MONTH, 3)
            repository.insertMoment(
                Moment(
                    title = "老婆生日",
                    year = wifeBirthday.get(Calendar.YEAR),
                    month = wifeBirthday.get(Calendar.MONTH) + 1,
                    day = wifeBirthday.get(Calendar.DAY_OF_MONTH),
                    isLunar = false,
                    isYearly = true,
                    cardColorIndex = 0,
                    type = "countdown",
                    notes = "准备一盒心形马卡龙和小夜灯 🎁",
                    repeatType = "yearly",
                    themeColorHex = "#FF7E40", // 蜜桃橙
                    autoArchive = true,
                    isArchived = false
                )
            )

            // Dad birthday (June 5) - or dynamic 12 days remaining
            val dadBirthday = Calendar.getInstance()
            dadBirthday.add(Calendar.DAY_OF_MONTH, 12)
            repository.insertMoment(
                Moment(
                    title = "爸爸生日 🎂",
                    year = dadBirthday.get(Calendar.YEAR),
                    month = dadBirthday.get(Calendar.MONTH) + 1,
                    day = dadBirthday.get(Calendar.DAY_OF_MONTH),
                    isLunar = true,
                    isYearly = true,
                    cardColorIndex = 1,
                    type = "countdown",
                    notes = "买泡好茶，记得打个温馨电话送祝福 🍵",
                    repeatType = "yearly",
                    themeColorHex = "#FFC107", // 奶油黄
                    autoArchive = false,
                    isArchived = false
                )
            )

            // Love anniversary (March 14) - countUp (positive days elapsed)
            val anniversary = Calendar.getInstance()
            anniversary.add(Calendar.DAY_OF_MONTH, -100) // 100 days elapsed
            repository.insertMoment(
                Moment(
                    title = "恋爱相守守护日",
                    year = anniversary.get(Calendar.YEAR),
                    month = anniversary.get(Calendar.MONTH) + 1,
                    day = anniversary.get(Calendar.DAY_OF_MONTH),
                    isLunar = false,
                    isYearly = false,
                    cardColorIndex = 2,
                    type = "countUp",
                    notes = "谢谢你来到我的世界，携手走过的每一天都闪闪发发 ✨",
                    repeatType = "none",
                    themeColorHex = "#E91E63", // 草莓红
                    autoArchive = false,
                    isArchived = false
                )
            )
        }

        val currentConsumables = repository.allConsumables.first()
        if (currentConsumables.isEmpty()) {
            val dayInMillis = 24 * 60 * 60 * 1000L

            // Water filter: Total 30 days, 8 remaining days -> used 22 days
            val filterStart = System.currentTimeMillis() - (22 * dayInMillis)
            repository.insertConsumable(
                Consumable(
                    name = "净水器滤芯",
                    totalDays = 30,
                    startDateMillis = filterStart,
                    cardColorIndex = 0
                )
            )

            // Cat litter: Total 15 days, 5 remaining days -> used 10 days
            val litterStart = System.currentTimeMillis() - (10 * dayInMillis)
            repository.insertConsumable(
                Consumable(
                    name = "猫砂",
                    totalDays = 15,
                    startDateMillis = litterStart,
                    cardColorIndex = 1
                )
            )

            // Toothbrush: Total 90 days, 3 remaining days -> used 87 days
            val brushStart = System.currentTimeMillis() - (87 * dayInMillis)
            repository.insertConsumable(
                Consumable(
                    name = "牙刷头",
                    totalDays = 90,
                    startDateMillis = brushStart,
                    cardColorIndex = 2
                )
            )
        }

        val currentInventories = repository.allInventories.first()
        if (currentInventories.isEmpty()) {
            repository.insertInventory(
                Inventory(name = "猫粮", currentQty = 1, safeQty = 2, unit = "袋", category = "食品")
            )
            repository.insertInventory(
                Inventory(name = "卫生纸", currentQty = 1, safeQty = 5, unit = "卷", category = "居家")
            )
            repository.insertInventory(
                Inventory(name = "猫砂", currentQty = 1, safeQty = 3, unit = "袋", category = "居家")
            )
        }

        val currentTags = repository.allTags.first()
        if (currentTags.isEmpty()) {
            repository.insertTag(TagModel(name = "食品"))
            repository.insertTag(TagModel(name = "洗护"))
            repository.insertTag(TagModel(name = "居家"))
            repository.insertTag(TagModel(name = "数码"))
        }
    }

    // Helper to schedule Moment Alerts
    private fun scheduleMomentNotification(moment: Moment) {
        val daysRemaining = CalendarHelper.calculateDays(moment)
        if (daysRemaining < 0) return
        val targetCal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis() + (daysRemaining.toLong() * 24 * 60 * 60 * 1000L)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val notificationId = getMomentNotificationId(moment.id)
        notificationHelper.cancelNotification(notificationId)
        notificationHelper.scheduleNotification(
            id = notificationId,
            title = "重要时刻提醒 ⏰",
            body = "今天是 [${moment.title}] 的日子哦，别忘了！",
            scheduledTimeMillis = targetCal.timeInMillis
        )
    }

    // Helper to schedule Consumable Alerts
    private fun scheduleConsumableNotification(consumableId: Int, consumableName: String, remainingDays: Int) {
        val targetCal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis() + (remainingDays.toLong() * 24 * 60 * 60 * 1000L)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val notificationId = getConsumableNotificationId(consumableId)
        notificationHelper.cancelNotification(notificationId)
        notificationHelper.scheduleNotification(
            id = notificationId,
            title = "备忘寿命到期 🕒",
            body = "你的 [${consumableName}] 已经使用完毕，请及时更换！",
            scheduledTimeMillis = targetCal.timeInMillis
        )
    }

    // 1. Actions for Moments
    fun addMoment(
        title: String,
        year: Int,
        month: Int,
        day: Int,
        isLunar: Boolean = false,
        isYearly: Boolean = true,
        type: String = "countdown",
        notes: String? = null,
        repeatType: String = "none",
        themeColorHex: String = "#FF7E40",
        autoArchive: Boolean = false,
        iconPath: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val m = Moment(
                title = title,
                year = year,
                month = month,
                day = day,
                isLunar = isLunar,
                isCompleted = false,
                isYearly = isYearly,
                cardColorIndex = (0..2).random(),
                type = type,
                notes = notes,
                repeatType = repeatType,
                themeColorHex = themeColorHex,
                autoArchive = autoArchive,
                isArchived = false,
                iconPath = iconPath
            )
            val insertedId = repository.insertMoment(m)
            val insertedMoment = m.copy(id = insertedId.toInt())
            scheduleMomentNotification(insertedMoment)
        }
    }

    fun completeMoment(moment: Moment) {
        viewModelScope.launch(Dispatchers.IO) {
            val isRecurring = moment.repeatType != "none"
            val updatedMoment = if (isRecurring) {
                // Determine next target date based on repeat cycle
                val nextCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, moment.year)
                    set(Calendar.MONTH, moment.month - 1)
                    set(Calendar.DAY_OF_MONTH, moment.day)
                    
                    when (moment.repeatType) {
                        "weekly" -> add(Calendar.WEEK_OF_YEAR, 1)
                        "monthly" -> add(Calendar.MONTH, 1)
                        "yearly" -> add(Calendar.YEAR, 1)
                    }
                }
                moment.copy(
                    year = nextCal.get(Calendar.YEAR),
                    month = nextCal.get(Calendar.MONTH) + 1,
                    day = nextCal.get(Calendar.DAY_OF_MONTH),
                    completionCount = moment.completionCount + 1
                )
            } else {
                // Fix Bug: Click complete ONLY increments count without archiving or hiding.
                moment.copy(completionCount = moment.completionCount + 1)
            }
            repository.updateMoment(updatedMoment)
            
            // Notification handling
            val nId = getMomentNotificationId(moment.id)
            notificationHelper.cancelNotification(nId)
            if (isRecurring) {
                scheduleMomentNotification(updatedMoment)
            }
        }
    }

    fun moveToTrash(moment: Moment) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMoment(moment.copy(isDeleted = 1, deletedAt = System.currentTimeMillis()))
            notificationHelper.cancelNotification(getMomentNotificationId(moment.id))
        }
    }

    fun restoreFromTrash(moment: Moment) {
        viewModelScope.launch(Dispatchers.IO) {
            // Restore from archive too if it was there
            repository.updateMoment(moment.copy(isDeleted = 0, deletedAt = null))
            scheduleMomentNotification(moment)
        }
    }

    fun permanentDeleteMoment(moment: Moment) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMoment(moment)
        }
    }

    fun updateMomentArchiveStatus(moment: Moment, isArchived: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMoment(moment.copy(isArchived = isArchived, isDeleted = 0))
        }
    }

    fun checkAndArchiveExpiredMoments() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.allMoments.first()
            val now = System.currentTimeMillis()
            for (moment in list) {
                if (moment.type == "countdown" && moment.autoArchive && !moment.isArchived) {
                    val days = CalendarHelper.calculateDays(moment, now)
                    if (days < 0) {
                        repository.updateMoment(moment.copy(isArchived = true))
                    }
                }
            }
        }
    }

    // 2. Actions for Consumables
    fun addConsumable(name: String, totalDays: Int, iconPath: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val insertedId = repository.insertConsumable(
                Consumable(
                    name = name,
                    totalDays = totalDays,
                    startDateMillis = System.currentTimeMillis(),
                    cardColorIndex = (0..2).random(),
                    iconPath = iconPath
                )
            )
            scheduleConsumableNotification(insertedId.toInt(), name, totalDays)
        }
    }

    fun resetConsumable(consumable: Consumable) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = consumable.copy(startDateMillis = System.currentTimeMillis())
            repository.updateConsumable(updated)
            scheduleConsumableNotification(updated.id, updated.name, updated.totalDays)
        }
    }

    fun moveToTrash(consumable: Consumable) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateConsumable(consumable.copy(isDeleted = 1, deletedAt = System.currentTimeMillis()))
            notificationHelper.cancelNotification(getConsumableNotificationId(consumable.id))
        }
    }

    fun restoreFromTrash(consumable: Consumable) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateConsumable(consumable.copy(isDeleted = 0, deletedAt = null))
            scheduleConsumableNotification(consumable.id, consumable.name, consumable.totalDays)
        }
    }

    fun permanentDeleteConsumable(consumable: Consumable) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteConsumable(consumable)
        }
    }

    // 3. Actions for Inventories
    fun addInventory(
        name: String, 
        currentQty: Int, 
        safeQty: Int, 
        unit: String, 
        category: String = "居家", 
        iconPath: String = "",
        shelfLifeDays: Int? = null,
        purchaseDate: Long? = null,
        autoConsumePeriod: String = "none",
        autoConsumeQuantity: Int = 0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val expiry = if (shelfLifeDays != null) {
                val base = purchaseDate ?: System.currentTimeMillis()
                base + (shelfLifeDays.toLong() * 24 * 60 * 60 * 1000L)
            } else null

            val insertedId = repository.insertInventory(
                Inventory(
                    name = name,
                    currentQty = currentQty,
                    safeQty = safeQty,
                    unit = unit,
                    category = category,
                    iconPath = iconPath,
                    shelfLifeDays = shelfLifeDays,
                    purchaseDate = purchaseDate,
                    expiryDate = expiry,
                    autoConsumePeriod = autoConsumePeriod,
                    autoConsumeQuantity = autoConsumeQuantity,
                    lastAutoConsumeDate = System.currentTimeMillis()
                )
            )
            val isLow = currentQty <= safeQty
            if (isLow) {
                notificationHelper.showInstantNotification(
                    id = getInventoryNotificationId(insertedId.toInt()),
                    title = "库存不足提醒 🛒",
                    body = "您的 [${name}] 库存不足，请及时备货！"
                )
            }
        }
    }

    fun replenishInventory(inventory: Inventory) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedQty = inventory.currentQty + 1
            val updated = inventory.copy(currentQty = updatedQty)
            repository.updateInventory(updated)
            
            // Cancel notification if it gets back to safe levels
            if (updatedQty > inventory.safeQty) {
                notificationHelper.cancelNotification(getInventoryNotificationId(inventory.id))
            }
        }
    }

    fun consumeInventory(inventory: Inventory) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedQty = (inventory.currentQty - 1).coerceAtLeast(0)
            val updated = inventory.copy(currentQty = updatedQty)
            repository.updateInventory(updated)
            
            // Trigger instant alert if it crosses below safety limit
            if (updated.isLow()) {
                notificationHelper.showInstantNotification(
                    id = getInventoryNotificationId(inventory.id),
                    title = "库存不足提醒 🛒",
                    body = "您的 [${inventory.name}] 库存不足，请及时备货！"
                )
            }
        }
    }

    fun moveToTrash(inventory: Inventory) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateInventory(inventory.copy(isDeleted = 1, deletedAt = System.currentTimeMillis()))
            notificationHelper.cancelNotification(getInventoryNotificationId(inventory.id))
        }
    }

    fun restoreFromTrash(inventory: Inventory) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateInventory(inventory.copy(isDeleted = 0, deletedAt = null))
        }
    }

    fun permanentDeleteInventory(inventory: Inventory) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteInventory(inventory)
        }
    }

    /**
     * Direct incremental/decremental quantity adjustment for inventory items
     */
    fun updateInventoryQuantity(inventory: Inventory, change: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedQty = (inventory.currentQty + change).coerceAtLeast(0)
            val updated = inventory.copy(currentQty = updatedQty)
            repository.updateInventory(updated)
            
            // Sync low stock alert system
            if (updatedQty > inventory.safeQty) {
                notificationHelper.cancelNotification(getInventoryNotificationId(inventory.id))
            } else if (updated.isLow() && change < 0) {
                notificationHelper.showInstantNotification(
                    id = getInventoryNotificationId(inventory.id),
                    title = "库存不足提醒 🛒",
                    body = "您的 [${inventory.name}] 库存不足，请及时备货！"
                )
            }
        }
    }

    fun addTag(name: String) {
        viewModelScope.launch {
            repository.insertTag(TagModel(name = name))
        }
    }

    fun deleteTag(tag: TagModel) {
        viewModelScope.launch {
            repository.deleteTag(tag)
        }
    }

    /**
     * Convert list of moments and consumables into a Map keyed by date string (YYYY-MM-DD) for a given month/year
     */
    fun getEventsMapForMonth(year: Int, month: Int, filter: String? = null): Map<String, List<Any>> {
        val map = mutableMapOf<String, MutableList<Any>>()
        val momentsList = allMoments.value
        val consumablesList = allConsumables.value
        
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        for (day in 1..maxDay) {
            val dateKey = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month, day)
            
            // Add Moments
            if (filter == null || filter == "moment") {
                for (moment in momentsList) {
                    if (CalendarHelper.isMomentOnDate(moment, year, month, day)) {
                        if (!map.containsKey(dateKey)) {
                            map[dateKey] = mutableListOf()
                        }
                        map[dateKey]?.add(moment)
                    }
                }
            }
            
            // Add Consumables expiry
            if (filter == null || filter == "consumable") {
                for (consumable in consumablesList) {
                    val expiryTime = consumable.startDateMillis + (consumable.totalDays * 24 * 60 * 60 * 1000L)
                    val expCal = Calendar.getInstance().apply { timeInMillis = expiryTime }
                    if (expCal.get(Calendar.YEAR) == year && 
                        expCal.get(Calendar.MONTH) == month - 1 && 
                        expCal.get(Calendar.DAY_OF_MONTH) == day) {
                        if (!map.containsKey(dateKey)) {
                            map[dateKey] = mutableListOf()
                        }
                        map[dateKey]?.add(consumable)
                    }
                }
            }
        }
        return map
    }

    fun updateMoment(moment: Moment) {
        viewModelScope.launch {
            repository.updateMoment(moment)
            scheduleMomentNotification(moment)
        }
    }

    fun updateConsumable(consumable: Consumable) {
        viewModelScope.launch {
            repository.updateConsumable(consumable)
            val remaining = consumable.getRemainingDays()
            scheduleConsumableNotification(consumable.id, consumable.name, remaining)
        }
    }

    fun updateInventory(inventory: Inventory) {
        viewModelScope.launch {
            repository.updateInventory(inventory)
        }
    }

    /**
     * Clear all database records completely and reset (Danger zone action used in Profile reset)
     */
    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            // Retrieve Database and clear all tables
            val database = LifeOrderedDatabase.getDatabase(getApplication())
            database.clearAllTables()
            // Re-seed database with default items
            seedDatabaseIfEmpty()
        }
    }
}

class LifeOrderedViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LifeOrderedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LifeOrderedViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
