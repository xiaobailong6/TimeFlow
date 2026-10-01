package com.lifeordered.core.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CalendarHelper {

    private const val TAG = "CalendarHelper"

    // High fidelity year metadata mapping from 2024 to 2032.
    // Represents Lunar New Year start date in Gregorian, leap month (0 if none), and month days length.
    private class LunarYearInfo(
        val gregorianNewYear: String, // "YYYY-MM-DD"
        val leapMonth: Int, // 0 if none, else leap month index (e.g. 6 means Month 6 is repeated)
        val monthDays: List<Int>
    )

    private val LUST_DATA = mapOf(
        2024 to LunarYearInfo(
            "2024-02-10", 0,
            listOf(30, 30, 29, 30, 29, 30, 29, 29, 30, 29, 30, 29)
        ),
        2025 to LunarYearInfo(
            "2025-01-29", 6,
            listOf(29, 30, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30)
        ),
        2026 to LunarYearInfo(
            "2026-02-17", 0,
            listOf(29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30)
        ),
        2027 to LunarYearInfo(
            "2027-02-06", 0,
            listOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 30, 29, 30)
        ),
        2028 to LunarYearInfo(
            "2028-01-26", 5,
            listOf(30, 29, 30, 29, 30, 29, 29, 30, 29, 30, 29, 30, 30)
        ),
        2029 to LunarYearInfo(
            "2029-02-13", 0,
            listOf(29, 30, 29, 30, 29, 30, 29, 29, 30, 29, 30, 29)
        ),
        2030 to LunarYearInfo(
            "2030-02-03", 0,
            listOf(30, 29, 30, 30, 29, 30, 29, 29, 30, 29, 30, 29)
        ),
        2031 to LunarYearInfo(
            "2031-01-23", 3,
            listOf(30, 29, 30, 30, 29, 30, 29, 29, 30, 29, 30, 29, 30)
        ),
        2032 to LunarYearInfo(
            "2032-02-11", 0,
            listOf(29, 30, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29)
        )
    )

    /**
     * Converts a Lunar date (Year, Month, Day) into Gregorian Calendar.
     * Keeps it extremely reliable for custom anniversaries.
     */
    fun l2s(year: Int, month: Int, day: Int): Calendar? {
        val info = LUST_DATA[year] ?: return null
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        try {
            val date = sdf.parse(info.gregorianNewYear) ?: return null
            cal.time = date
            
            // Sum of days up to the target month
            var dayOffset = 0
            val targetIndex = (month - 1).coerceAtMost(info.monthDays.size - 1)
            
            for (i in 0 until targetIndex) {
                dayOffset += info.monthDays[i]
            }
            
            // Day coercion: if month has only 29 days but 30 is requested, treat as 29 (last day)
            val monthMax = if (targetIndex < info.monthDays.size) info.monthDays[targetIndex] else 30
            dayOffset += (day.coerceIn(1, monthMax) - 1)
            
            cal.add(Calendar.DAY_OF_MONTH, dayOffset)
            return cal
        } catch (e: Exception) {
            Log.e(TAG, "Lunar to Solar calculation failure", e)
            return null
        }
    }

    /**
     * Calculates the remaining days until the next occurrence of a Gregorian (solar) or Lunar event.
     */
    fun getRemainingDays(
        isLunar: Boolean,
        month: Int,
        day: Int,
        isYearly: Boolean = true,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): Int {
        val today = Calendar.getInstance().apply {
            timeInMillis = referenceTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!isLunar) {
            // Gregorian Timeline calculations
            val target = Calendar.getInstance().apply {
                timeInMillis = referenceTimeMillis
                set(Calendar.YEAR, today.get(Calendar.YEAR))
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (isYearly) {
                if (target.before(today)) {
                    target.add(Calendar.YEAR, 1)
                }
            } else {
                if (target.before(today)) {
                    return 0 // Past non-yearly event
                }
            }

            val diff = target.timeInMillis - today.timeInMillis
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        } else {
            // Lunar Timeline calculations
            val currentYear = today.get(Calendar.YEAR)
            var lunarTargetGreg = l2s(currentYear, month, day)

            if (lunarTargetGreg == null) {
                return 365 // safe fallback
            }

            // Standardize lunar target Gregorian date to start-of-day
            lunarTargetGreg.set(Calendar.HOUR_OF_DAY, 0)
            lunarTargetGreg.set(Calendar.MINUTE, 0)
            lunarTargetGreg.set(Calendar.SECOND, 0)
            lunarTargetGreg.set(Calendar.MILLISECOND, 0)

            if (isYearly) {
                if (lunarTargetGreg.before(today)) {
                    // It occurred in the past of the current lunar year. Check next year's conversion
                    lunarTargetGreg = l2s(currentYear + 1, month, day) ?: lunarTargetGreg
                    lunarTargetGreg.set(Calendar.HOUR_OF_DAY, 0)
                    lunarTargetGreg.set(Calendar.MINUTE, 0)
                    lunarTargetGreg.set(Calendar.SECOND, 0)
                    lunarTargetGreg.set(Calendar.MILLISECOND, 0)
                }
            } else {
                if (lunarTargetGreg.before(today)) {
                    return 0 // past once-off lunar event
                }
            }

            val diff = lunarTargetGreg.timeInMillis - today.timeInMillis
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        }
    }

    /**
     * Convert Gregorian date to Chinese Lunar Month and Day Pair
     */
    fun getLunarDate(gregYear: Int, gregMonth: Int, gregDay: Int): Pair<Int, Int>? {
        var lunarYear = gregYear
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, gregYear)
            set(Calendar.MONTH, gregMonth - 1)
            set(Calendar.DAY_OF_MONTH, gregDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        var yearInfo = LUST_DATA[lunarYear] ?: return null
        var newYearCal = Calendar.getInstance().apply {
            time = sdf.parse(yearInfo.gregorianNewYear)!!
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        if (targetCal.before(newYearCal)) {
            lunarYear -= 1
            yearInfo = LUST_DATA[lunarYear] ?: return null
            newYearCal = Calendar.getInstance().apply {
                time = sdf.parse(yearInfo.gregorianNewYear)!!
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
        
        val diffMillis = targetCal.timeInMillis - newYearCal.timeInMillis
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        
        var dayOffset = diffDays
        for (i in 0 until yearInfo.monthDays.size) {
            val mDays = yearInfo.monthDays[i]
            if (dayOffset < mDays) {
                return Pair(i + 1, dayOffset + 1)
            }
            dayOffset -= mDays
        }
        return Pair(12, 30)
    }

    /**
     * Get the Chinese Lunar Day name
     */
    fun getLunarDayName(lunarMonth: Int, lunarDay: Int): String {
        if (lunarDay == 1) {
            val monthNames = listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "腊")
            val index = (lunarMonth - 1).coerceIn(0, 11)
            return monthNames[index] + "月"
        }
        val t2 = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
        return if (lunarDay <= 10) {
            "初" + t2[lunarDay - 1]
        } else if (lunarDay < 20) {
            "十" + t2[lunarDay - 11]
        } else if (lunarDay == 20) {
            "二十"
        } else if (lunarDay < 30) {
            "廿" + t2[lunarDay - 21]
        } else if (lunarDay == 30) {
            "三十"
        } else {
            "初一"
        }
    }

    /**
     * Get Chinese traditional festivals or generic Lunar day name
     */
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
     * Checks if a Moment occurs on a specific Gregorian date
     */
    fun isMomentOnDate(moment: com.lifeordered.data.models.Moment, year: Int, month: Int, day: Int): Boolean {
        if (moment.isCompleted || moment.isArchived) return false
        if (moment.isLunar) {
            val lunar = getLunarDate(year, month, day) ?: return false
            val isYearlyRepeat = moment.isYearly || moment.repeatType == "yearly"
            return if (isYearlyRepeat) {
                moment.month == lunar.first && moment.day == lunar.second
            } else {
                moment.year == year && moment.month == lunar.first && moment.day == lunar.second
            }
        } else {
            val isYearlyRepeat = moment.isYearly || moment.repeatType == "yearly"
            return if (isYearlyRepeat) {
                moment.month == month && moment.day == day
            } else {
                moment.year == year && moment.month == month && moment.day == day
            }
        }
    }

    /**
     * Compute remaining or elapsed days for a Moment based on type, repeat, and start date.
     */
    fun calculateDays(moment: com.lifeordered.data.models.Moment, referenceTimeMillis: Long = System.currentTimeMillis()): Int {
        val today = Calendar.getInstance().apply {
            timeInMillis = referenceTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startCal = Calendar.getInstance().apply {
            if (moment.isLunar) {
                val converted = l2s(moment.year, moment.month, moment.day)
                if (converted != null) {
                    timeInMillis = converted.timeInMillis
                } else {
                    set(Calendar.YEAR, moment.year)
                    set(Calendar.MONTH, moment.month - 1)
                    set(Calendar.DAY_OF_MONTH, moment.day)
                }
            } else {
                set(Calendar.YEAR, moment.year)
                set(Calendar.MONTH, moment.month - 1)
                set(Calendar.DAY_OF_MONTH, moment.day)
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (moment.type == "countUp") {
            if (today.before(startCal)) {
                return 0
            }
            val diff = today.timeInMillis - startCal.timeInMillis
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        } else {
            // countdown
            when (moment.repeatType) {
                "none" -> {
                    val diff = startCal.timeInMillis - today.timeInMillis
                    return (diff / (1000 * 60 * 60 * 24)).toInt()
                }
                "weekly" -> {
                    val targetDayOfWeek = startCal.get(Calendar.DAY_OF_WEEK)
                    val todayDayOfWeek = today.get(Calendar.DAY_OF_WEEK)
                    var daysDiff = targetDayOfWeek - todayDayOfWeek
                    if (daysDiff < 0) {
                        daysDiff += 7
                    }
                    return daysDiff
                }
                "monthly" -> {
                    if (!moment.isLunar) {
                        val targetDom = startCal.get(Calendar.DAY_OF_MONTH)
                        val targetCal = Calendar.getInstance().apply {
                            timeInMillis = today.timeInMillis
                            val maxInThisMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
                            set(Calendar.DAY_OF_MONTH, targetDom.coerceIn(1, maxInThisMonth))
                        }
                        if (targetCal.before(today)) {
                            targetCal.add(Calendar.MONTH, 1)
                            val maxInNextMonth = targetCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            targetCal.set(Calendar.DAY_OF_MONTH, targetDom.coerceIn(1, maxInNextMonth))
                        }
                        val diff = targetCal.timeInMillis - today.timeInMillis
                        return (diff / (1000 * 60 * 60 * 24)).toInt()
                    } else {
                        // Monthly Lunar Repeat: Same day every Lunar month
                        val todayLunar = getLunarDate(
                            today.get(Calendar.YEAR),
                            today.get(Calendar.MONTH) + 1,
                            today.get(Calendar.DAY_OF_MONTH)
                        ) ?: return 0
                        
                        var targetLunarMonth = todayLunar.first
                        var targetLunarYear = today.get(Calendar.YEAR)
                        
                        if (todayLunar.second > moment.day) {
                            targetLunarMonth += 1
                        }
                        
                        // Handle year overflow for lunar months
                        val yearInfo = LUST_DATA[targetLunarYear]
                        if (yearInfo != null && targetLunarMonth > yearInfo.monthDays.size) {
                            targetLunarMonth = 1
                            targetLunarYear += 1
                        }
                        
                        val targetGreg = l2s(targetLunarYear, targetLunarMonth, moment.day) ?: return 0
                        targetGreg.set(Calendar.HOUR_OF_DAY, 0)
                        targetGreg.set(Calendar.MINUTE, 0)
                        targetGreg.set(Calendar.SECOND, 0)
                        targetGreg.set(Calendar.MILLISECOND, 0)
                        
                        val diff = targetGreg.timeInMillis - today.timeInMillis
                        return (diff / (1000 * 60 * 60 * 24)).toInt()
                    }
                }
                "yearly" -> {
                    if (!moment.isLunar) {
                        val targetCal = Calendar.getInstance().apply {
                            timeInMillis = today.timeInMillis
                            set(Calendar.MONTH, moment.month - 1)
                            set(Calendar.DAY_OF_MONTH, moment.day)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        if (targetCal.before(today)) {
                            targetCal.add(Calendar.YEAR, 1)
                        }
                        val diff = targetCal.timeInMillis - today.timeInMillis
                        return (diff / (1000 * 60 * 60 * 24)).toInt()
                    } else {
                        val currentYear = today.get(Calendar.YEAR)
                        var lunarTargetGreg = l2s(currentYear, moment.month, moment.day) ?: startCal
                        lunarTargetGreg.set(Calendar.HOUR_OF_DAY, 0)
                        lunarTargetGreg.set(Calendar.MINUTE, 0)
                        lunarTargetGreg.set(Calendar.SECOND, 0)
                        lunarTargetGreg.set(Calendar.MILLISECOND, 0)

                        if (lunarTargetGreg.before(today)) {
                            lunarTargetGreg = l2s(currentYear + 1, moment.month, moment.day) ?: lunarTargetGreg
                            lunarTargetGreg.set(Calendar.HOUR_OF_DAY, 0)
                            lunarTargetGreg.set(Calendar.MINUTE, 0)
                            lunarTargetGreg.set(Calendar.SECOND, 0)
                            lunarTargetGreg.set(Calendar.MILLISECOND, 0)
                        }
                        val diff = lunarTargetGreg.timeInMillis - today.timeInMillis
                        return (diff / (1000 * 60 * 60 * 24)).toInt()
                    }
                }
                else -> {
                    val diff = startCal.timeInMillis - today.timeInMillis
                    return (diff / (1000 * 60 * 60 * 24)).toInt()
                }
            }
        }
    }
}
