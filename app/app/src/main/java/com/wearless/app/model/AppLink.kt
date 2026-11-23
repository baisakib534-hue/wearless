package com.wearless.app.model

/** A link as shown in the app UI. Speed/bytes stay null until measured. */
data class AppLink(
    val id: String,
    val label: String,
    val kind: Int, // LinkKind ordinal in bonding contract
    val enabled: Boolean = true,
    val rxBps: Long? = null,
    val txBps: Long? = null,
    val rxBytes: Long? = null,
    val txBytes: Long? = null,
    val rttMs: Long? = null,
    val lossPct: Double? = null,
    val active: Boolean = false,
    val isMeasured: Boolean = false
)
