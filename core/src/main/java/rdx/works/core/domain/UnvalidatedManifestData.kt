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

data class UnvalidatedManifestData(
    val instructions: String,
    val networkId: NetworkId,
    val plainMessage: String?,
    val blobs: List<BagOfBytes> = emptyList(),
) {

    val message: Message by lazy {
        plainMessage?.let { Message.plaintext(it) } ?: Message.None
    }

    companion object {
        fun from(
            manifest: TransactionManifest,
            message: String? = null
        ) = UnvalidatedManifestData(
            instructions = manifest.instructionsString,
            plainMessage = message,
            networkId = manifest.networkId,
            blobs = manifest.blobs.toList().map { it.bytes },
        )
    }
}
