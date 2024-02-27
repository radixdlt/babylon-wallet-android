package rdx.works.profile.ret

import com.radixdlt.ret.Instructions
import com.radixdlt.ret.TransactionManifest

data class TransactionManifestData(
    val instructions: String,
    val networkId: Int,
    val message: String? = null,
    val blobs: List<ByteArray> = emptyList(),
    val version: Long = TransactionVersion.Default.value
) {

    fun toTransactionManifest() = runCatching {
        TransactionManifest(
            instructions = Instructions.fromString(
                string = instructions,
                networkId = networkId.toUByte()
            ),
            blobs = blobs
        )
    }

    companion object {
        fun from(
            manifest: TransactionManifest,
            message: String? = null,
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