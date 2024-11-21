package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.sargon.TransactionToReview
import com.radixdlt.sargon.extensions.Curve25519SecretKey

data class TransactionToReviewOutcome(
    val transactionToReview: TransactionToReview,
    val ephemeralNotaryPrivateKey: Curve25519SecretKey
)
