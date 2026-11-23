package com.wearless.app.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Node config storage. The node key is pasted ONCE by the owner and stored in
 * Android Keystore-backed EncryptedSharedPreferences. It is never written to
 * source, never logged. The stored keyRef is only an opaque reference passed
 * to the bonding engine.
 */
object NodeConfigStore {

    data class NodeConfig(
        val serverAddress: String,
        val keyRef: String,
        /** "WLB", "MIN_RTT", or "auto" (case-insensitive). Null/blank = auto. */
        val scheduler: String = "auto",
        /** Reorder buffer on by default; persisted so the user/owner can disable it. */
        val reorderBuffer: Boolean = true,
    )

    private const val PREFS = "wearless_node_secure"
    private const val K_SERVER = "server_address"
    private const val K_KEY = "node_key_ref"
    private const val K_SCHEDULER = "scheduler"
    private const val K_REORDER = "reorder_buffer"

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun get(context: Context): NodeConfig? {
        val p = prefs(context)
        val server = p.getString(K_SERVER, null) ?: return null
        val key = p.getString(K_KEY, null) ?: return null
        val scheduler = p.getString(K_SCHEDULER, "auto") ?: "auto"
        val reorder = p.getBoolean(K_REORDER, true)
        return NodeConfig(server, key, scheduler, reorder)
    }

    fun save(
        context: Context,
        serverAddress: String,
        nodeKey: String,
        scheduler: String = "auto",
        reorderBuffer: Boolean = true,
    ) {
        prefs(context).edit()
            .putString(K_SERVER, serverAddress.trim())
            .putString(K_KEY, nodeKey.trim())
            .putString(K_SCHEDULER, scheduler.trim().uppercase())
            .putBoolean(K_REORDER, reorderBuffer)
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun isConfigured(context: Context): Boolean = get(context) != null
}
