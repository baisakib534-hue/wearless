package com.wearless.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.wearless.app.MainActivity
import com.wearless.app.R
import com.wearless.app.bonding.BondingBridge
import com.wearless.app.link.AndroidLinkHandle
import com.wearless.app.secure.NodeConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground VpnService shell. Honest rules:
 * - The tunnel TUN interface is only established after VPN consent and after
 *   the bonding engine actually starts.
 * - The bonding engine is wired through BondingBridge; if it is not present in
 *   the build the service stays in "engine not wired" state and NEVER claims
 *   the tunnel works.
 * - A watchdog with exponential backoff restarts the bonding engine if a link is
 *   lost mid-run; after MAX_WATCHDOG_ATTEMPTS consecutive failures the service
 *   reports a terminal Failed state instead of looping forever.
 */
class TunnelService : VpnService() {

    companion object {
        const val CHANNEL_ID = "wearless_tunnel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.wearless.app.vpn.START"
        const val ACTION_STOP = "com.wearless.app.vpn.STOP"

        /** TUN interface parameters. MTU 1500 is conservative; Tune if needed. */
        private const val TUN_ADDRESS = "10.111.0.2"
        private const val TUN_PREFIX = 32
        private const val TUN_MTU = 1500
        private const val TUN_DNS = "1.1.1.1"

        /** Watchdog retry cap. After this many consecutive failures, report Failed. */
        private const val MAX_WATCHDOG_ATTEMPTS = 5
        private const val WATCHDOG_BASE_DELAY_MS = 5_000L
        private const val WATCHDOG_MAX_DELAY_MS = 60_000L
    }

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var watchdog: Job? = null
    private var engineStarted = false
    private var tunFd: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTunnel()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundWithType()
                startTunnel()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithType() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Wearless tunnel",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (BondingBridge.isEngineAvailable()) {
            "Bonding engine wired. Watchdog active."
        } else {
            "Engine not wired — tunnel is NOT up. Waiting for bonding module."
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Wearless")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTunnel() {
        // Idempotence guard: if already running, tear down old TUN first.
        if (engineStarted) {
            stopTunnel()
        }

        // Battery exemption must be requested from a foreground Activity
        // (MainActivity), not from a background service. We do not request it
        // here; the activity handles it on user action.

        val prepared = prepare(this)
        if (prepared != null) {
            BondingBridge.state.value = BondingBridge.State.Stopped
            return
        }

        val cfg = NodeConfigStore.get(this)
        if (cfg == null) {
            BondingBridge.state.value = BondingBridge.State.Failed("No node configured")
            return
        }

        val links = AndroidLinkHandle.discover(this)
        val result = BondingBridge.start(cfg.serverAddress, cfg.keyRef, null, links)
        engineStarted = result.isSuccess

        if (!engineStarted) {
            BondingBridge.state.value = BondingBridge.State.Failed(
                "Bonding engine start failed: ${result.exceptionOrNull()?.message ?: "unknown"}"
            )
            return
        }

        // Establish TUN interface; check for null.
        val builder = Builder()
            .setSession("Wearless")
            .addAddress(TUN_ADDRESS, TUN_PREFIX)
            .addRoute("0.0.0.0", 0)
            .addDnsServer(TUN_DNS)
            .setMtu(TUN_MTU)
            // Do NOT call setBlocking(true) — default false is correct; the
            // service thread must not block on TUN I/O.

        val iface = builder.establish()
        if (iface == null) {
            BondingBridge.state.value = BondingBridge.State.Failed("TUN establish failed")
            engineStarted = false
            return
        }
        tunFd = iface

        startWatchdog()
    }

    /**
     * Re-establish the tunnel WITHOUT (re)arming the watchdog. Called by the
     * watchdog loop itself. Calling startTunnel() from inside the watchdog
     * would call startWatchdog(), which cancels the very job running it.
     */
    private fun restartTunnel() {
        stopTunnel()
        startTunnel()
    }

    /**
     * Watchdog with exponential backoff and bounded retries. After
     * MAX_WATCHDOG_ATTEMPTS consecutive failures, reports a terminal Failed
     * state and stops trying.
     */
    private fun startWatchdog() {
        watchdog?.cancel()
        watchdog = scope.launch {
            var attempt = 0
            var delayMs = WATCHDOG_BASE_DELAY_MS
            while (isActive && attempt < MAX_WATCHDOG_ATTEMPTS) {
                delay(delayMs)
                val state = BondingBridge.state.value
                if (state is BondingBridge.State.Failed || state is BondingBridge.State.Stopped) {
                    attempt++
                    if (attempt >= MAX_WATCHDOG_ATTEMPTS) {
                        BondingBridge.state.value = BondingBridge.State.Failed(
                            "Watchdog: engine unreachable after $attempt attempts"
                        )
                        break
                    }
                    // Exponential backoff: 5s, 10s, 20s, 40s, capped at 60s.
                    delayMs = (delayMs * 2).coerceAtMost(WATCHDOG_MAX_DELAY_MS)
                    // Use restartTunnel (NOT startTunnel): startTunnel calls
                    // startWatchdog which would cancel this running job.
                    restartTunnel()
                    // Reset delay after a successful restart attempt.
                    if (BondingBridge.state.value is BondingBridge.State.Running) {
                        attempt = 0
                        delayMs = WATCHDOG_BASE_DELAY_MS
                    }
                }
            }
        }
    }

    private fun stopTunnel() {
        watchdog?.cancel()
        watchdog = null
        tunFd?.close()
        tunFd = null
        BondingBridge.stop()
        engineStarted = false
    }

    override fun onRevoke() {
        stopTunnel()
        stopSelf()
    }

    override fun onDestroy() {
        stopTunnel()
        scope.cancel()
        super.onDestroy()
    }
}