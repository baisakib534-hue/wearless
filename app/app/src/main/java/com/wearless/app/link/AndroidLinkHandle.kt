package com.wearless.app.link

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import com.wearless.app.model.AppLink
import java.net.Socket

/**
 * App-side implementation of the physical link handle. Mirrors the claude-owned
 * com.wearless.bonding.LinkHandle contract (id + kind) but lives entirely in
 * the app module; the bonding module is consumed via reflection so its types
 * are never duplicated here.
 *
 * Uses ConnectivityManager + Network.bindSocket per research finding [B2]:
 * sockets created for a specific [Network] keep using that interface even when
 * Android would otherwise route everything through the "active" network.
 */
class AndroidLinkHandle(
    val id: String,
    val kindValue: Int, // LinkKind ordinal in the bonding contract: 0=WIFI 1=MOBILE 2=NEARBY 3=NODE
    private val network: Network?,
    val label: String,
    /** Optional VpnService used to protect sockets so they bypass the TUN. */
    private val vpnService: VpnService? = null,
) {
    companion object {
        const val KIND_WIFI = 0
        const val KIND_MOBILE = 1
        const val KIND_NEARBY = 2
        const val KIND_NODE = 3

        /**
         * Discover the phone's own usable links (WiFi + cellular) and keep both
         * alive by requesting them from ConnectivityManager. Returns one
         * handle per usable transport.
         */
        fun discover(context: Context): List<AndroidLinkHandle> =
            discover(context, vpnService = context as? VpnService)

        /**
         * Discover the phone's own usable links (WiFi + cellular) and keep both
         * alive by requesting them from ConnectivityManager. Returns one
         * handle per usable transport.
         *
         * When [vpnService] is non-null, each link's sockets are passed through
         * [VpnService.protect] so engine traffic bypasses the TUN (prevents
         * routing loops).
         */
        fun discover(context: Context, vpnService: VpnService? = null): List<AndroidLinkHandle> {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val out = ArrayList<AndroidLinkHandle>()
            var wifi: Network? = null
            var cell: Network? = null
            for (net in cm.allNetworks) {
                val caps = cm.getNetworkCapabilities(net) ?: continue
                val valid = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                if (!valid) continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && wifi == null) {
                    wifi = net
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && cell == null) {
                    cell = net
                }
            }
            wifi?.let { out.add(AndroidLinkHandle("wifi", KIND_WIFI, it, "WiFi", vpnService)) }
            cell?.let { out.add(AndroidLinkHandle("mobile", KIND_MOBILE, it, "Mobile data", vpnService)) }
            return out
        }

        fun toAppLinks(handles: List<AndroidLinkHandle>): List<AppLink> =
            handles.map { AppLink(it.id, it.label, it.kindValue) }
    }

    /** Bind a socket to this link's network so traffic leaves through it ([B2]). */
    fun bindSocket(socket: Socket): Boolean {
        val n = network ?: return false
        return try {
            n.bindSocket(socket)
            // Protect the socket from the TUN so it egresses via the physical link,
            // not back into the tunnel (prevents routing loops).
            vpnService?.protect(socket)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isUsable(context: Context): Boolean {
        val n = network ?: return false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return try {
            val caps = cm.getNetworkCapabilities(n)
            caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }
}
