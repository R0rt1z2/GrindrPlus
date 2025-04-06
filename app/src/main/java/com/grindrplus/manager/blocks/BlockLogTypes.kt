package com.grindrplus.manager.blocks

data class BlockEvent(
    val profileId: String,
    val displayName: String,
    val eventType: String,
    val timestamp: Long,
    val packageName: String? = null,
)