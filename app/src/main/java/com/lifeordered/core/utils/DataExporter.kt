package com.lifeordered.core.utils

import android.content.Context
import com.lifeordered.data.database.LifeOrderedDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 本地数据导出。
 *
 * ## 重构背景
 *
 * 重构前「云备份与数据导出」只是一个 Toast，提示了一个**根本不存在**的路径：
 * `/sdcard/Android/data/com.lifeordered/files/backup.json`（包名也不对，
 * 真实 applicationId 是 `com.aistudio.lifeordered.warmth`）。
 * 用户以为数据被备份了，实际上什么都没发生。
 *
 * 现在它真的会把三张表写成 JSON，并通过 `FileProvider` 交给系统分享面板，
 * 用户可以存到本地或发送到自己的云盘。
 *
 * 用 `org.json`（Android 框架内置）而不是 Moshi，是为了顺手移除
 * 项目里完全未被使用的 moshi / retrofit / okhttp 依赖。
 */
object DataExporter {

    /** 导出格式版本，便于将来做导入时的兼容判断。 */
    private const val SCHEMA_VERSION = 1

    private const val EXPORT_DIR = "exports"

    /**
     * 将全部数据导出为 JSON 文件。
     *
     * @return 写出的文件。优先使用应用外部文件目录（用户可见、便于分享），
     *         没有外部存储时回退到内部缓存。
     */
    suspend fun exportToJson(context: Context): File = withContext(Dispatchers.IO) {
        val database = LifeOrderedDatabase.getDatabase(context.applicationContext)

        val moments = database.momentDao().getAllMomentsFlow().first()
        val consumables = database.consumableDao().getAllConsumablesFlow().first()
        val inventories = database.inventoryDao().getAllInventoriesFlow().first()
        val categories = database.tagDao().getAllTagsFlow().first()

        val root = JSONObject().apply {
            put("schemaVersion", SCHEMA_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put(
                "moments",
                JSONArray().apply {
                    moments.forEach { moment ->
                        put(
                            JSONObject().apply {
                                put("id", moment.id)
                                put("title", moment.title)
                                put("year", moment.year)
                                put("month", moment.month)
                                put("day", moment.day)
                                put("isLunar", moment.isLunar)
                                put("isLeapMonth", moment.isLeapMonth)
                                put("kind", moment.kind.raw)
                                put("repeat", moment.repeat.raw)
                                put("notes", moment.notes ?: JSONObject.NULL)
                                put("themeColorHex", moment.themeColorHex)
                                put("cardColorIndex", moment.cardColorIndex)
                                put("autoArchive", moment.autoArchive)
                                put("isArchived", moment.isArchived)
                                put("icon", moment.iconPath)
                                put("completionCount", moment.completionCount)
                            }
                        )
                    }
                }
            )
            put(
                "consumables",
                JSONArray().apply {
                    consumables.forEach { consumable ->
                        put(
                            JSONObject().apply {
                                put("id", consumable.id)
                                put("name", consumable.name)
                                put("totalDays", consumable.totalDays)
                                put("startDateMillis", consumable.startDateMillis)
                                put("remainingDays", consumable.getRemainingDays())
                                put("cardColorIndex", consumable.cardColorIndex)
                                put("icon", consumable.iconPath)
                            }
                        )
                    }
                }
            )
            put(
                "inventories",
                JSONArray().apply {
                    inventories.forEach { inventory ->
                        put(
                            JSONObject().apply {
                                put("id", inventory.id)
                                put("name", inventory.name)
                                put("currentQty", inventory.currentQty)
                                put("safeQty", inventory.safeQty)
                                put("unit", inventory.unit)
                                put("category", inventory.category)
                                put("icon", inventory.iconPath)
                                put("isLow", inventory.isLow())
                                put("shelfLifeDays", inventory.shelfLifeDays ?: JSONObject.NULL)
                                put("expiryDate", inventory.expiryDate ?: JSONObject.NULL)
                                put("autoConsumePeriod", inventory.autoConsumePeriod)
                                put("autoConsumeQuantity", inventory.autoConsumeQuantity)
                            }
                        )
                    }
                }
            )
            put(
                "categories",
                JSONArray().apply {
                    categories.forEach { tag ->
                        put(JSONObject().apply {
                            put("id", tag.id)
                            put("name", tag.name)
                            put("colorHex", tag.colorHex)
                        })
                    }
                }
            )
        }

        val directory = resolveExportDirectory(context)
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(directory, "lifeordered-backup-$timestamp.json")
        file.writeText(root.toString(2))
        file
    }

    private fun resolveExportDirectory(context: Context): File {
        val external = context.getExternalFilesDir(null)
        val base = external ?: context.cacheDir
        val directory = File(base, EXPORT_DIR)
        if (!directory.exists()) directory.mkdirs()
        return directory
    }

    /** 生成用于 `FileProvider` 分享的 content:// URI。 */
    fun shareUriFor(context: Context, file: File) =
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
}
