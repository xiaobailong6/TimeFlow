package com.lifeordered.core.date

import android.icu.util.ChineseCalendar
import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.data.models.Moment
import java.util.Calendar

/**
 * 时刻日期计算的**唯一权威实现**。
 *
 * 重构前这套逻辑散落在三处且口径不一致：
 * - `CalendarHelper.calculateDays`  —— 卡片天数，认 `type`，按 `repeatType` 分支
 * - `CalendarHelper.isMomentOnDate` —— 日历落点，**不认 `type`**，另外读 `isYearly`
 * - `CalendarHelper.getRemainingDays` —— 死方法，按月/日 + `isYearly`
 *
 * 三者对同一份数据可能给出互相矛盾的结果（典型：正数日在首页显示「已过 100 天」，
 * 却只在自己起始日那天出现在日历上）。现在统一为：
 *
 * - [resolve]    → 这个时刻「现在该显示多少天」（UI 展示口径）
 * - [occursOn]   → 这个时刻「是否落在某个日期」（日历口径）
 * - [advance]    → 打卡后「下一次该是什么状态」（写入口径）
 *
 * 三个函数共享同一套日期解析与周期判定，因此不会再出现口径分叉。
 *
 * 设计约定（重构后明确下来的语义）：
 * - **方向**（[MomentKind]）只影响天数如何呈现，不影响出现规律。
 * - **周期**（[MomentRepeat]）只影响何时出现，不影响正数日的天数累积。
 * - 正数日的天数是「从起始日累计至今」，**不随打卡重置**，因此打卡永不推进日期。
 */
object MomentSchedule {

    /** 农历年度搜索上限。超出仍找不到合法日期时返回 [MomentDays.Invalid]。 */
    private const val LUNAR_YEAR_SEARCH_WINDOW = 20

    /** 农历按月匹配时逐日推进的上限（覆盖一个农历年 + 闰月）。 */
    private const val LUNAR_DAY_SEARCH_WINDOW = 400

    // ---------------------------------------------------------------------
    // 公开 API
    // ---------------------------------------------------------------------

    /**
     * 计算该时刻当前应展示的天数。
     *
     * @return [MomentDays.Countdown] / [MomentDays.CountUp] / [MomentDays.Invalid]。
     *         调用方**必须**处理 [MomentDays.Invalid]，不能再把 0 当作「算不出来」。
     */
    fun resolve(moment: Moment, nowMillis: Long = System.currentTimeMillis()): MomentDays {
        val original = startDateMillisOf(moment) ?: return MomentDays.Invalid
        val today = CalendarHelper.startOfLocalDay(nowMillis)

        return when (moment.kind) {
            // 正数日：从起始日累计至今。周期不参与，否则「已过 N 天」会被重置。
            MomentKind.COUNT_UP -> {
                val elapsed = CalendarHelper.localDaysBetween(original, today)
                MomentDays.CountUp(elapsed.coerceAtLeast(0))
            }

            MomentKind.COUNTDOWN -> when (moment.repeat) {
                MomentRepeat.NONE -> {
                    val remaining = CalendarHelper.localDaysBetween(today, original)
                    if (remaining >= 0) {
                        MomentDays.Countdown(remaining, isOverdue = false)
                    } else {
                        MomentDays.Countdown(-remaining, isOverdue = true)
                    }
                }

                MomentRepeat.WEEKLY -> {
                    val targetDow = dayOfWeekOf(original)
                    val currentDow = Calendar.getInstance().apply { timeInMillis = today }
                        .get(Calendar.DAY_OF_WEEK)
                    MomentDays.Countdown((targetDow - currentDow + 7) % 7, isOverdue = false)
                }

                MomentRepeat.MONTHLY -> {
                    val days = if (moment.isLunar) {
                        daysUntilLunarMonthly(today, moment, original)
                    } else {
                        daysUntilSolarMonthly(today, moment.day)
                    }
                    days?.let { MomentDays.Countdown(it, isOverdue = false) } ?: MomentDays.Invalid
                }

                MomentRepeat.YEARLY -> {
                    val days = daysUntilYearly(today, moment)
                    days?.let { MomentDays.Countdown(it, isOverdue = false) } ?: MomentDays.Invalid
                }
            }
        }
    }

