package com.grindrplus.utils

import kotlin.math.abs

/**
 * Fritsch–Carlson PCHIP (Piecewise Cubic Hermite Interpolation)
 * for monotonically increasing functions. By https://github.com/Supersonic
 */
class PCHIP(points: List<Pair<Long, Int>>) {

    private val n = points.size

    private val x = DoubleArray(n)
    private val y = DoubleArray(n)

    private val m = DoubleArray(n)

    init {
        require(n >= 2) { "Need at least two data points." }

        // Sanity check to ensure monotonicity
        for (i in points.indices) {
            x[i] = points[i].first.toDouble()
            y[i] = points[i].second.toDouble()
            if (i > 0) {
                require(x[i] > x[i - 1]) { "x must be strictly ascending" }
                require(y[i] >= y[i - 1]) { "y must be non-decreasing" }
            }
        }
        computeSlopes()
    }

    /**
     * 1) Compute the "interval slopes" delta_i = (y_{i+1} - y_i)/(x_{i+1} - x_i), i=0..n-2
     * 2) Set end slopes m_0 = delta_0, m_{n-1} = delta_{n-2}
     * 3) For each interior i=1..n-2:
     *     - if delta_{i-1} * delta_i <= 0 => m_i = 0
     *     - else => smooth them to ensure monotonicity
     */
    private fun computeSlopes() {
        val h = DoubleArray(n - 1)
        val delta = DoubleArray(n - 1)
        for (i in 0 until n - 1) {
            h[i] = x[i + 1] - x[i]
            require(h[i] > 0) {"Duplicate x-value"}
            delta[i] = (y[i + 1] - y[i]) / h[i]
        }

        m[0] = delta[0]
        m[n - 1] = delta[n - 2]

        for (i in 1 until n - 1) {
            if (delta[i - 1] * delta[i] <= 0.0) {
                m[i] = 0.0
            } else {
                // weighted harmonic mean
                val w1 = 2.0 * h[i] + h[i - 1]
                val w2 = h[i] + 2.0 * h[i - 1]
                m[i] = (w1 + w2) / (w1 / delta[i - 1] + w2 / delta[i])
            }
        }
    }

    /**
     * Derive the interpolant at time X (in seconds).
     * If X is outside [x[0], x[n-1]] we extrapolate using the last interval or the first.
     */
    fun interpolate(X: Double): Double {
        val i = findInterval(X)

        // transform X -> local t over [0..1]
        val h = x[i + 1] - x[i]
        val t = (X - x[i]) / h

        // hermite basis
        val t2 = t * t
        val t3 = t2 * t

        val h00 = 2.0 * t3 - 3.0 * t2 + 1.0
        val h10 = t3 - 2.0 * t2 + t
        val h01 = -2.0 * t3 + 3.0 * t2
        val h11 = t3 - t2

        return h00 * y[i] +
                h10 * h * m[i] +
                h01 * y[i + 1] +
                h11 * h * m[i + 1]
    }

    /**
     * Given a target Y (ID), find X via bisection search within the relevant interval,
     */
    fun invert(targetY: Double): Double {
        // ensure we have at least two points
        require(n >= 2) { "Need at least two data points for interpolation" }

        // clamp if targetY is outside [y[0], y[n-1]]
        if (targetY <= y[0]) return x[0]

        // use a linear extension for targetY larger than y[n-1]. Clamp to no later than now
        if (targetY >= y[n - 1]) {
            // prevent division by zero or index out of bounds by checking we have at least 2 points
            val extendedX = if (n > 1) {
                val yDiff = y[n-1] - y[n-2]
                val xDiff = x[n-1] - x[n-2]

                if (abs(yDiff) > 1e-10) {
                    val slopeXbyY = xDiff / yDiff
                    val deltaY = targetY - y[n-1]
                    x[n-1] + deltaY * slopeXbyY
                } else {
                    // if Y values are essentially the same, use the last known X value
                    x[n-1]
                }
            } else {
                // fallback to the only point if there's just one point
                x[0]
            }

            // use current system time as the upper bound
            val nowX = System.currentTimeMillis() / 1000.0

            return minOf(extendedX, nowX)
        }

        val i = findIntervalByY(targetY)
        var left = x[i]
        var right = x[i + 1]

        // 30 iterations is enough for the precision we need
        repeat(30) {
            val mid = 0.5 * (left + right)
            val fMid = interpolate(mid)
            if (fMid < targetY) {
                left = mid
            } else {
                right = mid
            }
        }
        return 0.5 * (left + right)
    }

    private fun findInterval(X: Double): Int {
        if (X <= x[0]) return 0
        if (X >= x[n - 1]) return n - 2
        for (i in 0 until n - 1) {
            if (X < x[i + 1]) {
                return i
            }
        }
        return n - 2
    }

    private fun findIntervalByY(targetY: Double): Int {
        for (i in 0 until n - 1) {
            if (targetY <= y[i + 1]) {
                return i
            }
        }
        // fallback
        return n - 2
    }
}