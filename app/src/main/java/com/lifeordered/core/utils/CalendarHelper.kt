package com.lifeordered.core.utils

import android.icu.util.ChineseCalendar
import android.util.Log
import com.lifeordered.data.models.Moment
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

/**
 * Date calculations used by countdowns, the calendar screen and reminders.
 *
 * All comparisons are performed on local calendar dates.  We deliberately do
 * not divide elapsed milliseconds by 24 hours because daylight-saving changes
 * can make a local day shorter or longer than 24 hours.
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

    private fun startOfDay(timeMillis: Long): Calendar = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun localDate(year: Int, month: Int, day: Int): Calendar? {
        if (month !in 1..12 || day < 1) return null
        return try {
            Calendar.getInstance().apply {
                clear()
                isLenient = false
                set(year, month - 1, day, 0, 0, 0)
                timeInMillis // Force validation while lenient is disabled.
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun annualSolarDate(year: Int, month: Int, day: Int): Calendar? =
        localDate(year, month, day) ?: if (month == 2 && day == 29) localDate(year, 2, 28) else null

    private fun annualSolarDayMatches(selected: Calendar, month: Int, day: Int): Boolean =
        selected.get(Calendar.MONTH) == month - 1 &&
            (selected.get(Calendar.DAY_OF_MONTH) == day ||
                (month == 2 && day == 29 && selected.get(Calendar.DAY_OF_MONTH) == 28 &&
                    selected.getActualMaximum(Calendar.DAY_OF_MONTH) == 28))

    /** Adds calendar days in the device timezone without assuming a day is 24 hours. */
    fun addLocalDays(timeMillis: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

    /** Difference between two local calendar dates, independent of DST changes. */
    fun localDaysBetween(startMillis: Long, endMillis: Long): Int =
        daysBetween(startOfDay(startMillis), startOfDay(endMillis))

    /** Returns a timezone-independent ordinal for a Gregorian date. */
    private fun dateOrdinal(calendar: Calendar): Long {
        val utc = GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US).apply {
            setGregorianChange(Date(Long.MIN_VALUE))
            clear()
            set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        }
        return Math.floorDiv(utc.timeInMillis, MILLIS_PER_DAY)
    }

    private fun daysBetween(from: Calendar, to: Calendar): Int =
        (dateOrdinal(to) - dateOrdinal(from)).toInt()

    private fun normalizedDate(calendar: Calendar): Calendar = startOfDay(calendar.timeInMillis)

    /**
     * Converts a Chinese lunar date to a Gregorian local date.
     *
     * The old implementation used a short hand-written table and silently
     * treated leap months as ordinary months. Android ICU contains the full
     * astronomical calendar data for the supported Android release and also
     * handles year boundaries correctly.
     *
     * Moment stores whether the requested month is a leap month. If day 30
     * does not exist, it follows the common anniversary convention and uses
     * the last day of that lunar month.
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
                    timeInMillis // Force validation.
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

    /** Calculates the remaining days until a solar or lunar annual date. */
    fun getRemainingDays(
        isLunar: Boolean,
        month: Int,
        day: Int,
        isYearly: Boolean = true,
        isLeapMonth: Boolean = false,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): Int {
        val today = startOfDay(referenceTimeMillis)
        val target = if (isLunar && isYearly) {
            val currentLunarYear = lunarYearOf(today)
            (currentLunarYear..(currentLunarYear + 20)).asSequence()
                .mapNotNull { year -> l2s(year, month, day, isLeapMonth)?.let(::normalizedDate) }
                .firstOrNull { !it.before(today) }
                ?: return 0
        } else {
            val currentYear = if (isLunar) lunarYearOf(today) else today.get(Calendar.YEAR)
            var candidate = if (isLunar) {
                l2s(currentYear, month, day, isLeapMonth) ?: return 0
            } else {
                annualSolarDate(currentYear, month, day) ?: return 0
            }
            candidate = normalizedDate(candidate)
            if (isYearly && candidate.before(today)) {
                candidate = if (isLunar) {
                    l2s(currentYear + 1, month, day, isLeapMonth)?.let(::normalizedDate) ?: return 0
                } else {
                    annualSolarDate(currentYear + 1, month, day) ?: return 0
                }
            }
            normalizedDate(candidate)
        }
        val difference = daysBetween(today, target)
        return if (isYearly) difference else difference.coerceAtLeast(0)
    }

    private fun lunarCalendarFor(solar: Calendar): ChineseCalendar = ChineseCalendar().apply {
        timeInMillis = solar.timeInMillis
    }

    private fun lunarYearOf(solar: Calendar): Int =
        lunarCalendarFor(solar).get(ChineseCalendar.YEAR)

    private fun lunarDateOf(solar: Calendar): LunarDate {
        val lunar = lunarCalendarFor(solar)
        return LunarDate(
            year = lunar.get(ChineseCalendar.YEAR),
            month = lunar.get(ChineseCalendar.MONTH) + 1,
            day = lunar.get(ChineseCalendar.DAY_OF_MONTH),
            isLeapMonth = lunar.get(ChineseCalendar.IS_LEAP_MONTH) == 1
        )
    }

    /** Converts a Gregorian date to a complete Chinese lunar date. */
    fun getLunarDate(gregYear: Int, gregMonth: Int, gregDay: Int): LunarDate? {
        val solar = localDate(gregYear, gregMonth, gregDay) ?: return null
        return try {
            lunarDateOf(solar)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Solar to lunar calculation failure", e)
            null
        }
    }


    /** Returns whether the specified lunar year contains the requested leap month. */
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

    private fun lunarDayMatches(requestedDay: Int, actualDay: Int): Boolean =
        actualDay == requestedDay || (requestedDay == 30 && actualDay == 29)

    fun getLunarDayName(lunarMonth: Int, lunarDay: Int): String {
        if (lunarMonth !in 1..12 || lunarDay !in 1..30) return ""
        if (lunarDay == 1) {
            val monthNames = listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "腊")
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

    /** Checks whether a moment should appear on a Gregorian calendar date. */
    fun isMomentOnDate(moment: Moment, year: Int, month: Int, day: Int): Boolean {
        if (moment.isCompleted || moment.isArchived || localDate(year, month, day) == null) return false

        val selected = localDate(year, month, day) ?: return false
        val isYearly = moment.isYearly || moment.repeatType == "yearly"

        if (moment.isLunar) {
            val lunar = getLunarDate(year, month, day) ?: return false
            val lunarMatches = when (moment.repeatType) {
                "weekly" -> l2s(moment.year, moment.month, moment.day, moment.isLeapMonth)
                    ?.let { selected.get(Calendar.DAY_OF_WEEK) == it.get(Calendar.DAY_OF_WEEK) }
                    ?: false
                "monthly" -> lunarDayMatches(moment.day, lunar.day) &&
                    lunar.isLeapMonth == moment.isLeapMonth
                "yearly" -> isYearly && lunar.month == moment.month &&
                    lunarDayMatches(moment.day, lunar.day) && lunar.isLeapMonth == moment.isLeapMonth
                else -> if (isYearly) {
                    lunar.month == moment.month && lunarDayMatches(moment.day, lunar.day) &&
                        lunar.isLeapMonth == moment.isLeapMonth
                } else {
                    val start = l2s(moment.year, moment.month, moment.day, moment.isLeapMonth)
                    start != null && dateOrdinal(selected) == dateOrdinal(start)
                }
            }
            return lunarMatches && selectedOnOrAfterStart(moment, selected)
        }

        val start = localDate(moment.year, moment.month, moment.day) ?: return false
        return when (moment.repeatType) {
            "weekly" -> selected.get(Calendar.DAY_OF_WEEK) == start.get(Calendar.DAY_OF_WEEK) &&
                selectedOnOrAfterStart(moment, selected)
            "monthly" -> selected.get(Calendar.DAY_OF_MONTH) ==
                moment.day.coerceAtMost(selected.getActualMaximum(Calendar.DAY_OF_MONTH)) &&
                selectedOnOrAfterStart(moment, selected)
            "yearly" -> isYearly && annualSolarDayMatches(selected, moment.month, moment.day) &&
                selectedOnOrAfterStart(moment, selected)
            else -> if (isYearly) annualSolarDayMatches(selected, moment.month, moment.day)
            else year == moment.year && month == moment.month && day == moment.day
        }
    }
    private fun selectedOnOrAfterStart(moment: Moment, selected: Calendar): Boolean {
        val start = if (moment.isLunar) l2s(moment.year, moment.month, moment.day, moment.isLeapMonth) else
            localDate(moment.year, moment.month, moment.day)
        return start == null || dateOrdinal(selected) >= dateOrdinal(start)
    }

    /**
     * Computes remaining days for a moment. Positive values are future days;
     * negative values are elapsed days for non-recurring countdowns.
     */
    fun calculateDays(moment: Moment, referenceTimeMillis: Long = System.currentTimeMillis()): Int {
        val today = startOfDay(referenceTimeMillis)
        val original = if (moment.isLunar) {
            l2s(moment.year, moment.month, moment.day, moment.isLeapMonth) ?: return 0
        } else {
            localDate(moment.year, moment.month, moment.day) ?: return 0
        }

        if (moment.type == "countUp") {
            return daysBetween(original, today).coerceAtLeast(0)
        }

        return when (moment.repeatType) {
            "weekly" -> daysUntilWeekday(today, original.get(Calendar.DAY_OF_WEEK))
            "monthly" -> if (moment.isLunar) {
                daysUntilLunarMonthly(today, moment, original)
            } else {
                daysUntilMonthly(today, moment.day)
            }
            "yearly" -> daysUntilYearly(today, moment)
            else -> daysBetween(today, original)
        }
    }

    private fun daysUntilWeekday(today: Calendar, targetDayOfWeek: Int): Int {
        val current = today.get(Calendar.DAY_OF_WEEK)
        return (targetDayOfWeek - current + 7) % 7
    }

    private fun daysUntilMonthly(today: Calendar, targetDay: Int): Int {
        val candidate = today.clone() as Calendar
        candidate.set(Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(candidate.getActualMaximum(Calendar.DAY_OF_MONTH)))
        if (candidate.before(today)) {
            candidate.add(Calendar.MONTH, 1)
            candidate.set(Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(candidate.getActualMaximum(Calendar.DAY_OF_MONTH)))
        }
        return daysBetween(today, candidate)
    }

    private fun daysUntilLunarMonthly(today: Calendar, moment: Moment, original: Calendar): Int {
        val cursor = (if (original.after(today)) original else today).clone() as Calendar
        repeat(370) {
            val lunar = lunarDateOf(cursor)
            if (lunarDayMatches(moment.day, lunar.day) &&
                lunar.isLeapMonth == moment.isLeapMonth &&
                dateOrdinal(cursor) >= dateOrdinal(original)
            ) {
                return daysBetween(today, cursor)
            }
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return 0
    }

    private fun daysUntilYearly(today: Calendar, moment: Moment): Int {
        if (moment.isLunar) {
            val currentYear = lunarYearOf(today)
            val target = (currentYear..(currentYear + 20)).asSequence()
                .mapNotNull { year ->
                    l2s(year, moment.month, moment.day, moment.isLeapMonth)?.let(::normalizedDate)
                }
                .firstOrNull { !it.before(today) }
                ?: return 0
            return daysBetween(today, target)
        }

        val currentYear = today.get(Calendar.YEAR)
        var target = annualSolarDate(currentYear, moment.month, moment.day) ?: return 0
        target = normalizedDate(target)
        if (target.before(today)) {
            target = annualSolarDate(currentYear + 1, moment.month, moment.day) ?: return 0
        }
        return daysBetween(today, normalizedDate(target))
    }

    /** Returns the next stored occurrence after completing a recurring moment. */
    fun advanceRecurringMoment(moment: Moment): Moment {
        if (moment.repeatType == "none") return moment

        if (moment.isLunar) {
            if (moment.repeatType == "yearly") {
                val nextYear = (moment.year + 1..moment.year + 20).asSequence()
                    .firstOrNull { l2s(it, moment.month, moment.day, moment.isLeapMonth) != null }
                if (nextYear != null) {
                    return moment.copy(
                        year = nextYear,
                        completionCount = moment.completionCount + 1
                    )
                }
            }

            val currentSolar = l2s(moment.year, moment.month, moment.day, moment.isLeapMonth)
            if (currentSolar != null) {
                val nextLunar = lunarCalendarFor(currentSolar).apply {
                    when (moment.repeatType) {
                        "weekly" -> add(Calendar.DAY_OF_YEAR, 7)
                        "monthly" -> add(ChineseCalendar.MONTH, 1)
                    }
                }
                return moment.copy(
                    year = nextLunar.get(ChineseCalendar.YEAR),
                    month = nextLunar.get(ChineseCalendar.MONTH) + 1,
                    day = if (moment.day == 30) 30 else nextLunar.get(ChineseCalendar.DAY_OF_MONTH),
                    isLeapMonth = nextLunar.get(ChineseCalendar.IS_LEAP_MONTH) == 1,
                    completionCount = moment.completionCount + 1
                )
            }
        } else {
            val next = localDate(moment.year, moment.month, moment.day)
            if (next != null) {
                when (moment.repeatType) {
                    "weekly" -> next.add(Calendar.WEEK_OF_YEAR, 1)
                    "monthly" -> next.add(Calendar.MONTH, 1)
                    "yearly" -> next.add(Calendar.YEAR, 1)
                }
                return moment.copy(
                    year = next.get(Calendar.YEAR),
                    month = next.get(Calendar.MONTH) + 1,
                    day = next.get(Calendar.DAY_OF_MONTH),
                    completionCount = moment.completionCount + 1
                )
            }
        }

        return moment.copy(completionCount = moment.completionCount + 1)
    }
}
