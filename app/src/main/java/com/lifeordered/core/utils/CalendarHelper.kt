package com.lifeordered.core.utils

import android.icu.util.ChineseCalendar
import android.util.Log
import com.lifeordered.core.date.MomentDays
import com.lifeordered.core.date.MomentRepeat
import com.lifeordered.core.date.MomentSchedule
import com.lifeordered.data.models.Moment
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

/**
 * 日期基础工具 + 公历/农历换算。
 *
 * ## 重构说明
 *
 * 重构前本文件同时承担两件事：
 * 1. 公历/农历换算与本地日历日运算（**保留**）
 * 2. 时刻的天数计算与日历落点判定（**已迁出**）
 *
 * 第 2 件事原本有 3 套互相矛盾的口径（`calculateDays` / `isMomentOnDate` /
 * `getRemainingDays`），现在统一由 [MomentSchedule] 实现。
 * 本文件中对应的公开函数保留为**兼容门面**，内部直接委托，
 * 因此既有调用方与单元测试无需改动即可继续工作。
 *
 * 所有比较都基于**本地日历日**：用 `startOfDay` + UTC 序数求差，
 * 绝不拿毫秒除以 86400000（夏令时会让一天变成 23 或 25 小时）。
 */
data class LunarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean
)

object CalendarHelper {

    private const val TAG = "CalendarHelper"
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    // ---------------------------------------------------------------------
    // 本地日历日基础运算
    // ---------------------------------------------------------------------

    private fun startOfDay(timeMillis: Long): Calendar = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    /** 返回指定时刻所在**本地日期**的零点毫秒。供 [MomentSchedule] 复用。 */
    fun startOfLocalDay(timeMillis: Long): Long = startOfDay(timeMillis).timeInMillis

