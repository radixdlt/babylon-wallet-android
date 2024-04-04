package rdx.works.profile.ret.transaction

import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.ret.Intent
import com.radixdlt.ret.Message
import com.radixdlt.ret.MessageContent
import com.radixdlt.ret.NotarizedTransaction
import com.radixdlt.ret.PlainTextMessage
import com.radixdlt.ret.SignedIntent
import com.radixdlt.ret.TransactionHeader
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.publicKey
import com.radixdlt.sargon.extensions.signature
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.toByteArray
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
                is PublicKey.Ed25519 -> com.radixdlt.ret.PublicKey.Ed25519(request.notaryPublicKey.bytes.toByteArray())
                is PublicKey.Secp256k1 -> com.radixdlt.ret.PublicKey.Secp256k1(request.notaryPublicKey.bytes.toByteArray())
            },
            notaryIsSignatory = request.notaryIsSignatory,
            tipPercentage = request.tipPercentage
        )

        // Create intent
        val transactionIntent = runCatching {
            Intent(
                header = header,
                manifest = request.manifestData.engineManifest,
                message = when (val message = request.manifestData.message) {
                    is TransactionManifestData.TransactionMessage.Public -> Message.PlainText(
                        value = PlainTextMessage(
                            mimeType = "text/plain",
                            message = MessageContent.Str(message.message)
                        )
                    )
                    TransactionManifestData.TransactionMessage.None -> Message.None
                }
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
                    val signatureBytes = it.signature.bytes.toByteArray()
                    val publicKeyBytes = it.publicKey.bytes.toByteArray()
                    when (it) {
                        is SignatureWithPublicKey.Ed25519 -> com.radixdlt.ret.SignatureWithPublicKey.Ed25519(
                            signature = signatureBytes,
                            publicKey = publicKeyBytes
                        )
                        is SignatureWithPublicKey.Secp256k1 -> com.radixdlt.ret.SignatureWithPublicKey.Secp256k1(
                            signature = signatureBytes
                        )
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
                    is Signature.Ed25519 -> com.radixdlt.ret.Signature.Ed25519(notarySignature.bytes.toByteArray())
                    is Signature.Secp256k1 -> com.radixdlt.ret.Signature.Secp256k1(notarySignature.bytes.toByteArray())
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
