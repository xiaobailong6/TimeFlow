package com.lifeordered

import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Moment
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UnitVerification {

    @Test
    fun `solar leap day and same day countdown are exact`() {
        val reference = localDate(2024, 2, 28)
        val leapDay = Moment(
            title = "leap day",
            year = 2024,
            month = 2,
            day = 29,
            isYearly = false
        )

        assertEquals(1, CalendarHelper.calculateDays(leapDay, reference))
        assertEquals(0, CalendarHelper.calculateDays(leapDay, localDate(2024, 2, 29)))
        assertTrue(
            CalendarHelper.isMomentOnDate(
                leapDay.copy(isYearly = true, repeatType = "yearly"),
                2025,
                2,
                28
            )
        )

    }
    @Test
    fun `lunar new year converts to known solar date`() {
        val lunarNewYear = CalendarHelper.l2s(2024, 1, 1)
        assertNotNull(lunarNewYear)
        assertEquals(Calendar.FEBRUARY, lunarNewYear!!.get(Calendar.MONTH))
        assertEquals(10, lunarNewYear!!.get(Calendar.DAY_OF_MONTH))
        assertEquals(2024, CalendarHelper.getLunarDate(2024, 2, 10)?.year)
        assertEquals(1, CalendarHelper.getLunarDate(2024, 2, 10)?.month)
        assertEquals(1, CalendarHelper.getLunarDate(2024, 2, 10)?.day)
        assertFalse(CalendarHelper.getLunarDate(2024, 2, 10)?.isLeapMonth ?: true)
    }
    @Test
    fun `lunar date resolves to the requested Gregorian year`() {
        val solar = CalendarHelper.solarDateInGregorianYear(
            lunarMonth = 1,
            lunarDay = 1,
            gregorianYear = 2024
        )

        assertNotNull(solar)
        assertEquals(2024, solar!!.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, solar!!.get(Calendar.MONTH))
        assertEquals(10, solar!!.get(Calendar.DAY_OF_MONTH))
    }


    @Test
    fun `regular and leap lunar months remain distinct`() {
        val regular = CalendarHelper.l2s(2023, 2, 1, false)
        val leap = CalendarHelper.l2s(2023, 2, 1, true)
        assertNotNull(regular)
        assertNotNull(leap)
        assertTrue(regular!!.timeInMillis != leap!!.timeInMillis)

        val regularDate = CalendarHelper.getLunarDate(
            regular.get(Calendar.YEAR), regular.get(Calendar.MONTH) + 1, regular.get(Calendar.DAY_OF_MONTH)
        )
        val leapDate = CalendarHelper.getLunarDate(
            leap.get(Calendar.YEAR), leap.get(Calendar.MONTH) + 1, leap.get(Calendar.DAY_OF_MONTH)
        )
        assertFalse(regularDate?.isLeapMonth ?: true)
        assertTrue(leapDate?.isLeapMonth ?: false)

        // 注意：Moment 的第一个参数是 id（@PrimaryKey），title 是第二个。
        // 这里必须用具名参数，位置参数会把 "regular" 塞给 id、把 2023 塞给 title。
        val regularMoment = Moment(
            title = "regular",
            year = 2023,
            month = 2,
            day = 1,
            isLunar = true,
            repeatType = "yearly"
        )
        val leapMoment = regularMoment.copy(isLeapMonth = true)
        assertFalse(CalendarHelper.isMomentOnDate(regularMoment, leap.get(Calendar.YEAR), leap.get(Calendar.MONTH) + 1, leap.get(Calendar.DAY_OF_MONTH)))
        assertTrue(CalendarHelper.isMomentOnDate(leapMoment, leap.get(Calendar.YEAR), leap.get(Calendar.MONTH) + 1, leap.get(Calendar.DAY_OF_MONTH)))
    }

    @Test
    fun `leap lunar annual recurrence skips years without the same leap month`() {
        val moment = Moment(
            title = "leap",
            year = 2025,
            month = 6,
            day = 1,
            isLunar = true,
            isLeapMonth = true,
            repeatType = "yearly"
        )
        val next = CalendarHelper.advanceRecurringMoment(moment)

        // 契约：推进到「下一个真正存在同一个闰月」的农历年，而不是简单地 +1 年。
        //
        // 原先这里写死 assertEquals(2028, next.year)，但 2028 年是否有闰六月
        // 取决于 ICU 的农历数据；写死具体年份会让测试要么恒真要么恒假，
        // 且与测试名所表达的意图（跳过不含该闰月的年份）无关。
        // 改为断言契约本身，既验证跳过行为，也不依赖某个硬编码年份。
        assertTrue("必须向后推进至少一年", next.year > moment.year)
        assertEquals(moment.month, next.month)
        assertTrue("推进后必须仍落在闰月", next.isLeapMonth)

        // 目标年份必须真实存在该闰月（回读校验）
        assertNotNull(
            "目标年份 ${next.year} 必须存在闰${next.month}月",
            CalendarHelper.l2s(next.year, next.month, next.day, isLeapMonth = true)
        )

        // 且中间不存在更早的候选年份——这才是「跳过」的真正含义
        for (year in moment.year + 1 until next.year) {
            assertNull(
                "年份 $year 不应存在闰${moment.month}月",
                CalendarHelper.l2s(year, moment.month, moment.day, isLeapMonth = true)
            )
        }
    }

    @Test
    fun `lunar annual countdown uses lunar year before spring festival`() {
        val lunarBirthday = Moment(
            title = "lunar birthday",
            year = 2024,
            month = 1,
            day = 1,
            isLunar = true,
            isYearly = true,
            repeatType = "yearly"
        )

        // 2024-01-01 is still lunar 2023, so the next lunar New Year is 2024-02-10.
        assertEquals(40, CalendarHelper.calculateDays(lunarBirthday, localDate(2024, 1, 1)))
    }

    @Test
    fun `lunar monthly countdown searches lunar dates rather than solar month dates`() {
        val lunarFifteenth = Moment(
            title = "lunar monthly",
            year = 2024,
            month = 1,
            day = 15,
            isLunar = true,
            isYearly = false,
            repeatType = "monthly"
        )

        // Lunar 2024-01-15 is solar 2024-02-24; from lunar New Year it is 14 days away.
        assertEquals(14, CalendarHelper.calculateDays(lunarFifteenth, localDate(2024, 2, 10)))
    }

    @Test
    fun `local date difference is not based on elapsed 24 hour blocks`() {
        val start = localDate(2024, 3, 9, 23, 30)
        val nextDate = localDate(2024, 3, 10, 0, 15)
        assertEquals(1, CalendarHelper.localDaysBetween(start, nextDate))

        val consumable = Consumable(
            name = "test",
            totalDays = 7,
            startDateMillis = start
        )
        assertTrue(consumable.getRemainingDays(nextDate) < consumable.totalDays)
    }

    private fun localDate(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis
}
