package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.RadixWalletException.PrepareTransactionException
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.IntentSignature
import com.radixdlt.sargon.IntentSignatures
import com.radixdlt.sargon.Nonce
import com.radixdlt.sargon.NotarizedTransaction
import com.radixdlt.sargon.NotarySignature
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.SignedIntent
import com.radixdlt.sargon.SignedIntentHash
import com.radixdlt.sargon.TransactionHeader
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.extensions.compile
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.init
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.transaction.NotarizationResult
import rdx.works.core.mapError
import javax.inject.Inject

class NotariseTransactionUseCase @Inject constructor() {

    @Suppress("ReturnCount")
    suspend operator fun invoke(
        request: Request,
        signatureGatherer: SignatureGatherer
    ): Result<NotarizationResult> {
        val intent = runCatching {
            TransactionIntent(
                header = TransactionHeader(
                    networkId = request.manifestData.networkId,
                    startEpochInclusive = request.startEpoch,
                    endEpochExclusive = request.endEpoch,
                    nonce = request.nonce,
                    notaryPublicKey = request.notaryPublicKey,
                    notaryIsSignatory = request.notaryIsSignatory,
                    tipPercentage = request.tipPercentage
                ),
                manifest = request.manifestData.manifestSargon,
                message = request.manifestData.messageSargon
            )
        }.getOrElse { error ->
            return Result.failure(PrepareTransactionException.BuildTransactionHeader(error))
        }

        val signatures = signatureGatherer.gatherSignatures(intent = intent).getOrElse { error ->
            return Result.failure(PrepareTransactionException.SignCompiledTransactionIntent(error))
        }

        val signedIntent = runCatching {
            SignedIntent(
                intent = intent,
                intentSignatures = IntentSignatures(signatures = signatures.map { IntentSignature.init(it) })
            )
        }.getOrElse { error ->
            return Result.failure(PrepareTransactionException.PrepareNotarizedTransaction(error))
        }

        return signatureGatherer.notarise(signedIntentHash = signedIntent.hash()).mapCatching { signature ->
            val notarizedTransaction = NotarizedTransaction(
                signedIntent = signedIntent,
                notarySignature = NotarySignature.init(signature)
            )

            NotarizationResult(
                intentHash = notarizedTransaction.signedIntent.intent.hash(),
                compiledNotarizedIntent = notarizedTransaction.compile(),
                endEpoch = request.endEpoch
            )
        }.mapError { error ->
            PrepareTransactionException.PrepareNotarizedTransaction(error)
        }
    }

    data class Request(
        val manifestData: TransactionManifestData,
        val notaryPublicKey: PublicKey,
        val notaryIsSignatory: Boolean,
        val startEpoch: Epoch,
        val endEpoch: Epoch,
        val nonce: Nonce,
        val tipPercentage: UShort
    )

    interface SignatureGatherer {

        suspend fun gatherSignatures(intent: TransactionIntent): Result<List<SignatureWithPublicKey>>

        suspend fun notarise(signedIntentHash: SignedIntentHash): Result<Signature>
    }
}
