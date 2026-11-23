package com.wearless.app

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.wearless.app.bonding.BondingBridge
import com.wearless.app.link.AndroidLinkHandle
import com.wearless.app.model.AppLink
import com.wearless.app.secure.NodeConfigStore
import com.wearless.app.vpn.TunnelService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var speedText: TextView
    private lateinit var toggleButton: Button
    private lateinit var linksContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)
        speedText = findViewById(R.id.speedText)
        toggleButton = findViewById(R.id.toggleButton)
        linksContainer = findViewById(R.id.linksContainer)

        findViewById<Button>(R.id.nodeConfigButton).setOnClickListener {
            startActivity(Intent(this, NodeConfigActivity::class.java))
        }
        findViewById<Button>(R.id.resourceMonitorButton).setOnClickListener {
            startActivity(Intent(this, ResourceMonitorActivity::class.java))
        }

        toggleButton.setOnClickListener {
            if (BondingBridge.state.value is BondingBridge.State.Running) {
                stopService(Intent(this, TunnelService::class.java).apply {
                    action = TunnelService.ACTION_STOP
                })
                renderStopped()
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
            statusText.text = "No node configured — open Node Config first."
            return
        }
        val prepare = VpnService.prepare(this)
        if (prepare != null) {
            // VPN consent flow. Tunnel is NOT up until the user accepts.
            startActivityForResult(prepare, 100)
            statusText.text = "Waiting for VPN permission…"
        } else {
            launchTunnelService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) launchTunnelService()
            else statusText.text = "VPN permission denied — tunnel is NOT up."
        }
    }

    private fun launchTunnelService() {
        startForegroundService(Intent(this, TunnelService::class.java).apply {
            action = TunnelService.ACTION_START
        })
        // Render immediately; the service sets the real state. No fake success.
        CoroutineScope(Dispatchers.Main).launch {
            renderState()
        }
    }

    private fun renderStopped() {
        statusText.text = "Stopped"
        speedText.text = "NOT MEASURED"
        renderLinks(AndroidLinkHandle.discover(this).map { AppLink(it.id, it.label, it.kindValue) })
    }

    private fun renderState() {
        when (val s = BondingBridge.state.value) {
            is BondingBridge.State.Running -> {
                val links = AndroidLinkHandle.discover(this)
                statusText.text = "Bonding ${links.size} links"
                toggleButton.text = "Stop"
                // Real speeds come only from the engine's snapshot. Until the
                // engine is wired and measured, display the literal string.
                val snap = BondingBridge.snapshot(AndroidLinkHandle.toAppLinks(links))
                speedText.text = if (snap.isMeasured && snap.aggregatedBps != null) {
                    formatBps(snap.aggregatedBps)
                } else {
                    "NOT MEASURED"
                }
                renderLinks(AndroidLinkHandle.toAppLinks(links))
            }
            is BondingBridge.State.EngineNotWired -> {
                statusText.text = "Engine not wired — tunnel NOT up"
                toggleButton.text = "Start"
                speedText.text = "NOT MEASURED"
                renderLinks(AndroidLinkHandle.toAppLinks(AndroidLinkHandle.discover(this)))
            }
            is BondingBridge.State.Failed -> {
                statusText.text = "Failed: ${s.message}"
                toggleButton.text = "Start"
                speedText.text = "NOT MEASURED"
                renderLinks(AndroidLinkHandle.toAppLinks(AndroidLinkHandle.discover(this)))
            }
            is BondingBridge.State.Stopped -> {
                statusText.text = if (NodeConfigStore.isConfigured(this)) "Stopped" else "No node configured"
                toggleButton.text = "Start"
                speedText.text = "NOT MEASURED"
                renderLinks(AndroidLinkHandle.toAppLinks(AndroidLinkHandle.discover(this)))
            }
        }
    }

    private fun renderLinks(links: List<AppLink>) {
        linksContainer.removeAllViews()
        if (links.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No usable links found (WiFi / mobile data)."
            tv.setPadding(0, 16, 0, 16)
            linksContainer.addView(tv)
            return
        }
        for (link in links) {
            val tv = TextView(this)
            val speed = if (link.isMeasured && link.rxBps != null) formatBps(link.rxBps) else "NOT MEASURED"
            val data = if (link.isMeasured && link.rxBytes != null) formatBytes(link.rxBytes) else "NOT MEASURED"
            tv.text = "${link.label}  ${if (link.active) "on" else "off"}\n" +
                "speed: $speed   data: $data"
            tv.setPadding(0, 16, 0, 16)
            linksContainer.addView(tv)
        }
    }

    private fun formatBps(bps: Long): String =
        if (bps >= 1_000_000) "%.1f Mbps".format(bps / 1_000_000.0)
        else "%.0f kbps".format(bps / 1_000.0)

    private fun formatBytes(b: Long): String =
        if (b >= 1_000_000_000) "%.2f GB".format(b / 1_000_000_000.0)
        else "%.1f MB".format(b / 1_000_000.0)
}
