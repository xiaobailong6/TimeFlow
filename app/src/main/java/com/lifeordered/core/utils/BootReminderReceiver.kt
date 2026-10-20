package com.lifeordered.core.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 在系统事件后重建全部本地提醒。
 *
 * `AlarmManager` 的闹钟**不跨设备重启存活**，重构前应用没有这个接收器，
 * 因此用户只要重启手机，所有纪念日与耗材提醒就会永久消失，直到他再次
 * 手动编辑某条记录。
 *
 * 同时处理 `ACTION_MY_PACKAGE_REPLACED`（应用升级后闹钟同样会被清空）。
 */
class BootReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val appContext = context.applicationContext
        // goAsync 让接收器在 onReceive 返回后仍能继续做 IO，避免被系统提前回收。
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ReminderRescheduler.rescheduleAll(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