    /**
     * 判断该时刻是否应当出现在指定公历日期上（日历页 / 月历圆点用）。
     *
     * 与重构前的关键差异：**周期判定对倒数日与正数日一视同仁**。
     * 重构前 `isMomentOnDate` 完全不看 `type`，而 `resolve` 把 `countUp`
     * 提到最高优先级，导致两个界面对同一数据给出矛盾结论。
     */
    fun occursOn(moment: Moment, year: Int, month: Int, day: Int): Boolean {
        if (moment.isArchived) return false

        val original = startDateMillisOf(moment) ?: return false
        val selected = localDateMillisOrNull(year, month, day) ?: return false

        // 用本地日历日差代替序数比较：CalendarHelper.localDaysBetween 内部已按
        // UTC 序数计算，夏令时不会让相邻两天相差 23/25 小时。
        val daysFromOriginal = CalendarHelper.localDaysBetween(original, selected)

        return when (moment.repeat) {
            MomentRepeat.NONE -> daysFromOriginal == 0

            // 循环事件只在起始日「及之后」出现，避免历史上溯出现幽灵事件。
            MomentRepeat.WEEKLY ->
                daysFromOriginal >= 0 && dayOfWeekOf(selected) == dayOfWeekOf(original)

            MomentRepeat.MONTHLY -> {
                if (daysFromOriginal < 0) {
                    false
                } else if (moment.isLunar) {
                    val lunar = CalendarHelper.getLunarDate(year, month, day)
                    lunar != null &&
                        lunarDayMatches(moment.day, lunar.day) &&
                        lunar.isLeapMonth == moment.isLeapMonth
                } else {
                    val maxDay = Calendar.getInstance().apply {
                        clear()
                        set(year, month - 1, 1)
                    }.getActualMaximum(Calendar.DAY_OF_MONTH)
                    day == moment.day.coerceAtMost(maxDay)
                }
            }

            MomentRepeat.YEARLY -> {
                if (daysFromOriginal < 0) {
                    false
                } else if (moment.isLunar) {
                    val lunar = CalendarHelper.getLunarDate(year, month, day)
                    lunar != null &&
                        lunar.month == moment.month &&
                        lunarDayMatches(moment.day, lunar.day) &&
                        lunar.isLeapMonth == moment.isLeapMonth
                } else {
                    solarMonthDayMatches(year, month, day, moment.month, moment.day)
                }
            }
        }
    }

    /**
     * 打卡后的下一个状态。
     *
     * 修复的两个缺陷：
     * 1. **正数日不再推进日期**（旧实现会 `add(YEAR/...)`，把「已过 100 天」重置成 0）。
     * 2. 一次性事件也在此统一累加 `completionCount`，不再由调用方各自实现。
     */
    fun advance(moment: Moment): Moment {
        val bumped = moment.copy(completionCount = moment.completionCount + 1)

        // 正数日：天数是累计值，推进日期等于抹掉历史，因此只记次数。
        if (moment.kind == MomentKind.COUNT_UP) return bumped

        if (!moment.repeat.isRecurring) return bumped

        return advanceRecurring(bumped)
    }

    // ---------------------------------------------------------------------
    // 起始日解析
    // ---------------------------------------------------------------------

    /**
     * 返回时刻起始日的本地零点毫秒；日期非法时返回 null。
     *
     * 农历走 `l2s`（含闰月回读校验），公历走严格构造，2 月 29 日
     * 在非闰年由 [solarMonthDayMatches] 在「落点判定」阶段降级处理。
     */
    fun startDateMillisOf(moment: Moment): Long? {
        if (moment.isLunar) {
            val solar = CalendarHelper.l2s(
                moment.year, moment.month, moment.day, moment.isLeapMonth
            ) ?: return null
            return CalendarHelper.startOfLocalDay(solar.timeInMillis)
        }
        return localDateMillisOrNull(moment.year, moment.month, moment.day)
    }

    // ---------------------------------------------------------------------
    // 周期推进（仅循环事件）
    // ---------------------------------------------------------------------

