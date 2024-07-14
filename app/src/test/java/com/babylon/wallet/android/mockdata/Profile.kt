package com.babylon.wallet.android.mockdata

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.sargon.addAccounts
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.core.sargon.initBabylon
import rdx.works.core.sargon.sample


fun Profile.Companion.sampleWithLedgerAccount(): Profile = with(FactorSource.Ledger.sample()) {
    Profile.sample().let {
        it
            .changeGatewayToNetworkId(NetworkId.MAINNET)
            .copy(factorSources = it.factorSources.asIdentifiable().append(this).asList())
    }.let {
        val newAccount = Account.initBabylon(
            networkId = NetworkId.MAINNET,
            displayName = DisplayName("My cool ledger"),
            hdPublicKey = HierarchicalDeterministicPublicKey.sample(),
            factorSourceId = value.id.asGeneral(),
            customAppearanceId = AppearanceId(0u)
        )
        it.addAccounts(listOf(Account.sampleMainnet.bob, Account.sampleMainnet.alice, Account.sampleMainnet.carol, newAccount), NetworkId.MAINNET)
    }
}