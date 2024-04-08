package com.babylon.wallet.android.data.transaction.model

import com.radixdlt.sargon.AccountAddress
import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal

data class TransactionFeePayers(
    val selectedAccountAddress: AccountAddress? = null,
    val candidates: List<FeePayerCandidate> = emptyList(),
) {
    data class FeePayerCandidate(
        val account: Network.Account,
        val xrdAmount: BigDecimal
    )
}

fun List<TransactionFeePayers.FeePayerCandidate>.findAccountWithAtLeast(value: BigDecimal, inSet: Set<Network.Account>) = find {
    it.account in inSet && it.xrdAmount >= value
}

fun List<TransactionFeePayers.FeePayerCandidate>.findAccountWithAtLeast(value: BigDecimal) = find {
    it.xrdAmount >= value
}
