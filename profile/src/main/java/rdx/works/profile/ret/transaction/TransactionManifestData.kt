package rdx.works.profile.ret.transaction

import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.Message
import com.radixdlt.ret.MessageContent
import com.radixdlt.ret.PlainTextMessage
import com.radixdlt.ret.TransactionManifest
import com.radixdlt.sargon.Blob
import com.radixdlt.sargon.Blobs
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.blobs
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.discriminant
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.instructionsString
import com.radixdlt.sargon.extensions.networkId
import com.radixdlt.sargon.extensions.toBagOfBytes
import com.radixdlt.sargon.extensions.toList
import rdx.works.core.toByteArray

data class TransactionManifestData(
    val instructions: String,
    val networkId: Int,
    val message: TransactionMessage = TransactionMessage.None,
    val blobs: List<ByteArray> = emptyList(),
    val version: Long = TransactionVersion.Default.value
) {

    internal val manifest: TransactionManifest by lazy {
        TransactionManifest(
            instructions = Instructions.fromString(
                string = instructions,
                networkId = networkId.toUByte()
            ),
            blobs = blobs
        )
    }

    val manifestSargon: com.radixdlt.sargon.TransactionManifest by lazy {
        com.radixdlt.sargon.TransactionManifest.init(
            instructionsString = instructions,
            networkId = NetworkId.init(discriminant = networkId.toUByte()),
            blobs = Blobs.init(blobs = blobs.map { Blob.init(it.toBagOfBytes()) })
        )
    }

    fun entitiesRequiringAuth(): EntitiesRequiringAuth {
        val summary = manifest.summary(networkId = networkId.toUByte())

        return EntitiesRequiringAuth(
            accounts = summary.accountsRequiringAuth.map { it.addressString() },
            identities = summary.identitiesRequiringAuth.map { it.addressString() }
        )
    }

    fun feePayerCandidates(): List<String> {
        val summary = manifest.summary(networkId.toUByte())
        return (summary.accountsWithdrawnFrom + summary.accountsDepositedInto + summary.accountsRequiringAuth).map { it.addressString() }
    }

    // Currently the only method that exposes RET
    fun executionSummary(encodedReceipt: ByteArray): ExecutionSummary = manifest.executionSummary(networkId.toUByte(), encodedReceipt)

    internal val engineMessage: Message = when (message) {
        is TransactionMessage.Public -> Message.PlainText(
            value = PlainTextMessage(
                mimeType = "text/plain",
                message = MessageContent.Str(message.message)
            )
        )
        TransactionMessage.None -> Message.None
    }

    sealed interface TransactionMessage {

        val messageOrNull: String?
            get() = when (this) {
                None -> null
                is Public -> message
            }

        object None : TransactionMessage
        data class Public(val message: String) : TransactionMessage
    }

    data class EntitiesRequiringAuth(
        val accounts: List<String>,
        val identities: List<String>
    )

    companion object {
        fun from(
            manifest: TransactionManifest,
            message: TransactionMessage = TransactionMessage.None,
        ) = TransactionManifestData(
            instructions = manifest.instructions().asStr(),
            networkId = manifest.instructions().networkId().toInt(),
            message = message,
            blobs = manifest.blobs(),
            version = TransactionVersion.Default.value
        )

        fun from(
            manifest: com.radixdlt.sargon.TransactionManifest,
            message: TransactionMessage = TransactionMessage.None
        ) = TransactionManifestData(
            instructions = manifest.instructionsString,
            networkId = manifest.networkId.discriminant.toInt(),
            message = message,
            blobs = manifest.blobs.toList().map { it.bytes.toByteArray() },
            version = TransactionVersion.Default.value
        )
    }
}

enum class TransactionVersion(val value: Long) {
    Default(1L)
}
