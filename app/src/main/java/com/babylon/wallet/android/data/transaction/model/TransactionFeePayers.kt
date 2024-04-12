package com.babylon.wallet.android.data.transaction.model

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.compareTo
import rdx.works.profile.data.model.pernetwork.Network

data class TransactionFeePayers(
    val selectedAccountAddress: AccountAddress? = null,
    val candidates: List<FeePayerCandidate> = emptyList(),
) {
    data class FeePayerCandidate(
        val account: Network.Account,
        val xrdAmount: Decimal192
    )
}

fun List<TransactionFeePayers.FeePayerCandidate>.findAccountWithAtLeast(value: Decimal192, inSet: Set<Network.Account>) = find {
    it.account in inSet && it.xrdAmount >= value
}

fun List<TransactionFeePayers.FeePayerCandidate>.findAccountWithAtLeast(value: Decimal192) = find {
    it.xrdAmount >= value
}
