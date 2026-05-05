package com.grindrplus.manager.installation.steps

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberUtilTest {

    @Test
    fun `zero renders as Zero`() {
        assertEquals("Zero", numberToWords(0))
    }

    @Test
    fun `single digits use the ones table`() {
        assertEquals("One", numberToWords(1))
        assertEquals("Five", numberToWords(5))
        assertEquals("Nine", numberToWords(9))
    }

    @Test
    fun `teen values use the dedicated entries`() {
        assertEquals("Ten", numberToWords(10))
        assertEquals("Eleven", numberToWords(11))
        assertEquals("Fifteen", numberToWords(15))
        assertEquals("Nineteen", numberToWords(19))
    }

    @Test
    fun `tens combine the tens table with ones`() {
        assertEquals("Twenty", numberToWords(20))
        assertEquals("Twenty One", numberToWords(21))
        assertEquals("Forty Two", numberToWords(42))
        assertEquals("Ninety Nine", numberToWords(99))
    }

    @Test
    fun `hundreds spell out the hundreds component then the remainder`() {
        assertEquals("One Hundred", numberToWords(100))
        assertEquals("One Hundred One", numberToWords(101))
        assertEquals("Two Hundred Fifty", numberToWords(250))
        assertEquals("Nine Hundred Ninety Nine", numberToWords(999))
    }

    @Test
    fun `thousands compose recursively`() {
        assertEquals("One Thousand", numberToWords(1_000))
        assertEquals("One Thousand One", numberToWords(1_001))
        assertEquals("Twelve Thousand Three Hundred Forty Five", numberToWords(12_345))
        assertEquals("Nine Hundred Ninety Nine Thousand Nine Hundred Ninety Nine", numberToWords(999_999))
    }

    @Test
    fun `millions compose recursively`() {
        assertEquals("One Million", numberToWords(1_000_000))
        assertEquals("Two Million Three Hundred Forty Five Thousand Six Hundred Seventy Eight", numberToWords(2_345_678))
    }

    @Test
    fun `billions compose recursively`() {
        assertEquals("One Billion", numberToWords(1_000_000_000))
        assertEquals(
            "Two Billion One Hundred Forty Seven Million Four Hundred Eighty Three Thousand Six Hundred Forty Seven",
            numberToWords(Int.MAX_VALUE),
        )
    }

    @Test
    fun `output never has trailing whitespace`() {
        // The internal accumulator inserts trailing spaces between scale words;
        // the final result must trim them.
        for (n in listOf(1, 19, 20, 100, 1_000, 1_000_000, 1_000_000_001, Int.MAX_VALUE)) {
            val words = numberToWords(n)
            assertEquals("trailing space in '$words'", words, words.trim())
        }
    }
}
