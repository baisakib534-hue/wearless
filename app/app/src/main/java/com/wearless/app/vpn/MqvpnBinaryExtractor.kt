package com.wearless.app.vpn

import android.content.Context
import java.io.File

object MqvpnBinaryExtractor {
    fun engineFile(context: Context): File = File(context.filesDir, "mqvpn")

    fun isPresent(context: Context): Boolean {
        val f = engineFile(context)
        if (f.exists() && f.canExecute()) return true
        return try {
            context.assets.open("mqvpn-x86_64").use { input ->
                f.outputStream().use { out -> input.copyTo(out) }
            }
            f.setExecutable(true)
            f.exists() && f.canExecute()
        } catch (_: Exception) {
            false
        }
    }
}