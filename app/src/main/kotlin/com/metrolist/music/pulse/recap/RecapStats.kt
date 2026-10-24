package com.metrolist.music.pulse.recap

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** A month ("2026-09") or a whole year ("2026") of listening. */
data class RecapPeriod(val year: Int, val month: Int? = null) {
    val isYear: Boolean get() = month == null

    val start: LocalDateTime get() = if (month == null) LocalDate.of(year, 1, 1).atStartOfDay() else LocalDate.of(year, month, 1).atStartOfDay()

    /** Exclusive. */
    val end: LocalDateTime get() = if (month == null) start.plusYears(1) else start.plusMonths(1)

    val key: String get() = if (month == null) "$year" else "%04d-%02d".format(year, month)

    /** The month or year just before this one, of the same kind. */
    fun previous(): RecapPeriod =
        if (month == null) RecapPeriod(year - 1) else YearMonth.of(year, month).minusMonths(1).let { RecapPeriod(it.year, it.monthValue) }

    companion object {
        fun parse(key: String?): RecapPeriod? {
            val parts = key?.split("-") ?: return null
            val year = parts[0].toIntOrNull()?.takeIf { it in 2000..2100 } ?: return null
            if (parts.size == 1) return RecapPeriod(year)
            val month = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..12 } ?: return null
            return if (parts.size == 2) RecapPeriod(year, month) else null
        }

        fun monthOf(time: LocalDateTime) = RecapPeriod(time.year, time.monthValue)
    }
}

/** One play from the history: when it started and how long it was heard. */
data class Play(val time: LocalDateTime, val ms: Long)

enum class PartOfDay { MORNING, DAY, EVENING, NIGHT }

data class RecapNumbers(
    val totalMs: Long,
    val plays: Int,
    /** Days with any music in the period. */
    val activeDays: Int,
    /** Most days in a row with music. */
    val longestStreak: Int,
    /** Listening time by part of day, in [PartOfDay] order. */
    val byPartOfDay: List<Long>,
    val busiestDay: LocalDate?,
    val busiestDayMs: Long,
    /** Twelve months for a year, empty for a month. */
    val byMonth: List<Long>,
) {
    val minutes: Long get() = totalMs / 60_000
    val favouritePart: PartOfDay? get() = byPartOfDay.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.let { PartOfDay.entries[it.index] }
    val busiestMonth: Int? get() = byMonth.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.let { it.index + 1 }
}

/**
 * Pulse: the numbers behind a recap, from plain plays so they are unit-tested. Times are the
 * phone's wall-clock time, as the history stores them. The desktop build compiles this file too.
 */
object RecapCalculator {
    /** Below this a recap would be a few lonely numbers, so it is not offered. */
    const val MIN_MINUTES = 30L

    fun partOf(time: LocalDateTime): PartOfDay = when (time.hour) {
        in 5..10 -> PartOfDay.MORNING
        in 11..16 -> PartOfDay.DAY
        in 17..22 -> PartOfDay.EVENING
        else -> PartOfDay.NIGHT
    }

    fun numbers(plays: List<Play>, period: RecapPeriod): RecapNumbers {
        val inPeriod = plays.filter { it.time >= period.start && it.time < period.end && it.ms > 0 }
        val byDay = inPeriod.groupBy { it.time.toLocalDate() }.mapValues { (_, list) -> list.sumOf { it.ms } }
        val busiest = byDay.maxByOrNull { it.value }
        val parts = LongArray(PartOfDay.entries.size)
        inPeriod.forEach { parts[partOf(it.time).ordinal] += it.ms }
        val months = if (period.isYear) {
            LongArray(12).also { m -> inPeriod.forEach { m[it.time.monthValue - 1] += it.ms } }.toList()
        } else {
            emptyList()
        }
        return RecapNumbers(
            totalMs = inPeriod.sumOf { it.ms },
            plays = inPeriod.size,
            activeDays = byDay.size,
            longestStreak = longestStreak(byDay.keys),
            byPartOfDay = parts.toList(),
            busiestDay = busiest?.key,
            busiestDayMs = busiest?.value ?: 0L,
            byMonth = months,
        )
    }

    fun longestStreak(days: Collection<LocalDate>): Int {
        var best = 0
        var run = 0
        var last: LocalDate? = null
        for (day in days.toSortedSet()) {
            run = if (last != null && last.plusDays(1) == day) run + 1 else 1
            best = maxOf(best, run)
            last = day
        }
        return best
    }

    /**
     * The recap to offer on opening the app: last year's in January, otherwise last month's,
     * unless it was already offered or there was too little music.
     */
    fun toOffer(now: LocalDateTime, lastOffered: String?, minutesIn: (RecapPeriod) -> Long): RecapPeriod? {
        val offered = lastOffered?.split(",").orEmpty().toSet()
        return candidates(now).firstOrNull { it.key !in offered && minutesIn(it) >= MIN_MINUTES }
    }

    fun candidates(now: LocalDateTime): List<RecapPeriod> = buildList {
        if (now.monthValue == 1) add(RecapPeriod(now.year - 1))
        add(RecapPeriod.monthOf(now).previous())
    }
}

/** A month or year with enough music for a recap, for the list of recaps. */
data class RecapEntry(val period: RecapPeriod, val totalMs: Long)

/**
 * Groups monthly totals into the list shown to the user (pure, so it is tested). Here rather than
 * next to the database queries, so Pulse on a computer lists its own recaps the same way.
 */
fun listRecaps(months: List<RecapEntry>): List<RecapEntry> {
    val enough = months.filter { it.totalMs / 60_000 >= RecapCalculator.MIN_MINUTES }
    return months.groupBy { it.period.year }.toSortedMap(compareByDescending { it }).flatMap { (year, inYear) ->
        val total = inYear.sumOf { it.totalMs }
        val yearEntry = RecapEntry(RecapPeriod(year), total).takeIf { total / 60_000 >= RecapCalculator.MIN_MINUTES }
        listOfNotNull(yearEntry) + enough.filter { it.period.year == year }.sortedByDescending { it.period.month }
    }
}

/** Room rows for the recap queries in DatabaseDao. */
data class RecapPlayRow(val timestamp: LocalDateTime, val playTime: Long)

data class RecapMonthRow(val month: String, val totalMs: Long)
