package rdx.works.core.domain

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.BagOfBytes
import com.radixdlt.sargon.Blob
import com.radixdlt.sargon.Blobs
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Message
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.blobs
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.executionSummary
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.instructionsString
import com.radixdlt.sargon.extensions.networkId
import com.radixdlt.sargon.extensions.plaintext
import com.radixdlt.sargon.extensions.summary
import com.radixdlt.sargon.extensions.toList

data class TransactionManifestData(
    val instructions: String,
    val networkId: NetworkId,
    val message: TransactionMessage = TransactionMessage.None,
    val blobs: List<BagOfBytes> = emptyList(),
    val version: Long = TransactionVersion.Default.value
) {

    val manifestSargon: TransactionManifest by lazy {
        TransactionManifest.init(
            instructionsString = instructions,
            networkId = networkId,
            blobs = Blobs.init(blobs = blobs.map { Blob.init(it) })
        )
    }

    val messageSargon: Message = when (message) {
        TransactionMessage.None -> Message.None
        is TransactionMessage.Public -> Message.plaintext(message.message)
    }

    fun entitiesRequiringAuth(): EntitiesRequiringAuth {
        val summary = manifestSargon.summary

        return EntitiesRequiringAuth(
            accounts = summary.addressesOfAccountsRequiringAuth,
            identities = summary.addressesOfPersonasRequiringAuth
        )
    }

    fun feePayerCandidates(): List<AccountAddress> {
        val summary = manifestSargon.summary
        return summary.addressesOfAccountsWithdrawnFrom +
            summary.addressesOfAccountsDepositedInto +
            summary.addressesOfAccountsRequiringAuth
    }

    // Currently the only method that exposes RET
    fun executionSummary(
        radixEngineToolkitReceipt: String
    ): ExecutionSummary = manifestSargon.executionSummary(radixEngineToolkitReceipt)

    sealed interface TransactionMessage {

        val messageOrNull: String?
            get() = when (this) {
                None -> null
                is Public -> message
            }

        data object None : TransactionMessage
        data class Public(val message: String) : TransactionMessage
    }

    data class EntitiesRequiringAuth(
        val accounts: List<AccountAddress>,
        val identities: List<IdentityAddress>
    )

    companion object {
        fun from(
            manifest: TransactionManifest,
            message: TransactionMessage = TransactionMessage.None
        ) = TransactionManifestData(
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
