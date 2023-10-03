package com.babylon.wallet.android.data.transaction.model

import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal

data class FeePayerSearchResult(
    val feePayerAddress: String? = null,
    val candidates: List<FeePayerCandidate> = emptyList(),
) {
    data class FeePayerCandidate(
        val account: Network.Account,
        val xrdAmount: BigDecimal
    )
}
