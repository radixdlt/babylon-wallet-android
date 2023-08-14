package com.babylon.wallet.android.data.transaction.model

import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal

data class FeePayerSearchResult(
    val feePayerAddressFromManifest: String? = null,
    val candidates: List<FeePayerCandidate> = emptyList(),
    val insufficientBalanceToPayTheFee: Boolean = false
) {

    fun candidateXrdBalance(candidateAddress: String? = feePayerAddressFromManifest): BigDecimal =
        candidates.find { candidate ->
            candidate.account.address == candidateAddress
        }?.xrdAmount ?: BigDecimal.ZERO

    data class FeePayerCandidate(
        val account: Network.Account,
        val xrdAmount: BigDecimal
    )
}
