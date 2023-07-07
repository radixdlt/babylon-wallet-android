package com.babylon.wallet.android.data.transaction.model

import com.radixdlt.ret.TransactionManifest
import rdx.works.core.crypto.PrivateKey

data class TransactionApprovalRequest(
    val manifest: TransactionManifest,
    val hasLockFee: Boolean = false,
    val ephemeralNotaryPrivateKey: PrivateKey = PrivateKey.EddsaEd25519.newRandom(),
    val feePayerAddress: String? = null
)
