package com.lifeordered.core.date

import android.icu.util.ChineseCalendar
import java.util.Calendar

/**
 * 农历「月份」条目。
 *
 * @param month 1..12，闰月与被闰的月份同号
 * @param isLeapMonth 是否闰月
 * @param days 该月天数（29 或 30）
 * @param startMillis 该月初一所在**本地日**的零点毫秒
 */
data class LunarMonth(
    val month: Int,
    val isLeapMonth: Boolean,
    val days: Int,
    val startMillis: Long
)

/**
 * 一个完整农历年的月份表。
 *
 * @param year 农历年标号，定义为**该年正月初一所在的公历年**
 * @param newYearMillis 正月初一的本地零点毫秒
 * @param months 按时间顺序排列的月份（闰月插在被闰月之后）
 */
data class LunarYear(
    val year: Int,
    val newYearMillis: Long,
    val months: List<LunarMonth>
) {
    /** 查找指定月份；闰月不存在时返回 null（调用方据此判定「该农历日期不存在」）。 */
    fun find(month: Int, isLeapMonth: Boolean = false): LunarMonth? =
        months.firstOrNull { it.month == month && it.isLeapMonth == isLeapMonth }
}

/**
 * 农历年表：把 ICU 的「逐日查询」预先折叠成「月份 → 天数」的静态表。
 *
 * ## 为什么必须重写（旧实现的 bug 根因）
 *
 * 旧 `CalendarHelper.l2s` 直接这样构造：
 *
 * ```
 * ChineseCalendar().apply { set(YEAR, 2024); set(MONTH, 0); set(DAY_OF_MONTH, 1) }
 * ```
 *
 * 但 `android.icu.util.ChineseCalendar` 的 `YEAR` **不是公历年**，它是
 * 「干支纪年的连续年号」，基准常量 `CHINESE_EPOCH_YEAR = -2636`，
 * 换算关系是 `YEAR ≈ 公历年 + 2637`。
 *
 * 于是 `set(YEAR, 2024)` 实际请求的是**公元前 613 年**的农历，
 * 回读校验（`lunar.get(YEAR) != year`）必然失败，`l2s` 返回 null，
 * 界面就弹出「该日期在农历中不存在」。任何公历年份都会触发，
 * 所以用户看到的是「不管怎么选都提示不存在」。
 *
 * ## 这一版的做法：完全不碰 YEAR 字段
 *
 * 1. 用**公历**游标从 `gy/1/15` 扫到 `gy/2/22`，找第一个「月=1、日=1、非闰」的
 *    公历日 —— 这就是该公历年的正月初一（春节落在 1/21 ~ 2/20 之间，45 天窗口足够）。
 * 2. 从 `正月初一(label)` 逐日走到 `正月初一(label + 1)`，读 `MONTH / DAY_OF_MONTH /
 *    IS_LEAP_MONTH` 三个**可靠**字段，把「连续同月同闰」的天数累计成月份表。
 * 3. 公历 → 农历时，先定位它属于哪个农历年（`labelOf`），再在表里二分/线性定位月份，
 *    用「该月起始日 + (日 - 1)」直接算出公历日 —— 全程不需要反查 YEAR。
 *
 * ## 年标号的定义
 *
 * 以**正月初一所在的公历年**为标号。这与用户口语一致（「2024 年的农历生日」＝
 * 2024 年春节之后的那个农历年），也满足 `UnitVerification` 中
 * `getLunarDate(2024, 2, 10)?.year == 2024` 的断言。
 *
 * ## 缓存
 *
 * 构建一年约 354 次 ICU 查询，对滚动轮盘来说太慢，因此按年缓存（最近 6 年）。
 * 所有访问都在同一把锁内，因为轮盘滚动可能在主线程之外被 Compose 的快照读取。
 */
object LunarTable {

    /** 支持范围。ICU 的天文数据本身覆盖更宽，这里收敛是为了让轮盘可选范围可预期。 */
    const val MIN_YEAR = 1900
    const val MAX_YEAR = 2100

    private const val CACHE_CAPACITY = 6

    /** 春节一定落在 1/21 ~ 2/20 之间，从 1/15 开始扫 45 天是安全窗口。 */
    private const val NEW_YEAR_SCAN_START_DAY = 15
    private const val NEW_YEAR_SCAN_DAYS = 45

    private val lock = Any()

    /** 插入序 LinkedHashMap，容量超限时手工淘汰最早插入的一项。 */
    private val yearCache = LinkedHashMap<Int, LunarYear>()

    private val newYearCache = HashMap<Int, Long>()

    // ---------------------------------------------------------------------
    // 对外接口
    // ---------------------------------------------------------------------

    /** 取某个农历年的月份表；超出 [MIN_YEAR]..[MAX_YEAR] 时返回 null。 */
    fun of(year: Int): LunarYear? {
        if (year < MIN_YEAR || year > MAX_YEAR) return null
        synchronized(lock) {
            yearCache[year]?.let { return it }
            val built = build(year)
            if (yearCache.size >= CACHE_CAPACITY) {
                yearCache.keys.firstOrNull()?.let { yearCache.remove(it) }
            }
            yearCache[year] = built
            return built
        }
    }

    /** 某个农历年的月份列表（含闰月）；超出范围时返回空列表。 */
    fun monthsOf(year: Int): List<LunarMonth> = of(year)?.months ?: emptyList()

