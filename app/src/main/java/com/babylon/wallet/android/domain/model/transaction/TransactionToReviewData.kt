package com.babylon.wallet.android.domain.model.transaction

import com.radixdlt.sargon.Message
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.TransactionToReview
import com.radixdlt.sargon.extensions.networkId

data class TransactionToReviewData(
    val transactionToReview: TransactionToReview,
    val message: Message
) {

    val networkId: NetworkId = transactionToReview.transactionManifest.networkId
}
