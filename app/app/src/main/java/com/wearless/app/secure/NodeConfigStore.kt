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

    data class NodeConfig(val serverAddress: String, val keyRef: String)

    private const val PREFS = "wearless_node_secure"
    private const val K_SERVER = "server_address"
    private const val K_KEY = "node_key_ref"

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
        return NodeConfig(server, key)
    }

    fun save(context: Context, serverAddress: String, nodeKey: String) {
        prefs(context).edit()
            .putString(K_SERVER, serverAddress.trim())
            .putString(K_KEY, nodeKey.trim())
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun isConfigured(context: Context): Boolean = get(context) != null
}