    /** 指定公历年的正月初一（本地零点毫秒）。 */
    fun chineseNewYear(gregorianYear: Int): Long {
        synchronized(lock) {
            newYearCache[gregorianYear]?.let { return it }
            val found = scanChineseNewYear(gregorianYear)
            newYearCache[gregorianYear] = found
            return found
        }
    }

    /**
     * 某个公历时刻属于哪个农历年（按「正月初一所在公历年」标号）。
     *
     * 元旦到春节之间属于上一农历年，所以必须拿正月初一比一次。
     */
    fun labelOf(solarMillis: Long): Int {
        val gregorianYear = Calendar.getInstance().apply { timeInMillis = solarMillis }
            .get(Calendar.YEAR)
        val dayStart = startOfLocalDay(solarMillis)
        val label = if (dayStart < chineseNewYear(gregorianYear)) gregorianYear - 1 else gregorianYear
        return label.coerceIn(MIN_YEAR, MAX_YEAR)
    }

    // ---------------------------------------------------------------------
    // 内部实现
    // ---------------------------------------------------------------------

    /**
     * 扫描某个公历年的春节。
     *
     * 用**公历**游标逐日推进，只在每一天读一次农历的 (MONTH, DAY, LEAP)，
     * 全程不写 YEAR —— 这是旧实现出错的唯一入口，必须绕开。
     */
    private fun scanChineseNewYear(gregorianYear: Int): Long {
        val probe = ChineseCalendar()
        val cursor = Calendar.getInstance().apply {
            clear()
            // 用中午 12 点做游标：某些时区在 00:00 有夏令时跳变，正午永远存在
            set(gregorianYear, Calendar.JANUARY, NEW_YEAR_SCAN_START_DAY, 12, 0, 0)
        }
        repeat(NEW_YEAR_SCAN_DAYS) {
            probe.timeInMillis = cursor.timeInMillis
            val isNewYear = probe.get(ChineseCalendar.MONTH) == 0 &&
                probe.get(ChineseCalendar.DAY_OF_MONTH) == 1 &&
                probe.get(ChineseCalendar.IS_LEAP_MONTH) == 0
            if (isNewYear) {
                return startOfLocalDay(cursor.timeInMillis)
            }
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        // 理论上不可达（春节必在窗口内）。兜底取 2 月 1 日，保证不返回 0 造成空指针连锁。
        return startOfLocalDay(
            Calendar.getInstance().apply {
                clear()
                set(gregorianYear, Calendar.FEBRUARY, 1, 12, 0, 0)
            }.timeInMillis
        )
    }

    /**
     * 从正月初一走到下一年正月初一，把每个月折叠成一条 [LunarMonth]。
     *
     * 用「探测第 30 天是否还属于本月」定月长，而不是逐日累加：
     * 农历月长只有 29 / 30 两种，逐日走除了慢没有额外信息。
     */
    private fun build(label: Int): LunarYear {
        val startMillis = chineseNewYear(label)
        val endMillis = chineseNewYear(label + 1)

        val probe = ChineseCalendar()
        val months = ArrayList<LunarMonth>(13)
        var monthStart = startMillis

        // 逐月推进，而不是逐日推进。
        //
        // 农历月长只有 29 / 30 两种，所以「本月第 30 天是否还属于本月」
        // 一次探测就能定出月长：是 → 30 天，否 → 29 天。
        // 整年因此只要约 13 次 ICU 查询，而逐日走要约 354 次。
        //
        // 这不是微优化：轮盘每吸附到一年就会重建一次表，而重建发生在
        // 组合（主线程）里，逐日版的数百次天文计算会直接让滑动掉帧。
        while (monthStart < endMillis) {
            probe.timeInMillis = noonOf(monthStart)
            val month = probe.get(ChineseCalendar.MONTH) + 1
            val leap = probe.get(ChineseCalendar.IS_LEAP_MONTH) == 1

            // 同一个月号 + 同闰标记才说明还停在本月。
            // 闰月虽然与被闰月同号，但 leap 不同，所以不会被误判成同一个月。
            probe.timeInMillis = noonAfter(monthStart, 29)
            val stillSameMonth = probe.get(ChineseCalendar.MONTH) + 1 == month &&
                (probe.get(ChineseCalendar.IS_LEAP_MONTH) == 1) == leap
            val days = if (stillSameMonth) 30 else 29

            months.add(LunarMonth(month, leap, days, monthStart))
            monthStart = startOfLocalDay(noonAfter(monthStart, days))
        }

        return LunarYear(year = label, newYearMillis = startMillis, months = months)
    }

    /**
     * 指定本地日往后 [days] 天的**正午**时刻。
     *
     * 一律用正午做游标：有些时区的 00:00 会碰上夏令时跳变（那天可能没有 00:30），
     * 正午则永远存在。`add(DAY_OF_YEAR, ...)` 而不是加毫秒，同样是为了绕开
     * 夏令时导致「一天不是 24 小时」。
     */
    private fun noonAfter(dayMillis: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = dayMillis
        set(Calendar.HOUR_OF_DAY, 12)
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

    /** 本地零点所在的当日正午。 */
    private fun noonOf(dayMillis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = dayMillis
        set(Calendar.HOUR_OF_DAY, 12)
    }.timeInMillis

    /** 本地日零点。本文件自带一份，避免与 `CalendarHelper` 互相 import。 */
    private fun startOfLocalDay(timeMillis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
