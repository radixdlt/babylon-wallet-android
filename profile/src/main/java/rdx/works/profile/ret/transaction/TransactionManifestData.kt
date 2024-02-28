package rdx.works.profile.ret.transaction

import com.radixdlt.ret.Instructions
import com.radixdlt.ret.Message
import com.radixdlt.ret.MessageContent
import com.radixdlt.ret.PlainTextMessage
import com.radixdlt.ret.TransactionManifest

data class TransactionManifestData(
    val instructions: String,
    val networkId: Int,
    val message: TransactionMessage = TransactionMessage.None,
    val blobs: List<ByteArray> = emptyList(),
    val version: Long = TransactionVersion.Default.value
) {

    fun entitiesRequiringAuth(): EntitiesRequiringAuth {
        val summary = toTransactionManifest().getOrThrow().summary(networkId = networkId.toUByte())

        return EntitiesRequiringAuth(
            accounts = summary.accountsRequiringAuth.map { it.addressString() },
            identities = summary.identitiesRequiringAuth.map { it.addressString() }
        )
    }

    fun toTransactionManifest() = runCatching {
        TransactionManifest(
            instructions = Instructions.fromString(
                string = instructions,
                networkId = networkId.toUByte()
            ),
            blobs = blobs
        )
    }

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
    }
}

enum class TransactionVersion(val value: Long) {
    Default(1L)
}