package com.lifeordered

import com.lifeordered.core.utils.CalendarHelper
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Moment
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

        val regularMoment = Moment("regular", 2023, 2, 1, isLunar = true, repeatType = "yearly")
        val leapMoment = regularMoment.copy(isLeapMonth = true)
        assertFalse(CalendarHelper.isMomentOnDate(regularMoment, leap.get(Calendar.YEAR), leap.get(Calendar.MONTH) + 1, leap.get(Calendar.DAY_OF_MONTH)))
        assertTrue(CalendarHelper.isMomentOnDate(leapMoment, leap.get(Calendar.YEAR), leap.get(Calendar.MONTH) + 1, leap.get(Calendar.DAY_OF_MONTH)))
    }

    @Test
    fun `leap lunar annual recurrence skips years without the same leap month`() {
        val moment = Moment("leap", 2025, 6, 1, isLunar = true, isLeapMonth = true, repeatType = "yearly")
        val next = CalendarHelper.advanceRecurringMoment(moment)
        assertEquals(2028, next.year)
        assertEquals(6, next.month)
        assertTrue(next.isLeapMonth)
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
