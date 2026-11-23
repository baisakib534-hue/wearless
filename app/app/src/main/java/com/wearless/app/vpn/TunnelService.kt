package com.wearless.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.wearless.app.MainActivity
import com.wearless.app.R
import com.wearless.app.bonding.BondingBridge
import com.wearless.app.link.AndroidLinkHandle
import com.wearless.app.secure.NodeConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground VpnService shell. Honest rules:
 * - The tunnel TUN interface is only established after VPN consent.
 * - The bonding engine is wired through BondingBridge; if it is not present in
 *   the build the service stays in "engine not wired" state and NEVER claims
 *   the tunnel works.
 * - A watchdog restarts the bonding engine if a link is lost mid-run.
 */
class TunnelService : VpnService() {

    companion object {
        const val CHANNEL_ID = "wearless_tunnel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.wearless.app.vpn.START"
        const val ACTION_STOP = "com.wearless.app.vpn.STOP"
    }

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var watchdog: Job? = null
    private var engineStarted = false

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
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTunnel() {
        requestBatteryExemptionIfNeeded()

        val prepared = prepare(this)
        if (prepared != null) {
            // Consent flow still pending. The activity handles the dialog and
            // restarts this service; the tunnel is NOT up yet.
            BondingBridge.state.value = BondingBridge.State.Stopped
            return
        }

        val cfg = NodeConfigStore.get(this)
        if (cfg == null) {
            BondingBridge.state.value = BondingBridge.State.Failed("No node configured")
            return
        }

        val links = AndroidLinkHandle.discover(this)
        val result = BondingBridge.start(cfg.serverAddress, cfg.keyRef, true, links)
        engineStarted = result.isSuccess

        // Establish the TUN interface only when the engine actually started.
        if (engineStarted) {
            Builder()
                .setSession("Wearless")
                .addAddress("10.111.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .setBlocking(true)
                .establish()
            startWatchdog()
        }
    }

    /**
     * Watchdog: every 5s check link health and engine state; restart the
     * engine if it died (tunnel loss), per WEARLESS-BOOK §6 row 4.
     */
    private fun startWatchdog() {
        watchdog?.cancel()
        watchdog = scope.launch {
            while (true) {
                delay(5_000)
                val state = BondingBridge.state.value
                if (state is BondingBridge.State.Failed || state is BondingBridge.State.Stopped) {
                    // Tunnel lost — attempt restart.
                    startTunnel()
                    break
                }
            }
        }
    }

    private fun requestBatteryExemptionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    @Suppress("DEPRECATION")
                    val intent = Intent(
                        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        android.net.Uri.parse("package:$packageName")
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (_: Exception) {
                    // Some OEMs block this intent; the exemption can also be
                    // granted manually in settings.
                }
            }
        }
    }

    private fun stopTunnel() {
        watchdog?.cancel()
        BondingBridge.stop()
        engineStarted = false
    }

    override fun onRevoke() {
        stopTunnel()
        stopSelf()
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }
}
