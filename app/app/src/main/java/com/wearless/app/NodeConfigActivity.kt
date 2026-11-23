package com.wearless.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wearless.app.secure.NodeConfigStore

/**
 * Node config: paste a node key + server address once. Stored in Keystore-
 * backed encrypted prefs; never logged, never committed to source.
 *
 * QR scan: a camera-based QR dependency (e.g. zxing-embedded) is NOT bundled
 * because it must be downloadable offline at build time; the exact blocker is
 * recorded in QrScanHook. Paste fully works.
 */
class NodeConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_node_config)

        val serverInput = findViewById<EditText>(R.id.serverInput)
        val keyInput = findViewById<EditText>(R.id.keyInput)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val qrInfo = findViewById<TextView>(R.id.qrInfo)
        val qrHook = com.wearless.app.qr.QrScanHook

        qrInfo.text = qrHook.availabilityMessage()

        val existing = NodeConfigStore.get(this)
        if (existing != null) {
            serverInput.setText(existing.serverAddress)
            keyInput.setText("") // key is never echoed back to the UI
        }

        saveButton.setOnClickListener {
            val server = serverInput.text.toString().trim()
            val key = keyInput.text.toString().trim()
            if (server.isEmpty() || key.isEmpty()) {
                Toast.makeText(this, "Both server address and node key are required.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            NodeConfigStore.save(this, server, key)
            Toast.makeText(this, "Saved (encrypted). Key will not be shown again.", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
