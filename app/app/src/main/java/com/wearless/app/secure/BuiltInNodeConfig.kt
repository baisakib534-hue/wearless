package com.wearless.app.secure

/**
 * Built-in node configuration shipped with the app. The app knows its node the
 * moment it is installed — no manual host/key entry is required. Node Config
 * remains an optional override for changing nodes later.
 */
object BuiltInNodeConfig {
    const val SERVER_ADDRESS = "wearless-node-test.onrender.com"
    const val NODE_KEY_REF = "ref:wearless-key-1"
    const val SCHEDULER = "auto"
    const val REORDER_BUFFER = true
}
