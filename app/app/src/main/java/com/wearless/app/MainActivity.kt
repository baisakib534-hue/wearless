package com.wearless.app

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.wearless.app.bonding.BondingBridge
import com.wearless.app.link.AndroidLinkHandle
import com.wearless.app.model.AppLink
import com.wearless.app.secure.NodeConfigStore
import com.wearless.app.vpn.TunnelService
import kotlinx.coroutines.launch

/**
 * Home screen. Material 3 layout (top app bar + hero status/speed card +
 * per-link cards + outlined secondary actions).
 *
 * Honesty rules kept intact:
 * - Unmeasured values render the literal "NOT MEASURED" — never a number.
 * - Start/stop and VPN consent still drive the real TunnelService; no fake
 *   success is shown.
 * - The hero speed number is announced once (accessibilityLiveRegion polite),
 *   never spammed.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var speedText: TextView
    private lateinit var toggleButton: MaterialButton
    private lateinit var linksContainer: LinearLayout
    private lateinit var linksEmptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        speedText = findViewById(R.id.speedText)
        toggleButton = findViewById(R.id.toggleButton)
        linksContainer = findViewById(R.id.linksContainer)
        linksEmptyText = findViewById(R.id.linksEmptyText)

        findViewById<MaterialButton>(R.id.nodeConfigButton).setOnClickListener {
            startActivity(Intent(this, NodeConfigActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.resourceMonitorButton).setOnClickListener {
            startActivity(Intent(this, ResourceMonitorActivity::class.java))
        }

        toggleButton.setOnClickListener {
            if (BondingBridge.state.value is BondingBridge.State.Running) {
                stopService(Intent(this, TunnelService::class.java).apply {
                    action = TunnelService.ACTION_STOP
                })
            } else {
                startTunnelWithConsent()
            }
        }

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        renderState()
    }

    override fun onResume() {
        super.onResume()
        renderState()
    }

    private fun startTunnelWithConsent() {
        if (!NodeConfigStore.isConfigured(this)) {
            statusText.text = getString(R.string.status_no_node)
            // Show transient feedback so the user knows the tap did something.
            android.widget.Toast.makeText(
                this,
                getString(R.string.status_no_node),
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }
        // Request battery exemption once per user action so the watchdog can
        // keep running under Doze. Safe to call repeatedly; the system dedupes.
        requestBatteryExemption()
        val prepare = VpnService.prepare(this)
        if (prepare != null) {
            // VPN consent flow. Tunnel is NOT up until the user accepts.
            startActivityForResult(prepare, 100)
            statusText.text = getString(R.string.status_waiting_vpn)
        } else {
            launchTunnelService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) launchTunnelService()
            else statusText.text = getString(R.string.status_vpn_denied)
        }
    }

    private fun launchTunnelService() {
        startForegroundService(Intent(this, TunnelService::class.java).apply {
            action = TunnelService.ACTION_START
        })
        // Render immediately; the service sets the real state. No fake success.
        lifecycleScope.launch {
            renderState()
        }
    }

    /**
     * Ask the user (once per start tap) to exempt the app from battery
     * optimisation so the foreground watchdog survives Doze. No-op if already
     * exempt or the intent cannot be resolved.
     */
    private fun requestBatteryExemption() {
        val pm = getSystemService(POWER_SERVICE) as? PowerManager ?: return
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
            )
        } catch (_: Exception) {
            // Some OEMs do not expose the dialog; ignore silently.
        }
    }

    private fun renderStopped() {
        statusText.text = getString(R.string.status_stopped)
        speedText.text = getString(R.string.not_measured)
        renderLinks(AndroidLinkHandle.discover(this).map { AppLink(it.id, it.label, it.kindValue) })
    }

    private fun renderState() {
        when (val s = BondingBridge.state.value) {
            is BondingBridge.State.Running -> {
                val links = AndroidLinkHandle.discover(this)
                statusText.text = getString(R.string.status_bonding_n, links.size)
                toggleButton.text = getString(R.string.action_stop)
                // Real speeds come only from the engine's snapshot. Until the
                // engine is wired and measured, display the literal string.
                val snap = BondingBridge.snapshot(AndroidLinkHandle.toAppLinks(links))
                speedText.text = if (snap.isMeasured) {
                    formatBps(snap.aggregatedRxBps ?: 0L)
                } else {
                    getString(R.string.not_measured)
                }
                renderLinks(AndroidLinkHandle.toAppLinks(links))
            }
            is BondingBridge.State.EngineNotWired -> {
                statusText.text = getString(R.string.status_engine_not_wired)
                toggleButton.text = getString(R.string.action_start)
                speedText.text = getString(R.string.not_measured)
                renderLinks(AndroidLinkHandle.toAppLinks(AndroidLinkHandle.discover(this)))
            }
            is BondingBridge.State.Failed -> {
                statusText.text = getString(R.string.status_failed, s.message)
                toggleButton.text = getString(R.string.action_start)
                speedText.text = getString(R.string.not_measured)
                renderLinks(AndroidLinkHandle.toAppLinks(AndroidLinkHandle.discover(this)))
            }
            is BondingBridge.State.Stopped -> {
                statusText.text = if (NodeConfigStore.isConfigured(this)) {
                    getString(R.string.status_stopped)
                } else {
                    getString(R.string.status_no_node)
                }
                toggleButton.text = getString(R.string.action_start)
                speedText.text = getString(R.string.not_measured)
                renderLinks(AndroidLinkHandle.toAppLinks(AndroidLinkHandle.discover(this)))
            }
        }
    }

    private fun renderLinks(links: List<AppLink>) {
        linksContainer.removeAllViews()
        if (links.isEmpty()) {
            linksEmptyText.visibility = View.VISIBLE
            return
        }
        linksEmptyText.visibility = View.GONE
        val inflater = LayoutInflater.from(this)
        for (link in links) {
            val card = inflater.inflate(R.layout.item_link_card, linksContainer, false) as MaterialCardView
            card.contentDescription = "${link.label} link"
            val name = card.findViewById<TextView>(R.id.linkNameText)
            val state = card.findViewById<TextView>(R.id.linkStateText)
            val speed = card.findViewById<TextView>(R.id.linkSpeedText)
            val data = card.findViewById<TextView>(R.id.linkDataText)
            name.text = link.label
            state.text = getString(if (link.active) R.string.link_on else R.string.link_off)
            speed.text = if (link.isMeasured && link.rxBps != null) formatBps(link.rxBps) else getString(R.string.not_measured)
            data.text = if (link.isMeasured && link.rxBytes != null) formatBytes(link.rxBytes) else getString(R.string.not_measured)
            linksContainer.addView(card)
        }
    }

    private fun formatBps(bps: Long): String =
        // Use 1000-based units consistently (1 kbps = 1000 bps, 1 Mbps = 1,000,000 bps).
        if (bps >= 1_000_000) "%.1f Mbps".format(bps / 1_000_000.0)
        else "%.0f kbps".format(bps / 1_000.0)

    private fun formatBytes(b: Long): String =
        // Use 1000-based units consistently (1 kB = 1000 bytes, 1 MB = 1,000,000 bytes).
        if (b >= 1_000_000_000) "%.2f GB".format(b / 1_000_000_000.0)
        else "%.1f MB".format(b / 1_000_000.0)
}