package com.grindrplus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateValidatorTest {

    @Test
    fun `valid coordinates pass`() {
        val result = validateCoordinates(40.7128, -74.0060, "New York")
        assertEquals(CoordinateValidationResult.Valid, result)
    }

    @Test
    fun `latitude boundary 90 passes`() {
        assertEquals(CoordinateValidationResult.Valid, validateCoordinates(90.0, 0.0, "North Pole"))
        assertEquals(CoordinateValidationResult.Valid, validateCoordinates(-90.0, 0.0, "South Pole"))
    }

    @Test
    fun `longitude boundary 180 passes`() {
        assertEquals(CoordinateValidationResult.Valid, validateCoordinates(0.0, 180.0, "Dateline"))
        assertEquals(CoordinateValidationResult.Valid, validateCoordinates(0.0, -180.0, "Dateline West"))
    }

    @Test
    fun `latitude above 90 is invalid`() {
        val result = validateCoordinates(90.1, 0.0, "Out of Range")
        assertTrue(result is CoordinateValidationResult.Invalid)
    }

    @Test
    fun `latitude below minus 90 is invalid`() {
        val result = validateCoordinates(-90.1, 0.0, "Out of Range")
        assertTrue(result is CoordinateValidationResult.Invalid)
    }

    @Test
    fun `longitude above 180 is invalid`() {
        val result = validateCoordinates(0.0, 180.1, "Out of Range")
        assertTrue(result is CoordinateValidationResult.Invalid)
    }

    @Test
    fun `longitude below minus 180 is invalid`() {
        val result = validateCoordinates(0.0, -180.1, "Out of Range")
        assertTrue(result is CoordinateValidationResult.Invalid)
    }

    @Test
    fun `blank name is invalid`() {
        val result = validateCoordinates(0.0, 0.0, "")
        assertTrue(result is CoordinateValidationResult.Invalid)
    }

    @Test
    fun `name over 100 chars is invalid`() {
        val longName = "a".repeat(101)
        val result = validateCoordinates(0.0, 0.0, longName)
        assertTrue(result is CoordinateValidationResult.Invalid)
    }

    @Test
    fun `name exactly 100 chars is valid`() {
        val maxName = "a".repeat(100)
        assertEquals(CoordinateValidationResult.Valid, validateCoordinates(0.0, 0.0, maxName))
    }

    @Test
    fun `null latitude is invalid`() {
        val result = validateCoordinates(null, 0.0, "Test")
        assertTrue(result is CoordinateValidationResult.Invalid)
    }

    @Test
    fun `null longitude is invalid`() {
        val result = validateCoordinates(0.0, null, "Test")
        assertTrue(result is CoordinateValidationResult.Invalid)
    }

    @Test
    fun `NaN latitude is invalid`() {
        val result = validateCoordinates(Double.NaN, 0.0, "Test")
        assertTrue(result is CoordinateValidationResult.Invalid)
    }

    @Test
    fun `NaN longitude is invalid`() {
        val result = validateCoordinates(0.0, Double.NaN, "Test")
        assertTrue(result is CoordinateValidationResult.Invalid)
    }

    @Test
    fun `positive infinity latitude is invalid`() {
        val result = validateCoordinates(Double.POSITIVE_INFINITY, 0.0, "Test")
        assertTrue(result is CoordinateValidationResult.Invalid)
    }

    @Test
    fun `negative infinity latitude is invalid`() {
        val result = validateCoordinates(Double.NEGATIVE_INFINITY, 0.0, "Test")
        assertTrue(result is CoordinateValidationResult.Invalid)
    }

    @Test
    fun `positive infinity longitude is invalid`() {
        val result = validateCoordinates(0.0, Double.POSITIVE_INFINITY, "Test")
        assertTrue(result is CoordinateValidationResult.Invalid)
    }

    @Test
    fun `negative infinity longitude is invalid`() {
        val result = validateCoordinates(0.0, Double.NEGATIVE_INFINITY, "Test")
        assertTrue(result is CoordinateValidationResult.Invalid)
    }
}
