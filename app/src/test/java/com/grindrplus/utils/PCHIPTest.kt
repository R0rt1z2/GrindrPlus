package com.grindrplus.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PCHIPTest {

    private val precision = 1e-6

    /** Inputs are (Long timestamp seconds, Int profile id). */
    private fun pchipOf(vararg pairs: Pair<Long, Int>): PCHIP =
        PCHIP(pairs.toList())

    // -------- construction-time preconditions --------

    @Test
    fun `single point throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            pchipOf(0L to 0)
        }
    }

    @Test
    fun `descending x throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            pchipOf(10L to 0, 5L to 1)
        }
    }

    @Test
    fun `duplicate x throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            pchipOf(0L to 0, 0L to 1)
        }
    }

    @Test
    fun `descending y throws`() {
        // PCHIP guards monotonically increasing curves only.
        assertThrows(IllegalArgumentException::class.java) {
            pchipOf(0L to 10, 1L to 5)
        }
    }

    @Test
    fun `flat y is allowed`() {
        // Non-decreasing means equal values must construct.
        pchipOf(0L to 5, 1L to 5, 2L to 5)
    }

    // -------- interpolate --------

    @Test
    fun `interpolation passes through every data point`() {
        val pchip = pchipOf(
            0L to 0,
            10L to 100,
            20L to 200,
            30L to 350,
        )
        assertEquals(0.0, pchip.interpolate(0.0), precision)
        assertEquals(100.0, pchip.interpolate(10.0), precision)
        assertEquals(200.0, pchip.interpolate(20.0), precision)
        assertEquals(350.0, pchip.interpolate(30.0), precision)
    }

    @Test
    fun `interpolation is monotonic non-decreasing across the domain`() {
        val pchip = pchipOf(
            0L to 0,
            10L to 50,
            20L to 200,
            30L to 220,
            40L to 1000,
        )
        var previous = Double.NEGATIVE_INFINITY
        var x = 0.0
        while (x <= 40.0) {
            val y = pchip.interpolate(x)
            assertTrue(
                "monotonicity violated at x=$x: previous=$previous, y=$y",
                y >= previous - precision
            )
            previous = y
            x += 0.5
        }
    }

    @Test
    fun `interpolation between two points produces a linear ramp`() {
        // Two-point Hermite reduces to a cubic that still hits both endpoints
        // and remains monotone; the midpoint should sit between the endpoints.
        val pchip = pchipOf(0L to 0, 10L to 100)
        val mid = pchip.interpolate(5.0)
        assertTrue("midpoint $mid should be between 0 and 100", mid in 0.0..100.0)
    }

    @Test
    fun `interpolation extrapolates below the domain via the first interval`() {
        val pchip = pchipOf(10L to 100, 20L to 200)
        // x < x[0] should keep the curve continuous; check it doesn't throw and
        // returns a finite, monotone-consistent value.
        val below = pchip.interpolate(0.0)
        assertTrue("extrapolation below should be finite, was $below", below.isFinite())
    }

    // -------- invert --------

    @Test
    fun `invert clamps to x0 when target is at or below y0`() {
        val pchip = pchipOf(100L to 5, 200L to 10, 300L to 20)
        assertEquals(100.0, pchip.invert(5.0), precision)
        assertEquals(100.0, pchip.invert(0.0), precision)
        assertEquals(100.0, pchip.invert(-1.0), precision)
    }

    @Test
    fun `invert produces a round-trip approximation inside the domain`() {
        val pchip = pchipOf(
            0L to 0,
            100L to 50,
            200L to 200,
            300L to 400,
        )
        // Pick a Y inside the range and verify interpolate(invert(Y)) ≈ Y.
        for (target in listOf(25.0, 100.0, 175.0, 300.0, 399.0)) {
            val x = pchip.invert(target)
            val recovered = pchip.interpolate(x)
            assertEquals(
                "round-trip failed for target=$target (x=$x, recovered=$recovered)",
                target, recovered, 0.5
            )
        }
    }

    @Test
    fun `invert linearly extends past the last point`() {
        // Use a fully historical PCHIP so the System.currentTimeMillis() upper
        // bound inside invert() never bites — the extension stays in the past.
        val pchip = pchipOf(
            946_684_800L to 100,    // 2000-01-01
            978_307_200L to 200,    // 2001-01-01
        )
        // Slope is (978_307_200 - 946_684_800) / (200 - 100) = 316_224 sec/id.
        // For target=300, expect ~ 978_307_200 + 100 * 316_224 = 1_009_929_600.
        val x = pchip.invert(300.0)
        assertEquals(1_009_929_600.0, x, 1.0)
    }

    @Test
    fun `invert clamps to current time for far-future targets`() {
        // Two recent-historical points; targetY huge → linear extension would
        // place X far in the future; invert() must cap at "now".
        val pchip = pchipOf(
            1L to 1,
            2L to 2,
        )
        val now = System.currentTimeMillis() / 1000.0
        val x = pchip.invert(1_000_000.0)
        assertTrue(
            "invert($1_000_000) should be capped near now ($now), was $x",
            x <= now + 1.0
        )
    }

    @Test
    fun `invert handles flat tail without throwing`() {
        // Trailing equal y values cause the linear-extension slope to hit the
        // yDiff~0 fallback. Result should be a finite x, not NaN/Infinity.
        val pchip = pchipOf(
            0L to 0,
            10L to 100,
            20L to 100,
            30L to 100,
        )
        val x = pchip.invert(150.0)
        assertTrue("invert on flat tail should be finite, was $x", x.isFinite())
    }
}
