package com.babylon.wallet.android.domain.usecases.signing

import com.babylon.wallet.android.data.dapp.model.LedgerErrorCode
import com.babylon.wallet.android.domain.RadixWalletException.DappRequestException
import com.babylon.wallet.android.domain.RadixWalletException.LedgerCommunicationException
import com.babylon.wallet.android.domain.RadixWalletException.PrepareTransactionException
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.IntentSignature
import com.radixdlt.sargon.IntentSignatures
import com.radixdlt.sargon.Message
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Nonce
import com.radixdlt.sargon.NotarizedTransaction
import com.radixdlt.sargon.NotarySignature
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.SignedIntent
import com.radixdlt.sargon.SignedIntentHash
import com.radixdlt.sargon.TransactionHeader
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.init
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
                    networkId = request.networkId,
                    startEpochInclusive = request.startEpoch,
                    endEpochExclusive = request.endEpoch,
                    nonce = request.nonce,
                    notaryPublicKey = request.notaryPublicKey,
                    notaryIsSignatory = request.notaryIsSignatory,
                    tipPercentage = request.tipPercentage
                ),
                manifest = request.manifest,
                message = request.message
            )
        }.getOrElse { error ->
            return Result.failure(PrepareTransactionException.BuildTransactionHeader(error))
        }

        val signatures = signatureGatherer.gatherSignatures(intent = intent).getOrElse { error ->
            if (error is DappRequestException.RejectedByUser ||
                (
                    error is LedgerCommunicationException.FailedToSignTransaction &&
                        error.reason == LedgerErrorCode.UserRejectedSigningOfTransaction
                    )
            ) {
                return Result.failure(DappRequestException.RejectedByUser)
            } else {
                return Result.failure(PrepareTransactionException.SignCompiledTransactionIntent(error))
            }
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
                notarySignature = signature
            )

            NotarizationResult(
                intentHash = notarizedTransaction.signedIntent.intent.hash(),
                endEpoch = request.endEpoch,
                notarizedTransaction = notarizedTransaction
            )
        }.mapError { error ->
            PrepareTransactionException.PrepareNotarizedTransaction(error)
        }
    }

    data class Request(
        val manifest: TransactionManifest,
        val networkId: NetworkId,
        val message: Message,
        val notaryPublicKey: PublicKey,
        val notaryIsSignatory: Boolean,
        val startEpoch: Epoch,
        val endEpoch: Epoch,
        val nonce: Nonce,
        val tipPercentage: UShort
    )

    interface SignatureGatherer {

        suspend fun gatherSignatures(intent: TransactionIntent): Result<List<SignatureWithPublicKey>>

        suspend fun notarise(signedIntentHash: SignedIntentHash): Result<NotarySignature>
    }
}
