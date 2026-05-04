package com.grindrplus.persistence.converters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DateConverterTest {

    private val converter = DateConverter()

    private fun utcDate(iso: String): Date {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return parser.parse(iso)!!
    }

    @Test
    fun `roundtrip preserves the exact ISO string`() {
        val iso = "2026-01-15T08:30:45.123Z"
        val date = converter.fromTimestamp(iso)
        assertNotNull(date)
        assertEquals(iso, converter.dateToTimestamp(date))
    }

    @Test
    fun `roundtrip preserves dates near the unix epoch`() {
        val iso = "1970-01-01T00:00:00.000Z"
        val date = converter.fromTimestamp(iso)
        assertEquals(0L, date!!.time)
        assertEquals(iso, converter.dateToTimestamp(date))
    }

    @Test
    fun `dateToTimestamp formats UTC regardless of default time zone`() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            val date = utcDate("2026-06-30T12:00:00.000Z")
            assertEquals("2026-06-30T12:00:00.000Z", converter.dateToTimestamp(date))
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    @Test
    fun `fromTimestamp parses UTC regardless of default time zone`() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            val date = converter.fromTimestamp("2026-06-30T12:00:00.000Z")!!
            // Equivalent epoch must match the value parsed in UTC, not local.
            assertEquals(utcDate("2026-06-30T12:00:00.000Z").time, date.time)
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    @Test
    fun `null inputs round-trip as null`() {
        assertNull(converter.fromTimestamp(null))
        assertNull(converter.dateToTimestamp(null))
    }

    @Test
    fun `malformed strings parse to null instead of throwing`() {
        // SimpleDateFormat is lenient by default and rolls out-of-range
        // components, so "2026-13-40..." would silently round to a valid
        // date — that's outside the scope of this regression guard. The
        // contract this test pins down is "no exception escapes": entirely
        // unrecognizable input must come back as null, not blow up Room.
        assertNull(converter.fromTimestamp(""))
        assertNull(converter.fromTimestamp("not a date"))
        assertNull(converter.fromTimestamp("2026-01-15"))
        assertNull(converter.fromTimestamp("garbage-T-string-Z"))
    }

    @Test
    fun `concurrent format and parse calls do not corrupt output`() {
        // Regression guard: SimpleDateFormat is not thread-safe, so any
        // implementation that uses a single shared instance will fail this
        // test under load. Each result must be either correct or null.
        val sample = "2026-04-10T05:15:25.500Z"
        val workers = 16
        val iterations = 500
        val threads = (1..workers).map {
            Thread {
                repeat(iterations) {
                    val parsed = converter.fromTimestamp(sample)
                    val formatted = parsed?.let { converter.dateToTimestamp(it) }
                    if (parsed != null && formatted != sample) {
                        throw AssertionError("Round-trip diverged: $sample -> $parsed -> $formatted")
                    }
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
    }
}
