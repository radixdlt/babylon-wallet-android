package com.babylon.wallet.android.presentation.model

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class DisplayableAddressTest(
    private val address: String,
    private val displayed: String
) {

    @Test
    fun `convert of address to dashboard url`() {
        assertEquals(displayed, ActionableAddress.from(address).displayAddress)
    }


    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "\"{0}\" => {1}")
        fun data() : Collection<Array<Any>> {
            return listOf(
                arrayOf(RESOURCE_ADDRESS, "reso...8z96qp"),
                arrayOf(RESOURCE_NFT_ADDRESS, "#1%23"),
                arrayOf(ACCOUNT_ADDRESS, "acco...8z96qp"),
                arrayOf(PACKAGE_ADDRESS, "pack...8z96qp"),
                arrayOf(TRANSACTION_ADDRESS, "tran...8z96qp"),
                arrayOf(COMPONENT_ADDRESS, "comp...8z96qp"),
                arrayOf(UNKNOWN_ADDRESS, "unkn...8z96qp")
            )
        }

        private const val RESOURCE_ADDRESS = "resource_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp"
        private const val RESOURCE_NFT_ADDRESS = "resource_tdx_b_1qrwpjqtg7qmpn7zhxv6y62eua0xtu3x5lseytycqyvssefkpwy:#1%23"
        private const val ACCOUNT_ADDRESS = "account_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp"
        private const val PACKAGE_ADDRESS = "package_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp"
        private const val TRANSACTION_ADDRESS = "transaction_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp"
        private const val COMPONENT_ADDRESS = "component_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp"
        private const val UNKNOWN_ADDRESS = "unknown_tdx_b_1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq8z96qp"
    }
}
