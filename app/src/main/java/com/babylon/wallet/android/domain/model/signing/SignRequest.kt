package com.babylon.wallet.android.domain.model.signing

import com.babylon.wallet.android.utils.removeTrailingSlash
import com.radixdlt.sargon.BagOfBytes
import com.radixdlt.sargon.CompiledTransactionIntent
import com.radixdlt.sargon.Hash
import com.radixdlt.sargon.Subintent
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.extensions.bagOfBytesOf
import com.radixdlt.sargon.extensions.compile
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import rdx.works.core.toByteArray

sealed interface SignRequest {

    fun intoHash(): Hash

    data class TransactionIntentSignRequest(
        val transactionIntent: TransactionIntent,
    ) : SignRequest {

        val compiledTransactionIntent: CompiledTransactionIntent = transactionIntent.compile()

        override fun intoHash(): Hash = transactionIntent.hash().hash
    }

    data class SubintentSignRequest(
        val subintent: Subintent
    ) : SignRequest {

        override fun intoHash(): Hash = subintent.hash().hash
    }

    data class RolaSignRequest(
        val challengeHex: String,
        val origin: String,
        val dAppDefinitionAddress: String
    ) : SignRequest {

        init {
            require(dAppDefinitionAddress.length <= UByte.MAX_VALUE.toInt())
        }

        val payload: BagOfBytes = bagOfBytesOf(
            byteArrayOf(ROLA_PAYLOAD_PREFIX.toByte()) +
                challengeHex.hexToBagOfBytes().toByteArray() +
                dAppDefinitionAddress.length.toUByte().toByte() +
                dAppDefinitionAddress.toByteArray() +
                origin.removeTrailingSlash().toByteArray()
        )

        override fun intoHash(): Hash = payload.hash()

        companion object {
            const val ROLA_PAYLOAD_PREFIX = 0x52
        }
    }
}
