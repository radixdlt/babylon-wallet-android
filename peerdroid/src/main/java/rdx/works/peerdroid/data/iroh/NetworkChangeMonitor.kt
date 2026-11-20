package rdx.works.peerdroid.data.iroh

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.LinkProperties
import android.net.Network
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import rdx.works.peerdroid.data.RadixConnectLog

/**
 * Reports changes of the device's network. iroh cannot detect them by itself on Android.
 */
internal fun interface NetworkChangeMonitor {

    /**
     * Emits whenever the default network changes (another network, or new addresses), while collected. The state at
     * the time of collection is not reported.
     */
    fun changes(): Flow<Unit>
}

internal class AndroidNetworkChangeMonitor(
    private val applicationContext: Context
) : NetworkChangeMonitor {

    override fun changes(): Flow<Unit> = callbackFlow {
        val connectivityManager = applicationContext.getSystemService(ConnectivityManager::class.java)
        // The callbacks run on a single ConnectivityManager thread. Registering reports the current default network
        // and its properties right away: those are remembered, not reported.
        val callback = object : ConnectivityManager.NetworkCallback() {
            private var currentNetwork: Network? = null
            private var currentAddresses: Set<LinkAddress>? = null

            override fun onAvailable(network: Network) {
                val previousNetwork = currentNetwork
                currentNetwork = network
                if (previousNetwork != null && previousNetwork != network) {
                    currentAddresses = null
                    report("another default network is available")
                }
            }

            override fun onLost(network: Network) {
                if (network == currentNetwork) {
                    currentNetwork = null
                    currentAddresses = null
                    report("the default network was lost")
                }
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                val addresses = linkProperties.linkAddresses.toSet()
                val previousAddresses = currentAddresses
                currentAddresses = addresses
                if (previousAddresses != null && previousAddresses != addresses) {
                    // Only how many: never the addresses themselves.
                    report("the default network's addresses changed (${previousAddresses.size} → ${addresses.size})")
                }
            }

            private fun report(change: String) {
                RadixConnectLog.d("🧊 network change: $change")
                trySend(Unit)
            }
        }
        val isRegistered = connectivityManager?.let {
            runCatching { it.registerDefaultNetworkCallback(callback) }
                .onFailure { error -> RadixConnectLog.w("🧊 could not observe network changes: ${error.javaClass.simpleName}") }
                .isSuccess
        } ?: false

        awaitClose {
            if (isRegistered) {
                runCatching { connectivityManager?.unregisterNetworkCallback(callback) }
            }
        }
    }.buffer(Channel.CONFLATED)
}
