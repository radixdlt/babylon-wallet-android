package com.babylon.wallet.android.fakes

import rdx.works.peerdroid.data.iroh.IrohRelayConfig

/**
 * Validates like the iroh library for the cases the tests need: https or http, with a host.
 */
class FakeIrohRelayConfig(
    var isLibraryAvailable: Boolean = true
) : IrohRelayConfig {

    override fun publicRelayUrls(): List<String> {
        check(isLibraryAvailable) { "the radix-connect-iroh native library is not available" }
        return PUBLIC_RELAYS
    }

    override fun validateRelayUrl(relayUrl: String): String? {
        check(isLibraryAvailable) { "the radix-connect-iroh native library is not available" }
        return when {
            !relayUrl.startsWith("https://") && !relayUrl.startsWith("http://") -> "invalid relay URL: the scheme must be https or http"
            relayUrl.substringAfter("://").isEmpty() -> "invalid relay URL: the URL has no host"
            else -> null
        }
    }

    companion object {
        val PUBLIC_RELAYS = listOf("https://euc1-1.relay.example.com./", "https://use1-1.relay.example.com./")
    }
}
