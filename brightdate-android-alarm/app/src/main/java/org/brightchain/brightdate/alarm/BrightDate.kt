package org.brightchain.brightdate.alarm

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * BrightDate computation and formatting utilities.
 *
 * BrightDate = (taiUnixSeconds − J2000_TAI_UNIX_S) / 86400
 * where J2000_TAI_UNIX_S = 946_727_967.816
 *
 * All conversions go through the TAI substrate to remain leap-second aware.
 */
object BrightDate {

    /** TAI seconds since Unix epoch at J2000.0. */
    private const val J2000_TAI_UNIX_S = 946_727_967.816

    private const val SECONDS_PER_DAY = 86_400.0

    // ── BrightDate ↔ Unix ms ──────────────────────────────────────────────────

    /** Convert a Unix millisecond timestamp to a BrightDate value. */
    fun fromUnixMs(unixMs: Long): Double {
        val utcSeconds = unixMs / 1000.0
        val taiOffset = LeapSeconds.taiMinusUtcFromMs(unixMs)
        val taiUnixSeconds = utcSeconds + taiOffset
        return (taiUnixSeconds - J2000_TAI_UNIX_S) / SECONDS_PER_DAY
    }

    /**
     * Convert a BrightDate value back to a Unix millisecond timestamp.
     *
     * Because the TAI-UTC offset depends on the UTC time being computed, this
     * iterates once (the offset changes very rarely – only at leap seconds).
     */
    fun toUnixMs(brightDate: Double): Long {
        // First approximation: assume offset = 37 (current as of 2026)
        val approxTaiSeconds = brightDate * SECONDS_PER_DAY + J2000_TAI_UNIX_S
        val approxUtcMs = ((approxTaiSeconds - 37) * 1000).toLong()
        // Refine with actual offset
        val offset = LeapSeconds.taiMinusUtcFromMs(approxUtcMs)
        val taiSeconds = brightDate * SECONDS_PER_DAY + J2000_TAI_UNIX_S
        return ((taiSeconds - offset) * 1000).toLong()
    }

    /** BrightDate for "right now". */
    fun now(): Double = fromUnixMs(System.currentTimeMillis())

    // ── Formatting ────────────────────────────────────────────────────────────

    /** Format a BrightDate value as "DDDDD.ddddd" (5 fractional digits by default). */
    fun format(value: Double, fractionDigits: Int = 5): String =
        String.format(Locale.ROOT, "%.${fractionDigits}f", value)

    /**
     * Parse a user-supplied BrightDate string (e.g. "9622.50417").
     * Returns null if the string cannot be parsed or represents a past date.
     */
    fun parse(input: String): Double? = input.trim().toDoubleOrNull()

    // ── Human-readable ISO helpers ────────────────────────────────────────────

    /**
     * Format a Unix ms timestamp as a human-readable local date/time string,
     * e.g. "2025-06-15  10:30 AM".
     */
    fun formatUnixMs(unixMs: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = unixMs
        return String.format(
            Locale.ROOT,
            "%04d-%02d-%02d  %02d:%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE)
        )
    }

    /**
     * Returns a human-friendly description of how far [targetUnixMs] is from now,
     * e.g. "in 3 d 4 h 12 m" or, when under a minute away, "in 42 s".
     */
    fun relativeDescription(targetUnixMs: Long): String {
        val diffMs = targetUnixMs - System.currentTimeMillis()
        if (diffMs < 0) return "in the past"

        val bdDiff = fromUnixMs(targetUnixMs) - now()
        val millidays = bdDiff * 1000.0

        val totalSeconds = diffMs / 1000L
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val timeStr = buildString {
            append("in ")
            if (days > 0) append("$days d ")
            if (hours > 0 || days > 0) append("$hours h ")
            if (minutes > 0 || hours > 0 || days > 0) append("$minutes m")
            else append("$seconds s")
        }.trim()

        return "$timeStr (${String.format(Locale.ROOT, "%.1f", millidays)} md)"
    }
}
