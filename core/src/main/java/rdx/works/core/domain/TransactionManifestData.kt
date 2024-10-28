package rdx.works.core.domain

import com.radixdlt.sargon.BagOfBytes
import com.radixdlt.sargon.Message
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.blobs
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.instructionsString
import com.radixdlt.sargon.extensions.networkId
import com.radixdlt.sargon.extensions.plaintext
import com.radixdlt.sargon.extensions.toList

data class TransactionManifestData(
    val manifest: TransactionManifest,
    val instructions: String,
    val networkId: NetworkId,
    val message: TransactionMessage = TransactionMessage.None,
    val blobs: List<BagOfBytes> = emptyList(),
    val version: Long = TransactionVersion.Default.value
) {

    val messageSargon: Message by lazy {
        when (message) {
            TransactionMessage.None -> Message.None
            is TransactionMessage.Public -> Message.plaintext(message.message)
        }
    }

    sealed interface TransactionMessage {

        val messageOrNull: String?
            get() = when (this) {
                None -> null
                is Public -> message
            }

        data object None : TransactionMessage
        data class Public(val message: String) : TransactionMessage
    }

    companion object {
        fun from(
            manifest: TransactionManifest,
            message: TransactionMessage = TransactionMessage.None
        ) = TransactionManifestData(
            manifest = manifest,
            instructions = manifest.instructionsString,
            networkId = manifest.networkId,
            message = message,
            blobs = manifest.blobs.toList().map { it.bytes },
            version = TransactionVersion.Default.value
        )
    }
}

enum class TransactionVersion(val value: Long) {
    Default(1L)
}
