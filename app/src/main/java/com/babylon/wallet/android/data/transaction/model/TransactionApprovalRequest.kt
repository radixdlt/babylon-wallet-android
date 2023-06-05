package com.babylon.wallet.android.data.transaction.model

import com.radixdlt.toolkit.models.crypto.PrivateKey
import com.radixdlt.toolkit.models.transaction.TransactionManifest

data class TransactionApprovalRequest(
    val manifest: TransactionManifest,
    val hasLockFee: Boolean = false,
    val ephemeralNotaryPrivateKey: PrivateKey = PrivateKey.EddsaEd25519.newRandom(),
    val feePayerAddress: String? = null
)
