package com.lifeordered.core.utils

import android.icu.util.ChineseCalendar
import android.util.Log
import com.lifeordered.core.date.LunarMonth
import com.lifeordered.core.date.LunarTable
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
 *
 * ## 农历换算为什么改成查表
 *
 * 旧实现直接对 `ChineseCalendar` 写 `set(YEAR, 2024)`。但 ICU 的
 * `ChineseCalendar.YEAR` 不是公历年（基准是 `CHINESE_EPOCH_YEAR = -2636`），
 * 写入公历年会落到公元前，回读校验必然失败 → 一律返回 null →
 * 界面永远提示「该日期在农历中不存在」。
 *
 * 现在所有农历换算都走 [LunarTable]（预先折叠的月份表），
 * 本文件不再写任何 `ChineseCalendar` 字段，只读 `MONTH` /
 * `DAY_OF_MONTH` / `IS_LEAP_MONTH` 这三个可靠字段。
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

    /** 农历月份中文名（「正」「二」…「腊」），索引 0 对应正月。 */
    private val LUNAR_MONTH_NAMES =
        listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "腊")

    private val LUNAR_DIGITS = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")

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
    // 农历换算（全部基于 LunarTable）
    // ---------------------------------------------------------------------

    /**
     * 农历 → 公历。
     *
     * 查 [LunarTable] 的月份表，用「该月初一 + (日 - 1)」直接定位，不做任何
     * `ChineseCalendar` 字段写入。
     *
     * 语义上有两条刻意保留的降级规则：
     * 1. **日超出月长时收敛到月末**（农历小月没有三十，除夕 30 → 29 是正确语义）；
     * 2. **闰月不存在时返回 null**（不收敛到平月）。[MomentSchedule] 的农历年度
     *    重现依赖这一点：2025 年闰六月的生日，在 2026/2027 年不应该有落点。
     */
    fun l2s(year: Int, month: Int, day: Int, isLeapMonth: Boolean = false): Calendar? {
        if (month !in 1..12 || day !in 1..30) return null
        val lunarYear = LunarTable.of(year) ?: return null
        val entry = lunarYear.find(month, isLeapMonth) ?: return null

        val clampedDay = day.coerceAtMost(entry.days)
        return Calendar.getInstance().apply {
            timeInMillis = entry.startMillis
            add(Calendar.DAY_OF_YEAR, clampedDay - 1)
            // 夏令时切换日 add() 可能把时间推到 23:00 或 01:00，重新归零保证是本地日零点
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun lunarCalendarFor(solar: Calendar): ChineseCalendar = ChineseCalendar().apply {
        timeInMillis = solar.timeInMillis
    }

    /**
     * 公历 → 农历。
     *
     * 年标号来自 [LunarTable.labelOf]（正月初一所在公历年），
     * 月/日/闰只读 ICU 的可靠字段——不再反读 `YEAR`。
     */
    private fun lunarDateOf(solar: Calendar): LunarDate {
        val lunar = lunarCalendarFor(solar)
        return LunarDate(
            year = LunarTable.labelOf(solar.timeInMillis),
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
        if (month !in 1..12) return false
        return LunarTable.of(year)?.find(month, isLeapMonth = true) != null
    }

    /** 指定农历年的全部月份（含闰月，按时间顺序）。供轮盘直接选农历日期用。 */
    fun lunarMonthsOf(year: Int): List<LunarMonth> = LunarTable.monthsOf(year)

    /** 指定农历月的天数（29 或 30）；月份不存在时返回 30 以免轮盘塌陷。 */
    fun lunarMonthDays(year: Int, month: Int, isLeapMonth: Boolean = false): Int {
        if (month !in 1..12) return 30
        return LunarTable.of(year)?.find(month, isLeapMonth)?.days ?: 30
    }

    /** 农历月份名：「正月」…「腊月」，闰月加「闰」前缀。 */
    fun getLunarMonthName(month: Int, isLeapMonth: Boolean = false): String {
        if (month !in 1..12) return ""
        val base = LUNAR_MONTH_NAMES[month - 1] + "月"
        return if (isLeapMonth) "闰$base" else base
    }

    /**
     * 农历日的中文名（「初一」…「三十」），**不含月份信息**。
     *
     * 轮盘的日轮直接选农历日期，需要独立的「日名」，
     * 不能像 [getLunarDayName] 那样在初一那天替换成月名。
     */
    fun getLunarDayLabel(lunarDay: Int): String {
        if (lunarDay !in 1..30) return ""
        return when {
            lunarDay <= 10 -> "初" + LUNAR_DIGITS[lunarDay - 1]
            lunarDay < 20 -> "十" + LUNAR_DIGITS[lunarDay - 11]
            lunarDay == 20 -> "二十"
            lunarDay < 30 -> "廿" + LUNAR_DIGITS[lunarDay - 21]
            else -> "三十"
        }
    }

    /** 农历「月 + 日」的展示名；初一那天用月名（传统写法），其余用日名。 */
    fun getLunarDayName(lunarMonth: Int, lunarDay: Int): String {
        if (lunarMonth !in 1..12 || lunarDay !in 1..30) return ""
        if (lunarDay == 1) return getLunarMonthName(lunarMonth)
        return getLunarDayLabel(lunarDay)
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