    private fun advanceRecurring(moment: Moment): Moment {
        if (moment.isLunar) {
            if (moment.repeat == MomentRepeat.YEARLY) {
                val nextYear = (moment.year + 1..moment.year + LUNAR_YEAR_SEARCH_WINDOW)
                    .firstOrNull {
                        CalendarHelper.l2s(it, moment.month, moment.day, moment.isLeapMonth) != null
                    }
                if (nextYear != null) {
                    return moment.copy(year = nextYear)
                }
                return moment
            }

            val currentSolar = CalendarHelper.l2s(
                moment.year, moment.month, moment.day, moment.isLeapMonth
            ) ?: return moment

            val nextLunar = ChineseCalendar().apply {
                timeInMillis = currentSolar.timeInMillis
                when (moment.repeat) {
                    MomentRepeat.WEEKLY -> add(Calendar.DAY_OF_YEAR, 7)
                    MomentRepeat.MONTHLY -> add(ChineseCalendar.MONTH, 1)
                    else -> Unit
                }
            }
            return moment.copy(
                year = nextLunar.get(ChineseCalendar.YEAR),
                month = nextLunar.get(ChineseCalendar.MONTH) + 1,
                // 用户选的是「三十」时保持 30：下一个农历月可能是小月（只有 29 天），
                // 由 CalendarHelper.l2s 的 convert(day) ?: convert(29) 负责降级，
                // 这样「每月三十」的语义不会因为某个月是小月而永久变成「二十九」。
                day = if (moment.day == 30) 30 else nextLunar.get(ChineseCalendar.DAY_OF_MONTH),
                isLeapMonth = nextLunar.get(ChineseCalendar.IS_LEAP_MONTH) == 1
            )
        }

        val next = localDateMillisOrNull(moment.year, moment.month, moment.day) ?: return moment
        val cal = Calendar.getInstance().apply {
            timeInMillis = next
            when (moment.repeat) {
                MomentRepeat.WEEKLY -> add(Calendar.WEEK_OF_YEAR, 1)
                MomentRepeat.MONTHLY -> add(Calendar.MONTH, 1)
                MomentRepeat.YEARLY -> add(Calendar.YEAR, 1)
                else -> Unit
            }
        }
        return moment.copy(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    // ---------------------------------------------------------------------
    // 天数求解
    // ---------------------------------------------------------------------

    private fun daysUntilSolarMonthly(today: Long, targetDay: Int): Int? {
        val candidate = Calendar.getInstance().apply {
            timeInMillis = today
            set(
                Calendar.DAY_OF_MONTH,
                targetDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH))
            )
        }
        if (candidate.timeInMillis < today) {
            candidate.add(Calendar.MONTH, 1)
            candidate.set(
                Calendar.DAY_OF_MONTH,
                targetDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH))
            )
        }
        return CalendarHelper.localDaysBetween(today, candidate.timeInMillis)
    }

    private fun daysUntilLunarMonthly(today: Long, moment: Moment, original: Long): Int? {
        val cursor = Calendar.getInstance().apply {
            timeInMillis = if (original > today) original else today
        }
        repeat(LUNAR_DAY_SEARCH_WINDOW) {
            val lunar = CalendarHelper.getLunarDate(
                cursor.get(Calendar.YEAR),
                cursor.get(Calendar.MONTH) + 1,
                cursor.get(Calendar.DAY_OF_MONTH)
            )
            if (lunar != null &&
                lunarDayMatches(moment.day, lunar.day) &&
                lunar.isLeapMonth == moment.isLeapMonth &&
                CalendarHelper.localDaysBetween(original, cursor.timeInMillis) >= 0
            ) {
                return CalendarHelper.localDaysBetween(today, cursor.timeInMillis)
            }
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    private fun daysUntilYearly(today: Long, moment: Moment): Int? {
        if (moment.isLunar) {
            val todayLunarYear = lunarYearOf(today) ?: return null
            for (lunarYear in todayLunarYear..todayLunarYear + LUNAR_YEAR_SEARCH_WINDOW) {
                val solar = CalendarHelper.l2s(
                    lunarYear, moment.month, moment.day, moment.isLeapMonth
                ) ?: continue
                val candidate = CalendarHelper.startOfLocalDay(solar.timeInMillis)
                if (candidate >= today) {
                    return CalendarHelper.localDaysBetween(today, candidate)
                }
            }
            return null
        }

        val todayCal = Calendar.getInstance().apply { timeInMillis = today }
        val currentYear = todayCal.get(Calendar.YEAR)

        val thisYear = solarAnnualDate(currentYear, moment.month, moment.day) ?: return null
        val target = if (thisYear >= today) {
            thisYear
        } else {
            solarAnnualDate(currentYear + 1, moment.month, moment.day) ?: return null
        }
        return CalendarHelper.localDaysBetween(today, target)
    }

    // ---------------------------------------------------------------------
    // 日期工具
    // ---------------------------------------------------------------------

    /** 严格构造本地日期零点；非法日期（如 2 月 30 日）返回 null。 */
    private fun localDateMillisOrNull(year: Int, month: Int, day: Int): Long? {
        if (month !in 1..12 || day !in 1..31) return null
        return try {
            val cal = Calendar.getInstance()
            cal.clear()
            cal.isLenient = false
            cal.set(year, month - 1, day, 0, 0, 0)
            cal.get(Calendar.DAY_OF_MONTH)
            cal.timeInMillis
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * 年度的公历落点。2 月 29 日在非闰年降级到 2 月 28 日，
     * 与「周年纪念日」的通行处理一致。
     */
    private fun solarAnnualDate(year: Int, month: Int, day: Int): Long? {
        localDateMillisOrNull(year, month, day)?.let { return it }
        if (month == 2 && day == 29) {
            return localDateMillisOrNull(year, 2, 28)
        }
        return null
    }

    /** 判断某公历日期是否命中「每年 M 月 D 日」，含 2 月 29 日降级规则。 */
    private fun solarMonthDayMatches(
        year: Int, month: Int, day: Int, targetMonth: Int, targetDay: Int
    ): Boolean {
        if (month != targetMonth) return false
        if (day == targetDay) return true
        if (targetMonth != 2 || targetDay != 29 || day != 28) return false
        val maxDay = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
        return maxDay == 28
    }

    /** 农历 30 日在小月降级为 29 日。 */
    private fun lunarDayMatches(requestedDay: Int, actualDay: Int): Boolean =
        actualDay == requestedDay || (requestedDay == 30 && actualDay == 29)

    private fun lunarYearOf(millis: Long): Int? {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return CalendarHelper.getLunarDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )?.year
    }

    private fun dayOfWeekOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)
}