    private fun localDate(year: Int, month: Int, day: Int): Calendar? {
        if (month !in 1..12 || day < 1) return null
        return try {
            Calendar.getInstance().apply {
                clear()
                isLenient = false
                set(year, month - 1, day, 0, 0, 0)
                timeInMillis // 在 isLenient = false 下强制校验日期合法性
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** 按本地日历加天，不假设一天恰好 24 小时。 */
    fun addLocalDays(timeMillis: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

    /** 两个本地日历日之间的天数差，与夏令时无关。 */
    fun localDaysBetween(startMillis: Long, endMillis: Long): Int =
        daysBetween(startOfDay(startMillis), startOfDay(endMillis))

    /** 返回与时区无关的格里高利日期序数。 */
    private fun dateOrdinal(calendar: Calendar): Long {
        val utc = GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US).apply {
            setGregorianChange(Date(Long.MIN_VALUE))
            clear()
            set(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
        return Math.floorDiv(utc.timeInMillis, MILLIS_PER_DAY)
    }

    private fun daysBetween(from: Calendar, to: Calendar): Int =
        (dateOrdinal(to) - dateOrdinal(from)).toInt()

    // ---------------------------------------------------------------------
    // 农历换算
    // ---------------------------------------------------------------------

    /**
     * 农历 → 公历。
     *
     * 基于 `android.icu.util.ChineseCalendar`（含完整天文数据），并对结果做
     * 回读校验：年/月/日/闰月四项必须与原请求完全一致，否则判为非法。
     * 农历 30 日在小月不存在时，按「周年纪念日」惯例降级到 29 日。
     */
    fun l2s(year: Int, month: Int, day: Int, isLeapMonth: Boolean = false): Calendar? {
        if (year < 1 || month !in 1..12 || day !in 1..30) return null

        fun convert(candidateDay: Int): Calendar? {
            return try {
                val lunar = ChineseCalendar().apply {
                    clear()
                    isLenient = false
                    set(ChineseCalendar.ERA, 1)
                    set(ChineseCalendar.YEAR, year)
                    set(ChineseCalendar.MONTH, month - 1)
                    set(ChineseCalendar.DAY_OF_MONTH, candidateDay)
                    set(ChineseCalendar.IS_LEAP_MONTH, if (isLeapMonth) 1 else 0)
                    timeInMillis // 强制校验
                }
                if (lunar.get(ChineseCalendar.YEAR) != year ||
                    lunar.get(ChineseCalendar.MONTH) != month - 1 ||
                    lunar.get(ChineseCalendar.DAY_OF_MONTH) != candidateDay ||
                    (lunar.get(ChineseCalendar.IS_LEAP_MONTH) == 1) != isLeapMonth
                ) {
                    return null
                }
                Calendar.getInstance().apply {
                    timeInMillis = lunar.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        return convert(day) ?: if (day == 30) convert(29) else null
    }

    private fun lunarCalendarFor(solar: Calendar): ChineseCalendar = ChineseCalendar().apply {
        timeInMillis = solar.timeInMillis
    }

    private fun lunarDateOf(solar: Calendar): LunarDate {
        val lunar = lunarCalendarFor(solar)
        return LunarDate(
            year = lunar.get(ChineseCalendar.YEAR),
            month = lunar.get(ChineseCalendar.MONTH) + 1,
            day = lunar.get(ChineseCalendar.DAY_OF_MONTH),
            isLeapMonth = lunar.get(ChineseCalendar.IS_LEAP_MONTH) == 1
        )
    }

    /** 公历 → 完整农历日期（含闰月标记）。 */
    fun getLunarDate(gregYear: Int, gregMonth: Int, gregDay: Int): LunarDate? {
        val solar = localDate(gregYear, gregMonth, gregDay) ?: return null
        return try {
            lunarDateOf(solar)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Solar to lunar calculation failure", e)
            null
        }
    }

    /** 指定农历年是否包含所请求的闰月。 */
    fun hasLeapMonth(year: Int, month: Int): Boolean {
        val start = l2s(year, month, 1) ?: return false
        val cursor = start.clone() as Calendar
        repeat(40) {
            val lunar = lunarDateOf(cursor)
            if (lunar.year == year && lunar.month == month && lunar.isLeapMonth) return true
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return false
    }

    fun getLunarDayName(lunarMonth: Int, lunarDay: Int): String {
        if (lunarMonth !in 1..12 || lunarDay !in 1..30) return ""
        if (lunarDay == 1) {
            val monthNames =
                listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "腊")
            return monthNames[lunarMonth - 1] + "月"
        }
        val digits = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
        return when {
            lunarDay <= 10 -> "初" + digits[lunarDay - 1]
            lunarDay < 20 -> "十" + digits[lunarDay - 11]
            lunarDay == 20 -> "二十"
            lunarDay < 30 -> "廿" + digits[lunarDay - 21]
            else -> "三十"
        }
    }

    fun getLunarFestivalOrDayName(lunarMonth: Int, lunarDay: Int): String {
        if (lunarMonth == 1 && lunarDay == 1) return "春节"
        if (lunarMonth == 1 && lunarDay == 15) return "元宵"
        if (lunarMonth == 5 && lunarDay == 5) return "端午"
        if (lunarMonth == 7 && lunarDay == 7) return "七夕"
        if (lunarMonth == 8 && lunarDay == 15) return "中秋"
        if (lunarMonth == 9 && lunarDay == 9) return "重阳"
        if (lunarMonth == 12 && lunarDay == 8) return "腊八"
        if (lunarMonth == 12 && lunarDay == 30) return "除夕"
        return getLunarDayName(lunarMonth, lunarDay)
    }

    /**
     * 求某农历日期在指定公历年内的公历落点。
     *
     * 存储的农历年份是创建时刻的年份，因此需在 ±1 年范围内搜索。
     */
    fun solarDateInGregorianYear(
        lunarMonth: Int,
        lunarDay: Int,
        isLeapMonth: Boolean = false,
        gregorianYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    ): Calendar? {
        if (gregorianYear < 1) return null
        return (gregorianYear - 1..gregorianYear + 1)
            .asSequence()
            .mapNotNull { lunarYear -> l2s(lunarYear, lunarMonth, lunarDay, isLeapMonth) }
            .firstOrNull { it.get(Calendar.YEAR) == gregorianYear }
    }

    // ---------------------------------------------------------------------
    // 兼容门面（实现在 MomentSchedule）
    // ---------------------------------------------------------------------

    /**
     * 该时刻是否落在指定公历日期上。
     *
     * @see MomentSchedule.occursOn 权威实现。此处仅为兼容旧调用方。
     */
    fun isMomentOnDate(moment: Moment, year: Int, month: Int, day: Int): Boolean =
        MomentSchedule.occursOn(moment, year, month, day)

    /**
     * 该时刻当前的天数。
     *
     * 兼容旧签名（返回 `Int`）。**新代码请直接使用 [MomentSchedule.resolve]**：
     * 这里为了维持 `Int` 契约，只能把「日期非法」折叠成 `0`，
     * 而 `0` 同时也表示「就是今天」，两者无法区分。
     */
    fun calculateDays(moment: Moment, referenceTimeMillis: Long = System.currentTimeMillis()): Int =
        when (val result = MomentSchedule.resolve(moment, referenceTimeMillis)) {
            is MomentDays.Countdown -> if (result.isOverdue) -result.days else result.days
            is MomentDays.CountUp -> result.elapsedDays
            MomentDays.Invalid -> 0
        }

    /**
     * 打卡后推进到下一个周期。
     *
     * 兼容旧行为：一次性事件（`repeatType == "none"`）原样返回。
     * 新代码请使用 [MomentSchedule.advance]（它同时负责累加打卡次数）。
     */
    fun advanceRecurringMoment(moment: Moment): Moment =
        if (moment.repeat == MomentRepeat.NONE) moment else MomentSchedule.advance(moment)
}
