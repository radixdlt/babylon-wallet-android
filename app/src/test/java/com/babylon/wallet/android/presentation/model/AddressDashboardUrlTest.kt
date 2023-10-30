package com.babylon.wallet.android.presentation.model

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import rdx.works.profile.derivation.model.NetworkId

@RunWith(Parameterized::class)
internal class AddressDashboardUrlTest(
    private val address: String,
    private val url: String
) {

    @Test
    fun `convert of address to dashboard url`() {
        assertEquals(url, ActionableAddress(address).toDashboardUrl(networkId = NetworkId.Mainnet))
    }


    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "\"{0}\" => {1}")
        fun data() : Collection<Array<Any>> {
            return listOf(
                arrayOf(RESOURCE_ADDRESS, "$BASE_MAIN_URL/resource/$RESOURCE_ADDRESS"),
                arrayOf(RESOURCE_NFT_ADDRESS, "$BASE_MAIN_URL/nft/$RESOURCE_NFT_ADDRESS"),
                arrayOf(ACCOUNT_ADDRESS, "$BASE_MAIN_URL/account/$ACCOUNT_ADDRESS"),
                arrayOf(PACKAGE_ADDRESS, "$BASE_MAIN_URL/package/$PACKAGE_ADDRESS"),
                arrayOf(
                    "txid_tdx_e_106vpmdem94zr2x4yt2rf4l935d26pzrnlmz6wzwrwgz8s6jpyhmsa5ze2x",
                    "$BASE_MAIN_URL/transaction/txid_tdx_e_106vpmdem94zr2x4yt2rf4l935d26pzrnlmz6wzwrwgz8s6jpyhmsa5ze2x"
                ),
                arrayOf(
                    "txid_tdx_22_1t0sdhcc6usx8sun36cd95jn6xlwzt4mw3gctz5r5yp94fjss9u3syuqcxg",
                    "$BASE_MAIN_URL/transaction/txid_tdx_22_1t0sdhcc6usx8sun36cd95jn6xlwzt4mw3gctz5r5yp94fjss9u3syuqcxg"
                ),
                arrayOf(COMPONENT_ADDRESS, "$BASE_MAIN_URL/component/$COMPONENT_ADDRESS"),
                arrayOf(UNKNOWN_ADDRESS, "$BASE_MAIN_URL/$UNKNOWN_ADDRESS")
            )
        }

        private const val BASE_MAIN_URL = "https://dashboard.radixdlt.com"
        private const val RESOURCE_ADDRESS = "resource_tdx_e_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxx8rpsmc"
        private const val RESOURCE_NFT_ADDRESS = "resource_tdx_e_1t45e0q75zln8jk5z3vyxp88hrugj5p7alr8spspv2r79lv0px6rpdg:#1#"
        private const val ACCOUNT_ADDRESS = "account_tdx_e_12xg8lhnkj986aza4kecg4e06d5tn0x4lsu2jxa84xhrld6y8shfzls"
        private const val PACKAGE_ADDRESS = "package_tdx_e_1pkgxxxxxxxxxresrcexxxxxxxxx000538436477xxxxxxxxxptqk7h"
        private const val TRANSACTION_ADDRESS = "txid_tdx_e_106vpmdem94zr2x4yt2rf4l935d26pzrnlmz6wzwrwgz8s6jpyhmsa5ze2x"
        private const val COMPONENT_ADDRESS = "component_tdx_e_1cptxxxxxxxxxfaucetxxxxxxxxx000527798379xxxxxxxxxnp0l9m"
        private const val UNKNOWN_ADDRESS = "unknown_tdx_e_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp"
    }
}
