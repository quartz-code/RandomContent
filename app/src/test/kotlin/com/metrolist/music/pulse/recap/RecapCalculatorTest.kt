package com.metrolist.music.pulse.recap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RecapCalculatorTest {
    private fun play(day: Int, hour: Int, minutes: Long, month: Int = 10) =
        Play(LocalDateTime.of(2026, month, day, hour, 0), minutes * 60_000)

    @Test
    fun monthNumbersCountOnlyThatMonth() {
        val plays = listOf(
            play(1, 8, 10),
            play(2, 20, 30),
            play(3, 21, 40),
            play(3, 2, 5),
            play(10, 13, 15),
            play(30, 23, 1, month = 9),
            play(1, 0, 50, month = 11),
        )
        val n = RecapCalculator.numbers(plays, RecapPeriod(2026, 10))
        assertEquals(100, n.minutes)
        assertEquals(5, n.plays)
        assertEquals(4, n.activeDays)
        assertEquals(3, n.longestStreak)
        assertEquals(PartOfDay.EVENING, n.favouritePart)
        assertEquals(listOf(10L, 15L, 70L, 5L).map { it * 60_000 }, n.byPartOfDay)
        assertEquals(LocalDate.of(2026, 10, 3), n.busiestDay)
        assertEquals(emptyList<Long>(), n.byMonth)
    }

    @Test
    fun yearSplitsByMonth() {
        val plays = listOf(play(5, 12, 10, month = 3), play(6, 12, 20, month = 3), play(1, 12, 5, month = 12))
        val n = RecapCalculator.numbers(plays, RecapPeriod(2026))
        assertEquals(12, n.byMonth.size)
        assertEquals(30L * 60_000, n.byMonth[2])
        assertEquals(3, n.busiestMonth)
    }

    @Test
    fun emptyPeriodHasNoFavourites() {
        val n = RecapCalculator.numbers(emptyList(), RecapPeriod(2026, 1))
        assertEquals(0, n.minutes)
        assertNull(n.favouritePart)
        assertNull(n.busiestDay)
        assertEquals(0, n.longestStreak)
    }

    @Test
    fun periodsParseAndStepBack() {
        assertEquals(RecapPeriod(2026, 9), RecapPeriod.parse("2026-09"))
        assertEquals(RecapPeriod(2026), RecapPeriod.parse("2026"))
        assertNull(RecapPeriod.parse("2026-13"))
        assertNull(RecapPeriod.parse("сентябрь"))
        assertEquals(RecapPeriod(2025, 12), RecapPeriod(2026, 1).previous())
        assertEquals("2026-01", RecapPeriod(2026, 1).key)
    }

    @Test
    fun offerIsLastYearInJanuaryThenLastMonthAndOnlyOnce() {
        val january = LocalDateTime.of(2027, 1, 3, 10, 0)
        val lots: (RecapPeriod) -> Long = { 600 }
        assertEquals(RecapPeriod(2026), RecapCalculator.toOffer(january, null, lots))
        assertEquals(RecapPeriod(2026, 12), RecapCalculator.toOffer(january, "2026", lots))
        assertNull(RecapCalculator.toOffer(january, "2026,2026-12", lots))
        val november = LocalDateTime.of(2026, 11, 1, 9, 0)
        assertEquals(RecapPeriod(2026, 10), RecapCalculator.toOffer(november, "2026-09", lots))
        assertNull(RecapCalculator.toOffer(november, null) { 10 })
    }

    @Test
    fun recapListPutsYearsFirstAndSkipsQuietMonths() {
        val hour = 3_600_000L
        val list = listRecaps(
            listOf(
                RecapEntry(RecapPeriod(2026, 10), 5 * hour),
                RecapEntry(RecapPeriod(2026, 9), 60_000),
                RecapEntry(RecapPeriod(2025, 12), 2 * hour),
            ),
        )
        assertEquals(listOf("2026", "2026-10", "2025", "2025-12"), list.map { it.period.key })
    }
}
