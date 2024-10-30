package com.babylon.wallet.android.domain.model.transaction

import com.radixdlt.sargon.ManifestSummary
import com.radixdlt.sargon.Message
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.TransactionToReview
import com.radixdlt.sargon.extensions.summary

data class TransactionToReviewData(
    val transactionToReview: TransactionToReview,
    val message: Message
) {

    val networkId: NetworkId = transactionToReview.transactionManifest.networkId
    val manifestSummary: ManifestSummary = requireNotNull(transactionToReview.transactionManifest.summary)
}
