package com.babylon.wallet.android.presentation.model

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class AddressTypeTest(
    private val address: String,
    private val type: Address.Type
) {

    @Test
    fun `convert of address to type`() {
        assertEquals(type, Address.Type.from(address))
    }


    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "\"{0}\" => {1}")
        fun data() : Collection<Array<Any>> {
            return listOf(
                arrayOf(RESOURCE_ADDRESS, Address.Type.RESOURCE),
                arrayOf(RESOURCE_NFT_ADDRESS, Address.Type.RESOURCE),
                arrayOf(ACCOUNT_ADDRESS, Address.Type.ACCOUNT),
                arrayOf(PACKAGE_ADDRESS, Address.Type.PACKAGE),
                arrayOf(TRANSACTION_ADDRESS, Address.Type.TRANSACTION),
                arrayOf(COMPONENT_ADDRESS, Address.Type.COMPONENT),
                arrayOf(UNKNOWN_ADDRESS, Address.Type.TRANSACTION)
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
