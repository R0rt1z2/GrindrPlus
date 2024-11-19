package com.grindrplus.persistence.model

data class GrindrSettings(
    val approximateDistance: Boolean,
    val hideViewedMe: Boolean,
    val incognito: Boolean,
    val locationSearchOptOut: Boolean
)