package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.utils.removeTrailingSlash
import com.radixdlt.sargon.BagOfBytes
import com.radixdlt.sargon.Hash
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.extensions.bagOfBytesOf
import com.radixdlt.sargon.extensions.compile
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import rdx.works.core.toByteArray

sealed interface SignRequest {

    val dataToSign: BagOfBytes
    val hashedDataToSign: Hash

    class SignTransactionRequest(
        intent: TransactionIntent
    ) : SignRequest {
        // Used when signing with Ledger
        override val dataToSign: BagOfBytes = intent.compile()

        // Used when signing with device
        override val hashedDataToSign: Hash = intent.hash().hash
    }

    class SignAuthChallengeRequest(
        val challengeHex: String,
        val origin: String,
        val dAppDefinitionAddress: String
    ) : SignRequest {

        // TODO removeTrailingSlash is a hack to fix the issue with dapp login, it should be removed after logic is moved to sargon
        override val dataToSign: BagOfBytes
            get() {
                require(dAppDefinitionAddress.length <= UByte.MAX_VALUE.toInt())
                return bagOfBytesOf(
                    byteArrayOf(ROLA_PAYLOAD_PREFIX.toByte()) +
                        challengeHex.hexToBagOfBytes().toByteArray() +
                        dAppDefinitionAddress.length.toUByte().toByte() +
                        dAppDefinitionAddress.toByteArray() +
                        origin.removeTrailingSlash().toByteArray()
                )
            }

        override val hashedDataToSign: Hash
            get() = dataToSign.hash()

        companion object {
            const val ROLA_PAYLOAD_PREFIX = 0x52
        }
    }
}
