package org.brightchain.brightdate.alarm

/**
 * Leap second table and TAI-UTC offset lookup.
 *
 * Table is current as of 2026 (last leap second: 2017-01-01, offset = 37 s).
 * Each entry: Unix timestamp (UTC seconds) at which the new offset takes effect.
 *
 * Source: IERS Bulletin C / IANA leap-seconds.list
 */
object LeapSeconds {

    private val table = arrayOf(
        63072000L  to 10,  // 1972-01-01
        78796800L  to 11,  // 1972-07-01
        94694400L  to 12,  // 1973-01-01
        126230400L to 13,  // 1974-01-01
        157766400L to 14,  // 1975-01-01
        189302400L to 15,  // 1976-01-01
        220924800L to 16,  // 1977-01-01
        252460800L to 17,  // 1978-01-01
        283996800L to 18,  // 1979-01-01
        315532800L to 19,  // 1980-01-01
        362793600L to 20,  // 1981-07-01
        394329600L to 21,  // 1982-07-01
        425865600L to 22,  // 1983-07-01
        489024000L to 23,  // 1985-07-01
        567993600L to 24,  // 1988-01-01
        631152000L to 25,  // 1990-01-01
        662688000L to 26,  // 1991-01-01
        709948800L to 27,  // 1992-07-01
        741484800L to 28,  // 1993-07-01
        773020800L to 29,  // 1994-07-01
        820454400L to 30,  // 1996-01-01
        867715200L to 31,  // 1997-07-01
        915148800L to 32,  // 1999-01-01
        1136073600L to 33, // 2006-01-01
        1230768000L to 34, // 2009-01-01
        1341100800L to 35, // 2012-07-01
        1435708800L to 36, // 2015-07-01
        1483228800L to 37  // 2017-01-01
    )

    /** TAI-UTC offset (seconds) for the given UTC Unix timestamp (seconds). */
    fun taiMinusUtc(utcUnixSeconds: Long): Int {
        var offset = 10
        for ((ts, off) in table) {
            if (utcUnixSeconds >= ts) offset = off else break
        }
        return offset
    }

    /** TAI-UTC offset for the given UTC Unix millisecond timestamp. */
    fun taiMinusUtcFromMs(utcUnixMs: Long): Int = taiMinusUtc(utcUnixMs / 1000L)
}
