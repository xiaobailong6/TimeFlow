package com.lifeordered.core.utils

import android.content.Context
import com.lifeordered.core.date.MomentDays
import com.lifeordered.core.date.MomentKind
import com.lifeordered.core.date.MomentSchedule
import com.lifeordered.data.database.LifeOrderedDatabase
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory
import com.lifeordered.data.models.Moment
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * 提醒调度的**唯一入口**。
 *
 * ## 重构背景
 *
 * 重构前提醒逻辑有三个问题：
 * 1. 只在「用户操作」时重排。进程被杀或设备重启后，所有 `AlarmManager`
 *    闹钟永久丢失（`AlarmManager` 不跨重启存活）。
 * 2. 「我的」页的推送开关只是 `remember { mutableStateOf(true) }`，
 *    既不持久化也不影响任何调度，是个纯装饰。
 * 3. 正数日（`countUp`）也会在起始日 09:00 收到「今天是 [X] 的日子哦」，
 *    对一个「已过 100 天」的纪念日来说语义完全错位。
 *
 * 现在提醒的**数据源是数据库**，因此 [rescheduleAll] 可以在任意时刻
 * 无条件重建全部提醒——开机、应用升级、用户重新打开开关，都走同一条路径，
 * 不需要额外持久化「待发提醒」表。
 */
object ReminderRescheduler {

    /** 与 ViewModel 共用的 SharedPreferences。 */
    const val PREFS_NAME = "life_ordered_prefs"
    const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val KEY_HOME_SECTION_ORDER = "home_section_order"
    const val DEFAULT_HOME_SECTION_ORDER = "moments,alerts,inventory"

    /** 时刻提醒的触发时刻：当天 09:00。 */
    private const val MOMENT_ALERT_HOUR = 9

    /** 耗材提醒的触发时刻：当天 10:00。 */
    private const val CONSUMABLE_ALERT_HOUR = 10

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 用户是否开启了系统提醒推送。默认开启。 */
    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    // ---------------------------------------------------------------------
    // 单个实体调度
    // ---------------------------------------------------------------------

    /**
     * 重排某个时刻的提醒。
     *
     * 会先无条件取消旧闹钟，因此可安全地重复调用（幂等）。
     */
    fun rescheduleMoment(context: Context, moment: Moment) {
        val helper = NotificationHelper.getInstance(context)
        val notificationId = NotificationHelper.momentNotificationId(moment.id)
        helper.cancelNotification(notificationId)

        if (!isEnabled(context)) return
        if (moment.isArchived || moment.isDeleted == 1) return

        // 正数日是「已经过去多少天」的累计计数，没有「还有几天」的概念，
        // 因此不排提醒（重构前会误报）。
        if (moment.kind == MomentKind.COUNT_UP) return

        val days = when (val resolved = MomentSchedule.resolve(moment)) {
            is MomentDays.Countdown -> if (resolved.isOverdue) null else resolved.days
            else -> null
        } ?: return

        val target = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DAY_OF_YEAR, days)
            set(Calendar.HOUR_OF_DAY, MOMENT_ALERT_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        helper.scheduleNotification(
            id = notificationId,
            title = "重要时刻提醒 ⏰",
            body = "今天是 [${moment.title}] 的日子哦，别忘了！",
            scheduledTimeMillis = target.timeInMillis
        )
    }

    /** 重排某个耗材的到期提醒。 */
    fun rescheduleConsumable(context: Context, consumable: Consumable) {
        val helper = NotificationHelper.getInstance(context)
        val notificationId = NotificationHelper.consumableNotificationId(consumable.id)
        helper.cancelNotification(notificationId)

        if (!isEnabled(context)) return
        if (consumable.isDeleted == 1) return

        val target = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DAY_OF_YEAR, consumable.getRemainingDays())
            set(Calendar.HOUR_OF_DAY, CONSUMABLE_ALERT_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        helper.scheduleNotification(
            id = notificationId,
            title = "备忘寿命到期 🕒",
            body = "你的 [${consumable.name}] 已经使用完毕，请及时更换！",
            scheduledTimeMillis = target.timeInMillis
        )
    }

    /** 库存低于安全线时的即时提醒。 */
    fun notifyLowStock(context: Context, inventory: Inventory) {
        if (!isEnabled(context)) return
        NotificationHelper.getInstance(context).showInstantNotification(
            id = NotificationHelper.inventoryNotificationId(inventory.id),
            title = "库存不足提醒 🛒",
            body = "您的 [${inventory.name}] 库存不足，请及时备货！"
        )
    }

    fun cancelMoment(context: Context, momentId: Int) {
        NotificationHelper.getInstance(context)
            .cancelNotification(NotificationHelper.momentNotificationId(momentId))
    }

    fun cancelConsumable(context: Context, consumableId: Int) {
        NotificationHelper.getInstance(context)
            .cancelNotification(NotificationHelper.consumableNotificationId(consumableId))
    }

    fun cancelInventory(context: Context, inventoryId: Int) {
        NotificationHelper.getInstance(context)
            .cancelNotification(NotificationHelper.inventoryNotificationId(inventoryId))
    }

    // ---------------------------------------------------------------------
    // 全量重建 / 全量取消
    // ---------------------------------------------------------------------

    /**
     * 从数据库重建**全部**提醒。
     *
     * 调用时机：开机完成、应用升级、用户重新打开推送开关。
     * 因为数据源是数据库，这个操作天然幂等。
     */
    suspend fun rescheduleAll(context: Context) {
        if (!isEnabled(context)) return

        val database = LifeOrderedDatabase.getDatabase(context.applicationContext)
        database.momentDao().getAllMomentsFlow().first().forEach { moment ->
            rescheduleMoment(context, moment)
        }
        database.consumableDao().getAllConsumablesFlow().first().forEach { consumable ->
            rescheduleConsumable(context, consumable)
        }
    }

    /** 取消全部提醒（含库存即时提醒），用于关闭推送开关。 */
    suspend fun cancelAll(context: Context) {
        val helper = NotificationHelper.getInstance(context)
        val database = LifeOrderedDatabase.getDatabase(context.applicationContext)

        val ids = mutableListOf<Int>()
        database.momentDao().getAllMomentsFlow().first()
            .forEach { ids.add(NotificationHelper.momentNotificationId(it.id)) }
        database.consumableDao().getAllConsumablesFlow().first()
            .forEach { ids.add(NotificationHelper.consumableNotificationId(it.id)) }
        database.inventoryDao().getAllInventoriesFlow().first()
            .forEach { ids.add(NotificationHelper.inventoryNotificationId(it.id)) }

        helper.cancelAll(ids)
    }
}
