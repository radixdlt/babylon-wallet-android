package rdx.works.core.domain

import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.Instructions
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Blob
import com.radixdlt.sargon.Blobs
import com.radixdlt.sargon.Message
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.blobs
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.discriminant
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.instructionsString
import com.radixdlt.sargon.extensions.networkId
import com.radixdlt.sargon.extensions.plaintext
import com.radixdlt.sargon.extensions.toBagOfBytes
import com.radixdlt.sargon.extensions.toList
import rdx.works.core.toByteArray

private typealias EngineManifest = com.radixdlt.ret.TransactionManifest

data class TransactionManifestData(
    val instructions: String,
    val networkId: Int,
    val message: TransactionMessage = TransactionMessage.None,
    val blobs: List<ByteArray> = emptyList(),
    val version: Long = TransactionVersion.Default.value
) {

    val engineManifest: EngineManifest by lazy {
        EngineManifest(
            instructions = Instructions.fromString(
                string = instructions,
                networkId = networkId.toUByte()
            ),
            blobs = blobs
        )
    }

    val manifestSargon: TransactionManifest by lazy {
        TransactionManifest.init(
            instructionsString = instructions,
            networkId = NetworkId.init(discriminant = networkId.toUByte()),
            blobs = Blobs.init(blobs = blobs.map { Blob.init(it.toBagOfBytes()) })
        )
    }

    val messageSargon: Message = when (message) {
        TransactionMessage.None -> Message.None
        is TransactionMessage.Public -> Message.plaintext(message.message)
    }

    val networkIdSargon: NetworkId
        get() = NetworkId.init(discriminant = networkId.toUByte())

    fun entitiesRequiringAuth(): EntitiesRequiringAuth {
        val summary = engineManifest.summary(networkId = networkId.toUByte())

        return EntitiesRequiringAuth(
            accounts = summary.accountsRequiringAuth.map { it.addressString() },
            identities = summary.identitiesRequiringAuth.map { it.addressString() }
        )
    }

    fun feePayerCandidates(): List<AccountAddress> {
        val summary = engineManifest.summary(networkId.toUByte())
        return (summary.accountsWithdrawnFrom + summary.accountsDepositedInto + summary.accountsRequiringAuth).map {
            AccountAddress.init(it.addressString())
        }
    }

    // Currently the only method that exposes RET
    fun executionSummary(encodedReceipt: ByteArray): ExecutionSummary = engineManifest.executionSummary(networkId.toUByte(), encodedReceipt)

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
