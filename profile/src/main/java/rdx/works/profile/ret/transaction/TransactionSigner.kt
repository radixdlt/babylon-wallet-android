package rdx.works.profile.ret.transaction

import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.ret.Intent
import com.radixdlt.ret.NotarizedTransaction
import com.radixdlt.ret.SignedIntent
import com.radixdlt.ret.TransactionHeader
import rdx.works.profile.ret.crypto.PublicKey
import rdx.works.profile.ret.crypto.Signature
import rdx.works.profile.ret.crypto.SignatureWithPublicKey
import javax.inject.Inject
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Result
import kotlin.String
import kotlin.Throwable
import kotlin.UInt
import kotlin.ULong
import kotlin.UShort
import kotlin.getOrElse
import kotlin.runCatching
import kotlin.toUByte

interface TransactionSigner {

    suspend fun notarise(
        request: Request,
        signatureGatherer: SignatureGatherer
    ): Result<Notarization>

    data class Request(
        val manifestData: TransactionManifestData,
        val notaryPublicKey: PublicKey,
        val notaryIsSignatory: Boolean,
        val startEpoch: ULong,
        val endEpoch: ULong,
        val nonce: UInt,
        val tipPercentage: UShort
    )
    data class Notarization(
        val txIdHash: String,
        val notarizedTransactionIntentHex: String,
        val endEpoch: ULong
    )

    interface SignatureGatherer {

        suspend fun gatherSignatures(dataToSign: ByteArray, hashedDataToSign: ByteArray): Result<List<SignatureWithPublicKey>>

        suspend fun notarise(signedIntentHash: ByteArray): Result<Signature>
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
        val header = TransactionHeader(
            networkId = request.manifestData.networkId.toUByte(),
            startEpochInclusive = request.startEpoch,
            endEpochExclusive = request.endEpoch,
            nonce = request.nonce,
            notaryPublicKey = when (request.notaryPublicKey) {
                is PublicKey.Ed25519 -> com.radixdlt.ret.PublicKey.Ed25519(request.notaryPublicKey.value)
                is PublicKey.Secp256k1 -> com.radixdlt.ret.PublicKey.Secp256k1(request.notaryPublicKey.value)
            },
            notaryIsSignatory = request.notaryIsSignatory,
            tipPercentage = request.tipPercentage
        )

        // Create intent
        val transactionIntent = runCatching {
            Intent(
                header = header,
                manifest = request.manifestData.manifest,
                message = request.manifestData.engineMessage
            )
        }.getOrElse {
            return Result.failure(TransactionSigner.Error.Sign())
        }

        val transactionIntentHash = runCatching { transactionIntent.intentHash() }.getOrElse {
            return Result.failure(TransactionSigner.Error.Sign())
        }

        val compiledTransactionIntent = runCatching { transactionIntent.compile() }.getOrElse {
            return Result.failure(TransactionSigner.Error.Prepare())
        }

        // Sign intent
        val signatures = signatureGatherer.gatherSignatures(
            dataToSign = compiledTransactionIntent,
            hashedDataToSign = transactionIntentHash.bytes()
        ).getOrElse { throwable ->
            return Result.failure(TransactionSigner.Error.Sign(throwable))
        }
        val signedTransactionIntent = runCatching {
            SignedIntent(
                intent = transactionIntent,
                intentSignatures = signatures.map {
                    when (it) {
                        is SignatureWithPublicKey.Ed25519 -> com.radixdlt.ret.SignatureWithPublicKey.Ed25519(it.signature, it.publicKey)
                        is SignatureWithPublicKey.Secp256k1 -> com.radixdlt.ret.SignatureWithPublicKey.Secp256k1(it.signature)
                    }
                }
            )
        }.getOrElse {
            return Result.failure(TransactionSigner.Error.Sign(it))
        }

        val signedIntentHash = runCatching {
            signedTransactionIntent.signedIntentHash()
        }.getOrElse { error ->
            return Result.failure(TransactionSigner.Error.Prepare(error))
        }

        // Notarise signed intent
        val notarySignature = signatureGatherer.notarise(signedIntentHash = signedIntentHash.bytes()).getOrElse {
            return Result.failure(it)
        }

        val compiledNotarizedIntent = runCatching {
            NotarizedTransaction(
                signedIntent = signedTransactionIntent,
                notarySignature = when (notarySignature) {
                    is Signature.Ed25519 -> com.radixdlt.ret.Signature.Ed25519(notarySignature.value)
                    is Signature.Secp256k1 -> com.radixdlt.ret.Signature.Secp256k1(notarySignature.value)
                }
            ).compile()
        }.getOrElse { e ->
            return Result.failure(TransactionSigner.Error.Prepare(e))
        }

        return Result.success(
            TransactionSigner.Notarization(
                txIdHash = transactionIntentHash.asStr(),
                notarizedTransactionIntentHex = compiledNotarizedIntent.toHexString(),
                endEpoch = header.endEpochExclusive
            )
        )
    }
}
