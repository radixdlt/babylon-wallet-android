package com.babylon.wallet.android.presentation.model

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class AddressDashboardUrlTest(
    private val address: String,
    private val url: String
) {

    @Test
    fun `convert of address to dashboard url`() {
        assertEquals(url, ActionableAddress.from(address).toDashboardUrl())
    }


    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "\"{0}\" => {1}")
        fun data() : Collection<Array<Any>> {
            return listOf(
                arrayOf(RESOURCE_ADDRESS, "$BASE_URL/resource/$RESOURCE_ADDRESS"),
                arrayOf(RESOURCE_NFT_ADDRESS, "$BASE_URL/nft/$RESOURCE_NFT_ADDRESS"),
                arrayOf(ACCOUNT_ADDRESS, "$BASE_URL/account/$ACCOUNT_ADDRESS"),
                arrayOf(PACKAGE_ADDRESS, "$BASE_URL/package/$PACKAGE_ADDRESS"),
                arrayOf(TRANSACTION_ADDRESS, "$BASE_URL/transaction/$TRANSACTION_ADDRESS"),
                arrayOf(COMPONENT_ADDRESS, "$BASE_URL/component/$COMPONENT_ADDRESS"),
                arrayOf(UNKNOWN_ADDRESS, "$BASE_URL/transaction/$UNKNOWN_ADDRESS")
            )
        }

        private const val BASE_URL = "https://rcnet-dashboard.radixdlt.com"
        private const val RESOURCE_ADDRESS = "resource_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp"
        private const val RESOURCE_NFT_ADDRESS = "resource_tdx_b_1qrwpjqtg7qmpn7zhxv6y62eua0xtu3x5lseytycqyvssefkpwy:#1#"
        private const val ACCOUNT_ADDRESS = "account_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp"
        private const val PACKAGE_ADDRESS = "package_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp"
        private const val TRANSACTION_ADDRESS = "transaction_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp"
        private const val COMPONENT_ADDRESS = "component_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp"
        private const val UNKNOWN_ADDRESS = "unknown_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp"
    }
}
