package com.babylon.wallet.android.data.transaction.model

import com.radixdlt.ret.Message
import com.radixdlt.ret.MessageContent
import com.radixdlt.ret.PlainTextMessage
import com.radixdlt.ret.TransactionManifest
import rdx.works.core.ret.crypto.PrivateKey
import rdx.works.profile.derivation.model.NetworkId

data class TransactionApprovalRequest(
    val manifest: TransactionManifest,
    val networkId: NetworkId,
    val hasLockFee: Boolean = false,
    val ephemeralNotaryPrivateKey: PrivateKey = PrivateKey.EddsaEd25519.newRandom(),
    val feePayerAddress: String? = null,
    val message: TransactionMessage = TransactionMessage.None
) {

    sealed interface TransactionMessage {
        object None : TransactionMessage

        data class Public(val message: String) : TransactionMessage

        fun toEngineMessage(): Message = when (this) {
            is Public -> Message.PlainText(
                value = PlainTextMessage(
                    mimeType = "text/plain",
                    message = MessageContent.Str(message)
                )
            )
            None -> Message.None
        }
    }
}
