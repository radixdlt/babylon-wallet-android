package com.babylon.wallet.android.presentation.model

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class ActionableAddressTypeTest(
    private val address: String,
    private val type: ActionableAddress.Type
) {

    @Test
    fun `convert of address to type`() {
        assertEquals(type, ActionableAddress.Type.from(address))
    }


    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "\"{0}\" => {1}")
        fun data() : Collection<Array<Any>> {
            return listOf(
                arrayOf(RESOURCE_ADDRESS, ActionableAddress.Type.RESOURCE),
                arrayOf(RESOURCE_NFT_ADDRESS, ActionableAddress.Type.RESOURCE),
                arrayOf(ACCOUNT_ADDRESS, ActionableAddress.Type.ACCOUNT),
                arrayOf(PACKAGE_ADDRESS, ActionableAddress.Type.PACKAGE),
                arrayOf(TRANSACTION_ADDRESS, ActionableAddress.Type.TRANSACTION),
                arrayOf(COMPONENT_ADDRESS, ActionableAddress.Type.COMPONENT),
                arrayOf(UNKNOWN_ADDRESS, ActionableAddress.Type.TRANSACTION)
            )
        }

        private const val RESOURCE_ADDRESS = "resource_tdx_e_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxx8rpsmc"
        private const val RESOURCE_NFT_ADDRESS = "resource_tdx_e_1nfv59lfzr3ur7ahlh98n0k558savhvr28yvu6m2jukxehtl388l7eh"
        private const val ACCOUNT_ADDRESS = "account_tdx_e_168e8u653alt59xm8ple6khu6cgce9cfx9mlza6wxf7qs3wwd7ce3h5"
        private const val PACKAGE_ADDRESS = "package_tdx_e_1pkgxxxxxxxxxpackgexxxxxxxxx000726633226xxxxxxxxxr0grwh"
        private const val TRANSACTION_ADDRESS = "txid_tdx_e_1957r3acmpul3zg0fe4le974lgufc8exu93f352ve0fymqrwlhy8svlggqm"
        private const val COMPONENT_ADDRESS = "component_tdx_e_1cptxxxxxxxxxfaucetxxxxxxxxx000527798379xxxxxxxxxnp0l9m"
        private const val UNKNOWN_ADDRESS = "unknown_tdx_e_168e8u653alt59xm8ple6khu6cgce9cfx9mlza6wxf7qs3wwd7ce3h5"
    }
}
