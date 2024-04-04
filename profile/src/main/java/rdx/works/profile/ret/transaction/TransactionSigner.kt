package rdx.works.profile.ret.transaction

import com.radixdlt.sargon.CompiledNotarizedIntent
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
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.extensions.compile
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.init
import rdx.works.core.domain.TransactionManifestData
import javax.inject.Inject

interface TransactionSigner {

    suspend fun notarise(
        request: Request,
        signatureGatherer: SignatureGatherer
    ): Result<Notarization>

    data class Request(
        val manifestData: TransactionManifestData,
        val notaryPublicKey: PublicKey,
        val notaryIsSignatory: Boolean,
        val startEpoch: Epoch,
        val endEpoch: Epoch,
        val nonce: Nonce,
        val tipPercentage: UShort
    )

    data class Notarization(
        val txIdHash: SignedIntentHash,
        val notarizedTransactionIntentHex: CompiledNotarizedIntent,
        val endEpoch: Epoch
    )

    interface SignatureGatherer {

        suspend fun gatherSignatures(intent: TransactionIntent): Result<List<SignatureWithPublicKey>>

        suspend fun notarise(signedIntentHash: SignedIntentHash): Result<Signature>
    }

    sealed class Error : Throwable() {
        data class Sign(override val cause: Throwable? = null) : Error()
        data class Prepare(override val cause: Throwable? = null) : Error()
    }
}

class TransactionSignerImpl @Inject constructor() : TransactionSigner {

    @Suppress("ReturnCount", "LongMethod")
    override suspend fun notarise(
        request: TransactionSigner.Request,
        signatureGatherer: TransactionSigner.SignatureGatherer
    ): Result<TransactionSigner.Notarization> {
        // Build header
        val header = com.radixdlt.sargon.TransactionHeader(
            networkId = request.manifestData.networkIdSargon,
            startEpochInclusive = request.startEpoch,
            endEpochExclusive = request.endEpoch,
            nonce = request.nonce,
            notaryPublicKey = request.notaryPublicKey,
            notaryIsSignatory = request.notaryIsSignatory,
            tipPercentage = request.tipPercentage
        )

        // Create intent
        val transactionIntent = runCatching {
            TransactionIntent(
                header = header,
                manifest = request.manifestData.manifestSargon,
                message = request.manifestData.messageSargon
            )
        }.getOrElse {
            return Result.failure(TransactionSigner.Error.Sign())
        }

        // Sign intent
        val signatures = signatureGatherer.gatherSignatures(
            intent = transactionIntent
        ).getOrElse { throwable ->
            return Result.failure(TransactionSigner.Error.Sign(throwable))
        }
        val signedTransactionIntent = runCatching {
            SignedIntent(
                intent = transactionIntent,
                intentSignatures = IntentSignatures(signatures = signatures.map { IntentSignature.init(it) })
            )
        }.getOrElse {
            return Result.failure(TransactionSigner.Error.Sign(it))
        }

        val signedIntentHash = runCatching {
            signedTransactionIntent.hash()
        }.getOrElse { error ->
            return Result.failure(TransactionSigner.Error.Prepare(error))
        }

        // Notarise signed intent
        val notarySignature = signatureGatherer.notarise(signedIntentHash = signedIntentHash).getOrElse {
            return Result.failure(it)
        }

        val notarisedTransaction = runCatching {
            NotarizedTransaction(
                signedIntent = signedTransactionIntent,
                notarySignature = NotarySignature.init(notarySignature)
            )
        }.getOrElse { e ->
            return Result.failure(TransactionSigner.Error.Prepare(e))
        }

        return Result.success(
            TransactionSigner.Notarization(
                txIdHash = notarisedTransaction.signedIntent.hash(),
                notarizedTransactionIntentHex = notarisedTransaction.compile(),
                endEpoch = header.endEpochExclusive
            )
        )
    }
}
