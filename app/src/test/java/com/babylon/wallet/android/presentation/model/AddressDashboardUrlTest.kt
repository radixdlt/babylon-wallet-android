package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddress
import com.babylon.wallet.android.utils.encodeUtf8
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.ComponentAddress
import com.radixdlt.sargon.IntentHash
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.PackageAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
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
        val actionableAddress = runCatching {
            ActionableAddress.Address(Address.init(address), true)
        }.getOrNull() ?: runCatching {
            ActionableAddress.GlobalId(NonFungibleGlobalId.init(address), true, true)
        }.getOrNull() ?: runCatching {
            ActionableAddress.TransactionId(IntentHash.init(address), true)
        }.getOrNull()

        assertEquals(url, actionableAddress?.dashboardUrl())
    }


    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "\"{0}\" => {1}")
        fun data() : Collection<Array<Any>> {
            return listOf(
                arrayOf(RESOURCE_ADDRESS, "$BASE_MAIN_URL/resource/$RESOURCE_ADDRESS"),
                arrayOf(RESOURCE_NFT_ADDRESS, "$BASE_MAIN_URL/nft/${RESOURCE_NFT_ADDRESS.encodeUtf8()}"),
                arrayOf(ACCOUNT_ADDRESS, "$BASE_MAIN_URL/account/$ACCOUNT_ADDRESS"),
                arrayOf(PACKAGE_ADDRESS, "$BASE_MAIN_URL/package/$PACKAGE_ADDRESS"),
                arrayOf(
                    TRANSACTION_ADDRESS, "$BASE_MAIN_URL/transaction/$TRANSACTION_ADDRESS"
                ),
                arrayOf(COMPONENT_ADDRESS, "$BASE_MAIN_URL/component/$COMPONENT_ADDRESS"),
            )
        }

        private const val BASE_MAIN_URL = "https://dashboard.radixdlt.com"
        private val RESOURCE_ADDRESS = ResourceAddress.sampleMainnet().string
        private val RESOURCE_NFT_ADDRESS = NonFungibleGlobalId.sample().string
        private val ACCOUNT_ADDRESS = AccountAddress.sampleMainnet().string
        private val PACKAGE_ADDRESS = PackageAddress.sampleMainnet().string
        private val TRANSACTION_ADDRESS = IntentHash.sample().bech32EncodedTxId
        private val COMPONENT_ADDRESS = ComponentAddress.sampleMainnet().string
    }
}
