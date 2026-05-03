package com.grindrplus.core

sealed class CoordinateValidationResult {
    object Valid : CoordinateValidationResult()
    data class Invalid(val reason: String) : CoordinateValidationResult()
}

fun validateCoordinates(lat: Double?, lon: Double?, name: String): CoordinateValidationResult {
    if (name.isBlank()) return CoordinateValidationResult.Invalid("Location name cannot be empty")
    if (name.length > 100) return CoordinateValidationResult.Invalid("Location name too long (max 100 characters)")
    if (lat == null) return CoordinateValidationResult.Invalid("Latitude is required")
    if (lon == null) return CoordinateValidationResult.Invalid("Longitude is required")
    if (lat !in -90.0..90.0) return CoordinateValidationResult.Invalid("Latitude must be between -90 and 90")
    if (lon !in -180.0..180.0) return CoordinateValidationResult.Invalid("Longitude must be between -180 and 180")
    return CoordinateValidationResult.Valid
}
