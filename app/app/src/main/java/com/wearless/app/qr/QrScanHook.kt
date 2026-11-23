package com.wearless.app.qr

/**
 * QR scan hook. NOT wired: bundling a camera QR-scanner dependency
 * (journeyapps:zxing-android-embedded or ML Kit) requires adding a new Gradle
 * dependency plus the CAMERA permission. Exact blocker: no offline-verified
 * QR library artifact was confirmed for this local-only build, so scan is
 * intentionally left as this hook. Paste works and is the primary path.
 *
 * To wire it later: implement [startScan] with a scan activity/fragment and
 * return the scanned node string through the callback.
 */
object QrScanHook {

    fun isAvailable(): Boolean = false

    fun availabilityMessage(): String =
        "QR scan: not wired (exact blocker: no offline-confirmed QR scanner dependency; " +
        "adding journeyapps zxing-android-embedded requires a fresh dependency download + CAMERA permission). " +
        "Paste the node key instead — paste fully works."

    fun startScan(@Suppress("UNUSED_PARAMETER") onResult: (String) -> Unit): Boolean {
        // Not implemented — see availabilityMessage(). Returns false so the
        // caller never pretends a scan happened.
        return false
    }
}
