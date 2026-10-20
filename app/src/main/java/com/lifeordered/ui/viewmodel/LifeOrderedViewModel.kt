package com.lifeordered.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeordered.core.date.MomentDays
import com.lifeordered.core.date.MomentKind
import com.lifeordered.core.date.MomentSchedule
import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.core.utils.DataExporter
import com.lifeordered.core.utils.ReminderRescheduler
import com.lifeordered.data.database.LifeOrderedDatabase
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory
import com.lifeordered.data.models.Moment
import com.lifeordered.data.models.TagModel
import com.lifeordered.data.repository.LifeOrderedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

/**
 * 应用唯一的 ViewModel。
 *
 * ## 重构要点
 *
 * 1. **移除导航状态**：`activeTabState` / `detailScreenState` / `navigateToDetail`
 *    已删除，导航改由 `HomeScreen` 内的 `NavigationState` 持有。
 *    导航是 UI 关注点，放在 ViewModel 里既无法观测、也无法支持返回栈。
 * 2. **移除跨页筛选状态**：`itemCategoryFilter` / `calendarFilterType` 已下沉到
 *    各自的屏幕。原先首页为了「跳转并预设筛选」而直接写 ViewModel 字段，
 *    形成了隐式跨页耦合。
 * 3. **提醒统一走 [ReminderRescheduler]**：不再在 ViewModel 里手写闹钟时间。
 * 4. **打卡语义修正**：统一使用 [MomentSchedule.advance]，正数日不再推进日期。
 */
class LifeOrderedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LifeOrderedRepository
    private val context = application.applicationContext

    // ---------------------------------------------------------------------
    // 数据流
    // ---------------------------------------------------------------------

    val allMoments: StateFlow<List<Moment>>
    val activeMoments: StateFlow<List<Moment>>
    val archivedMoments: StateFlow<List<Moment>>
    val deletedMoments: StateFlow<List<Moment>>

    val allConsumables: StateFlow<List<Consumable>>
    val deletedConsumables: StateFlow<List<Consumable>>

    val allInventories: StateFlow<List<Inventory>>
    val deletedInventories: StateFlow<List<Inventory>>

    val allTags: StateFlow<List<TagModel>>

    // ---------------------------------------------------------------------
    // 偏好设置（Compose 可观察）
    // ---------------------------------------------------------------------

    /** 系统提醒推送开关。真实持久化，并真正影响调度。 */
    val notificationsEnabled = mutableStateOf(ReminderRescheduler.isEnabled(application))

    private val prefs = application.getSharedPreferences(
        ReminderRescheduler.PREFS_NAME,
        android.content.Context.MODE_PRIVATE
    )

    private val _homeSectionOrder = mutableStateOf(
        prefs.getString(
            ReminderRescheduler.KEY_HOME_SECTION_ORDER,
            ReminderRescheduler.DEFAULT_HOME_SECTION_ORDER
        )?.split(",")?.filter { it.isNotBlank() }
            ?: ReminderRescheduler.DEFAULT_HOME_SECTION_ORDER.split(",")
    )

    /** 首页三大板块的展示顺序，读取路径上有 Compose 状态，因此可触发重组。 */
    val homeSectionOrder: List<String> get() = _homeSectionOrder.value

    fun updateHomeSectionOrder(newOrder: List<String>) {
        if (newOrder.isEmpty()) return
        _homeSectionOrder.value = newOrder
        prefs.edit()
            .putString(ReminderRescheduler.KEY_HOME_SECTION_ORDER, newOrder.joinToString(","))
            .apply()
    }

    // ---------------------------------------------------------------------
    // 初始化
    // ---------------------------------------------------------------------

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

        // 注意：这里保持纯映射。重构前在 map 里直接写库（副作用），
        // 每次重新订阅都会触发一次写入，且违反了 Flow 的纯函数约定。
        allInventories = repository.allInventories.stateIn(
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

        viewModelScope.launch(Dispatchers.IO) {
            seedDatabaseIfEmpty()
            checkAndArchiveExpiredMoments()
            applyInventoryAutoConsumption()
            // 冷启动时对齐一次提醒，覆盖「上次运行后被系统清掉闹钟」的情况。
            ReminderRescheduler.rescheduleAll(context)
        }
    }

    // ---------------------------------------------------------------------
    // 问候与统计
    // ---------------------------------------------------------------------

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

    fun getInstallDays(): Long {
        return try {
            val installTime = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .firstInstallTime
            CalendarHelper.localDaysBetween(installTime, System.currentTimeMillis())
                .toLong()
                .coerceAtLeast(1)
        } catch (_: Exception) {
            1
        }
    }

    // ---------------------------------------------------------------------
    // 提醒开关
    // ---------------------------------------------------------------------

    fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled.value = enabled
        ReminderRescheduler.setEnabled(context, enabled)
        viewModelScope.launch(Dispatchers.IO) {
            if (enabled) {
                ReminderRescheduler.rescheduleAll(context)
            } else {
                ReminderRescheduler.cancelAll(context)
            }
        }
    }

    // ---------------------------------------------------------------------
    // 数据导出
    // ---------------------------------------------------------------------

    fun exportData(onResult: (Result<File>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { DataExporter.exportToJson(context) })
        }
    }

    // ---------------------------------------------------------------------
    // 种子数据
    // ---------------------------------------------------------------------

    private suspend fun seedDatabaseIfEmpty() {
        val currentMoments = repository.allMoments.first()
        if (currentMoments.isEmpty()) {
            val wifeBirthday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 3) }
            repository.insertMoment(
                Moment(
                    title = "老婆生日",
                    year = wifeBirthday.get(Calendar.YEAR),
                    month = wifeBirthday.get(Calendar.MONTH) + 1,
                    day = wifeBirthday.get(Calendar.DAY_OF_MONTH),
                    isLunar = false,
                    type = MomentKind.COUNTDOWN.raw,
                    notes = "准备一盒心形马卡龙和小夜灯 🎁",
                    repeatType = "yearly",
                    themeColorHex = "#FF7E40",
                    autoArchive = true,
                    cardColorIndex = 0
                )
            )

            val dadBirthday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 12) }
            repository.insertMoment(
                Moment(
                    title = "爸爸生日 🎂",
                    year = dadBirthday.get(Calendar.YEAR),
                    month = dadBirthday.get(Calendar.MONTH) + 1,
                    day = dadBirthday.get(Calendar.DAY_OF_MONTH),
                    isLunar = true,
                    type = MomentKind.COUNTDOWN.raw,
                    notes = "买泡好茶，记得打个温馨电话送祝福 🍵",
                    repeatType = "yearly",
                    themeColorHex = "#FFC107",
                    cardColorIndex = 1
                )
            )

            val anniversary = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -100) }
            repository.insertMoment(
                Moment(
                    title = "恋爱相守守护日",
                    year = anniversary.get(Calendar.YEAR),
                    month = anniversary.get(Calendar.MONTH) + 1,
                    day = anniversary.get(Calendar.DAY_OF_MONTH),
                    isLunar = false,
                    type = MomentKind.COUNT_UP.raw,
                    notes = "谢谢你来到我的世界，携手走过的每一天都闪闪发光 ✨",
                    repeatType = "none",
                    themeColorHex = "#E91E63",
                    cardColorIndex = 2
                )
            )
        }

        val currentConsumables = repository.allConsumables.first()
        if (currentConsumables.isEmpty()) {
            val now = System.currentTimeMillis()
            repository.insertConsumable(
                Consumable(
                    name = "净水器滤芯",
                    totalDays = 30,
                    startDateMillis = CalendarHelper.addLocalDays(now, -22),
                    cardColorIndex = 0
                )
            )
            repository.insertConsumable(
                Consumable(
                    name = "猫砂",
                    totalDays = 15,
                    startDateMillis = CalendarHelper.addLocalDays(now, -10),
                    cardColorIndex = 1
                )
            )
            repository.insertConsumable(
                Consumable(
                    name = "牙刷头",
                    totalDays = 90,
                    startDateMillis = CalendarHelper.addLocalDays(now, -87),
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

    // ---------------------------------------------------------------------
    // 时刻（Moment）
    // ---------------------------------------------------------------------

    fun addMoment(
        title: String,
        year: Int,
        month: Int,
        day: Int,
        isLunar: Boolean = false,
        isLeapMonth: Boolean = false,
        type: String = MomentKind.COUNTDOWN.raw,
        notes: String? = null,
        repeatType: String = "none",
        themeColorHex: String = "#FF7E40",
        autoArchive: Boolean = false,
        iconPath: String = "",
        cardColorIndex: Int = 0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val moment = Moment(
                title = title,
                year = year,
                month = month,
                day = day,
                isLunar = isLunar,
                isLeapMonth = isLeapMonth && isLunar,
                isCompleted = false,
                isYearly = repeatType == "yearly",
                // 重构前这里是 (0..2).random()，导致第 4 种图标永远不可达，
                // 且用户无法控制外观。现在由表单显式选择。
                cardColorIndex = cardColorIndex.coerceIn(0, 3),
                type = type,
                notes = notes,
                repeatType = repeatType,
                themeColorHex = themeColorHex,
                autoArchive = autoArchive,
                isArchived = false,
                iconPath = iconPath
            )
            val insertedId = repository.insertMoment(moment)
            ReminderRescheduler.rescheduleMoment(context, moment.copy(id = insertedId.toInt()))
        }
    }

    fun updateMoment(moment: Moment) {
        viewModelScope.launch(Dispatchers.IO) {
            val normalized = moment.copy(isLeapMonth = moment.isLeapMonth && moment.isLunar)
            repository.updateMoment(normalized)
            ReminderRescheduler.rescheduleMoment(context, normalized)
        }
    }

    /**
     * 打卡。
     *
     * 语义由 [MomentSchedule.advance] 统一决定：
     * - 正数日：只累加次数，**不推进日期**（修复：原先会把「已过 100 天」重置为 0）
     * - 循环倒数日：推进到下一周期并重排提醒
     * - 一次性倒数日：只累加次数
     */
    fun completeMoment(moment: Moment) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = MomentSchedule.advance(moment)
            repository.updateMoment(updated)

            ReminderRescheduler.cancelMoment(context, moment.id)
            if (updated.kind == MomentKind.COUNTDOWN && updated.repeat.isRecurring) {
                ReminderRescheduler.rescheduleMoment(context, updated)
            }
        }
    }

    fun moveToTrash(moment: Moment) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMoment(moment.copy(isDeleted = 1, deletedAt = System.currentTimeMillis()))
            ReminderRescheduler.cancelMoment(context, moment.id)
        }
    }

    fun restoreFromTrash(moment: Moment) {
        viewModelScope.launch(Dispatchers.IO) {
            val restored = moment.copy(isDeleted = 0, deletedAt = null)
            repository.updateMoment(restored)
            // 修复：重构前这里传入的是仍然带着 isDeleted = 1 的旧对象，
            // 导致恢复后提醒不会被重排。
            ReminderRescheduler.rescheduleMoment(context, restored)
        }
    }

    fun permanentDeleteMoment(moment: Moment) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderRescheduler.cancelMoment(context, moment.id)
            repository.deleteMoment(moment)
        }
    }

    fun updateMomentArchiveStatus(moment: Moment, isArchived: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = moment.copy(isArchived = isArchived, isDeleted = 0)
            repository.updateMoment(updated)
            if (isArchived) {
                ReminderRescheduler.cancelMoment(context, moment.id)
            } else {
                ReminderRescheduler.rescheduleMoment(context, updated)
            }
        }
    }

    /**
     * 冷启动扫描：把已过期且开启了自动归档的倒数日移入归档。
     *
     * 用 [MomentSchedule.resolve] 判断，而不是原来的 `calculateDays < 0`，
     * 这样「日期非法」与「已逾期」不会再被混为一谈。
     */
    fun checkAndArchiveExpiredMoments() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.allMoments.first().forEach { moment ->
                if (moment.kind != MomentKind.COUNTDOWN) return@forEach
                if (!moment.autoArchive || moment.isArchived) return@forEach

                val resolved = MomentSchedule.resolve(moment)
                if (resolved is MomentDays.Countdown && resolved.isOverdue) {
                    repository.updateMoment(moment.copy(isArchived = true))
                    ReminderRescheduler.cancelMoment(context, moment.id)
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // 耗材（Consumable）
    // ---------------------------------------------------------------------

    fun addConsumable(name: String, totalDays: Int, iconPath: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val startAt = System.currentTimeMillis()
            val consumable = Consumable(
                name = name,
                totalDays = totalDays,
                startDateMillis = startAt,
                cardColorIndex = (0..2).random(),
                iconPath = iconPath
            )
            val insertedId = repository.insertConsumable(consumable)
            ReminderRescheduler.rescheduleConsumable(
                context,
                consumable.copy(id = insertedId.toInt())
            )
        }
    }

    fun updateConsumable(consumable: Consumable) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateConsumable(consumable)
            ReminderRescheduler.rescheduleConsumable(context, consumable)
        }
    }

    fun resetConsumable(consumable: Consumable) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = consumable.copy(startDateMillis = System.currentTimeMillis())
            repository.updateConsumable(updated)
            ReminderRescheduler.rescheduleConsumable(context, updated)
        }
    }

    fun moveToTrash(consumable: Consumable) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateConsumable(
                consumable.copy(isDeleted = 1, deletedAt = System.currentTimeMillis())
            )
            ReminderRescheduler.cancelConsumable(context, consumable.id)
        }
    }

    fun restoreFromTrash(consumable: Consumable) {
        viewModelScope.launch(Dispatchers.IO) {
            val restored = consumable.copy(isDeleted = 0, deletedAt = null)
            repository.updateConsumable(restored)
            ReminderRescheduler.rescheduleConsumable(context, restored)
        }
    }

    fun permanentDeleteConsumable(consumable: Consumable) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderRescheduler.cancelConsumable(context, consumable.id)
            repository.deleteConsumable(consumable)
        }
    }

    // ---------------------------------------------------------------------
    // 库存（Inventory）
    // ---------------------------------------------------------------------

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
            val expiry = shelfLifeDays?.let { shelfLife ->
                CalendarHelper.addLocalDays(purchaseDate ?: System.currentTimeMillis(), shelfLife)
            }

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

            if (currentQty <= safeQty) {
                ReminderRescheduler.notifyLowStock(
                    context,
                    Inventory(
                        id = insertedId.toInt(),
                        name = name,
                        currentQty = currentQty,
                        safeQty = safeQty,
                        unit = unit,
                        category = category
                    )
                )
            }
        }
    }

    fun updateInventory(inventory: Inventory) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateInventory(inventory)
        }
    }

    fun moveToTrash(inventory: Inventory) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateInventory(
                inventory.copy(isDeleted = 1, deletedAt = System.currentTimeMillis())
            )
            ReminderRescheduler.cancelInventory(context, inventory.id)
        }
    }

    fun restoreFromTrash(inventory: Inventory) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateInventory(inventory.copy(isDeleted = 0, deletedAt = null))
        }
    }

    fun permanentDeleteInventory(inventory: Inventory) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderRescheduler.cancelInventory(context, inventory.id)
            repository.deleteInventory(inventory)
        }
    }

    /** 数量增减，并同步低库存提醒状态。 */
    fun updateInventoryQuantity(inventory: Inventory, change: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedQty = (inventory.currentQty + change).coerceAtLeast(0)
            val updated = inventory.copy(currentQty = updatedQty)
            repository.updateInventory(updated)

            if (updatedQty > inventory.safeQty) {
                ReminderRescheduler.cancelInventory(context, inventory.id)
            } else if (change < 0) {
                ReminderRescheduler.notifyLowStock(context, updated)
            }
        }
    }

    /**
     * 结算所有库存物品的自动消耗周期。
     *
     * 重构前这段逻辑写在 `repository.allInventories.map { ... }` 里，
     * 属于在 Flow 变换中做数据库写入：每次重新订阅（切页、重建）都会
     * 重新执行一遍副作用。现在改为初始化时显式结算一次。
     */
    private suspend fun applyInventoryAutoConsumption() {
        val now = System.currentTimeMillis()
        repository.allInventories.first().forEach { inventory ->
            inventory.performAutoConsumption(now)?.let { repository.updateInventory(it) }
        }
    }

    // ---------------------------------------------------------------------
    // 品类标签
    // ---------------------------------------------------------------------

    fun addTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.insertTag(TagModel(name = trimmed))
        }
    }

    fun deleteTag(tag: TagModel) {
        viewModelScope.launch {
            repository.deleteTag(tag)
        }
    }

    // ---------------------------------------------------------------------
    // 日历聚合
    // ---------------------------------------------------------------------

    /**
     * 把当月每一天的事件聚合成 `yyyy-MM-dd -> List<Any>`。
     *
     * 重构前是「遍历每一天 × 遍历每个时刻」的双重循环（O(天数 × 条目数)），
     * 且每层都重新解析农历。现在改为**先算每个条目命中的日期，再塞进 map**，
     * 复杂度降到 O(条目数 × 命中次数)，农历换算次数也大幅减少。
     */
    fun getEventsMapForMonth(year: Int, month: Int, filter: String? = null): Map<String, List<Any>> {
        val result = mutableMapOf<String, MutableList<Any>>()

        fun put(dateKey: String, item: Any) {
            result.getOrPut(dateKey) { mutableListOf() }.add(item)
        }

        if (filter == null || filter == "moment") {
            allMoments.value.forEach { moment ->
                if (moment.isArchived) return@forEach
                // 一次性事件只可能命中一天；循环事件才需要逐日检查。
                if (!moment.repeat.isRecurring) {
                    val start = MomentSchedule.startDateMillisOf(moment) ?: return@forEach
                    val cal = Calendar.getInstance().apply { timeInMillis = start }
                    if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) + 1 == month) {
                        put(dateKeyOf(year, month, cal.get(Calendar.DAY_OF_MONTH)), moment)
                    }
                    return@forEach
                }
                for (day in 1..daysInMonth(year, month)) {
                    if (MomentSchedule.occursOn(moment, year, month, day)) {
                        put(dateKeyOf(year, month, day), moment)
                    }
                }
            }
        }

        if (filter == null || filter == "consumable") {
            allConsumables.value.forEach { consumable ->
                val expiry = CalendarHelper.addLocalDays(
                    consumable.startDateMillis,
                    consumable.totalDays
                )
                val cal = Calendar.getInstance().apply { timeInMillis = expiry }
                if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) + 1 == month) {
                    put(dateKeyOf(year, month, cal.get(Calendar.DAY_OF_MONTH)), consumable)
                }
            }
        }

        return result
    }

    private fun daysInMonth(year: Int, month: Int): Int =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

    private fun dateKeyOf(year: Int, month: Int, day: Int): String =
        String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month, day)

    // ---------------------------------------------------------------------
    // 重置
    // ---------------------------------------------------------------------

    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            val database = LifeOrderedDatabase.getDatabase(getApplication())
            ReminderRescheduler.cancelAll(context)
            database.clearAllTables()
            seedDatabaseIfEmpty()
            ReminderRescheduler.rescheduleAll(context)
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
