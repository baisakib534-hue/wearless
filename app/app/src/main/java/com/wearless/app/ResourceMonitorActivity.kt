package com.wearless.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.PowerManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Resource Monitor (owner's V1 order, 2026-11-18): the app's own live resource
 * use, as much as Android exposes. Where the OS does not expose a value the
 * UI shows "unavailable" — never an invented number.
 *
 * Exposed by Android to a normal app:
 * - RAM: ActivityManager memory info + Debug native heap (own process).
 * - CPU: total process CPU time via /proc self stat deltas — an approximate
 *   load percentage; Android gives no direct per-app CPU% API.
 * - Battery: level + charging state via BatteryManager (sticky broadcast).
 * - Temperature: only battery temperature is exposed; device SoC temp needs
 *   API 34 PowerManager.getThermalHeadroom / ThermalService which is
 *   restricted — shown as unavailable unless battery temp works.
 * - Per-link data: from the bonding engine snapshot; NOT MEASURED until wired.
 */
class ResourceMonitorActivity : AppCompatActivity() {

    private lateinit var ramText: TextView
    private lateinit var cpuText: TextView
    private lateinit var batteryText: TextView
    private lateinit var tempText: TextView
    private lateinit var perLinkText: TextView
    private var poller: Job? = null

    private var lastCpuMillis: Long = -1
    private var lastWallMillis: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resource_monitor)
        ramText = findViewById(R.id.ramText)
        cpuText = findViewById(R.id.cpuText)
        batteryText = findViewById(R.id.batteryText)
        tempText = findViewById(R.id.tempText)
        perLinkText = findViewById(R.id.perLinkText)
        findViewById<MaterialButton>(R.id.backButton).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        poller = CoroutineScope(Dispatchers.Main + Job()).launch {
            while (isActive) {
                sample()
                delay(2_000)
            }
        }
    }

    override fun onPause() {
        poller?.cancel()
        super.onPause()
    }

    private fun sample() {
        // RAM
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pid = android.os.Process.myPid()
        val infos = am.getProcessMemoryInfo(intArrayOf(pid))
        val info = infos.firstOrNull()
        val totalPssKb = info?.totalPss ?: -1
        val dalvikKb = info?.dalvikPss ?: -1
        val nativeKb = info?.nativePss ?: -1
        ramText.text = if (totalPssKb >= 0) {
            "App RAM (PSS): ${"%.1f".format(totalPssKb / 1024.0)} MB\n" +
                "  dalvik: ${"%.1f".format(dalvikKb / 1024.0)} MB, " +
                "native: ${"%.1f".format(nativeKb / 1024.0)} MB, " +
                "Debug native heap allocated: ${"%.1f".format(Debug.getNativeHeapAllocatedSize() / 1048576.0)} MB"
        } else {
            "App RAM: unavailable"
        }

        // CPU — Android exposes no direct per-app CPU%; approximate from
        // /proc/<pid>/stat utime+stime deltas vs wall clock.
        cpuText.text = sampleCpu(pid)

        // Battery
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        batteryText.text = if (level in 1..100) {
            "Battery: $level% ${if (charging) "(charging)" else "(discharging)"}"
        } else {
            "Battery: unavailable"
        }

        // Temperature — battery temp only (SoC temp restricted on API 34).
        val batIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batTempTenths = batIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        tempText.text = if (batTempTenths != Int.MIN_VALUE) {
            "Device temperature: only battery sensor exposed by OS: " +
                "${"%.1f".format(batTempTenths / 10.0)} °C (SoC temp: unavailable)"
        } else {
            "Device temperature: unavailable"
        }

        // Per-link data — only from the wired bonding engine.
        val snap = com.wearless.app.bonding.BondingBridge.snapshot(emptyList())
        perLinkText.text = if (snap.isMeasured) {
            snap.links.joinToString("\n") { l ->
                val rx = l.rxBps?.let { "${"%.0f".format(it / 1000.0)} kbps" } ?: "NOT MEASURED"
                val bytes = l.rxBytes?.let { "${"%.1f".format(it / 1048576.0)} MB" } ?: "NOT MEASURED"
                "${l.label}: rx $rx, total $bytes"
            }
        } else {
            "Per-link data: NOT MEASURED (bonding engine not wired)"
        }
    }

    private fun sampleCpu(pid: Int): String {
        return try {
            val stat = java.io.File("/proc/$pid/stat").readText().split(" ")
            // fields 14 (utime) and 15 (stime), 1-based -> index 13, 14
            val cpuTicks = stat[13].toLong() + stat[14].toLong()
            val now = System.currentTimeMillis()
            if (lastCpuMillis >= 0 && now > lastWallMillis) {
                val ticksPerSec = 100L // standard USER_HZ on Android/Linux
                val cpuDeltaMs = (cpuTicks - lastCpuMillis) * 1000 / ticksPerSec
                val wallDelta = now - lastWallMillis
                val pct = if (wallDelta > 0) cpuDeltaMs * 100.0 / wallDelta else 0.0
                "CPU (approx, process): ${"%.1f".format(pct)}% (deltas over ${wallDelta} ms; " +
                    "Android exposes no direct per-app CPU% API)"
            } else {
                "CPU (approx, process): measuring…"
            }
            .also { line ->
                lastCpuMillis = cpuTicks
                lastWallMillis = now
            }
        } catch (_: Exception) {
            "CPU: unavailable"
        }
    }
}
