@file:Suppress("TopLevelPropertyNaming", "MagicNumber", "MaximumLineLength", "MaxLineLength")

package rdx.works.profile.olympiaimport

import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.crypto.PublicKey
import com.radixdlt.toolkit.models.request.DeriveOlympiaAddressFromPublicKeyRequest
import com.radixdlt.toolkit.models.request.OlympiaNetwork
import okio.ByteString.Companion.decodeBase64
import rdx.works.core.blake2Hash
import rdx.works.profile.data.model.pernetwork.DerivationPath
import timber.log.Timber
import javax.inject.Inject

private const val HeaderSeparator = "]"
private const val InnerSeparator = "^"
private const val OuterSeparator = "~"
private const val EndOfAccountName = "}"
private const val AccountNameForbiddenCharsReplacement = "_"

class OlympiaWalletDataParser @Inject constructor() {

    fun parseOlympiaWalletAccountData(
        olympiaWalletDataChunks: Collection<String>,
        existingAccountHashes: Set<ByteArray> = emptySet()
    ): OlympiaWalletData? {
        val headerToPayloadList = olympiaWalletDataChunks.map { payloadChunk ->
            val headerAndPayload = payloadChunk.split(HeaderSeparator)
            val headerChunks = headerAndPayload[0].split(InnerSeparator)
            PayloadHeader(headerChunks[0].toInt(), headerChunks[1].toInt(), headerChunks[2].toInt()) to headerAndPayload[1]
        }.sortedBy { it.first.payloadIndex }
        val fullPayload = headerToPayloadList.joinToString(separator = "") { it.second }
        val header = headerToPayloadList.first().first
        return if (olympiaWalletDataChunks.size == header.payloadCount) {
            try {
                val accountData = fullPayload.split(OuterSeparator).map { singleAccountData ->
                    val singleAccountDataChunks = singleAccountData.split(InnerSeparator)
                    val type = requireNotNull(OlympiaAccountType.from(singleAccountDataChunks[0]))
                    val publicKeyHex = requireNotNull(singleAccountDataChunks[1].decodeBase64()?.hex())
                    val publicKey = PublicKey.EcdsaSecp256k1(publicKeyHex)
                    val index = requireNotNull(singleAccountDataChunks[2].toInt())
                    val name = if (singleAccountDataChunks.size == 4) {
                        singleAccountDataChunks[3]
                            .replace(EndOfAccountName, "")
                            .replace(Regex("[$HeaderSeparator$InnerSeparator$OuterSeparator]"), AccountNameForbiddenCharsReplacement)
                    } else {
                        ""
                    }
                    val publicKeyHash = publicKey.toByteArray().blake2Hash().takeLast(26).toByteArray()
                    val olympiaAddress = RadixEngineToolkit.deriveOlympiaAddressFromPublicKey(
                        DeriveOlympiaAddressFromPublicKeyRequest(OlympiaNetwork.Mainnet, publicKey)
                    ).getOrThrow().olympiaAccountAddress
                    OlympiaAccountDetails(
                        index = index,
                        type = type,
                        address = olympiaAddress,
                        publicKey = publicKeyHex,
                        accountName = name,
                        derivationPath = DerivationPath.forLegacyOlympia(accountIndex = index),
                        alreadyImported = existingAccountHashes.containsWithEqualityCheck(publicKeyHash)
                    )
                }
                return OlympiaWalletData(header.mnemonicWordCount, accountData)
            } catch (e: Exception) {
                Timber.d(e)
                return null
            }
        } else {
            null
        }
    }

    fun chunkInfo(olympiaWalletDataChunks: Collection<String>): ChunkInfo? {
        val headerChunks = olympiaWalletDataChunks.firstOrNull()
            ?.split(HeaderSeparator)?.getOrNull(0)?.split(InnerSeparator) ?: return null
        return ChunkInfo(olympiaWalletDataChunks.size, headerChunks[0].toInt())
    }

    @Suppress("SwallowedException")
    fun isProperQrPayload(olympiaWalletDataChunk: String): Boolean {
        return try {
            val headerAndPayload = olympiaWalletDataChunk.split(HeaderSeparator)
            val headerChunks = headerAndPayload[0].split(InnerSeparator)
            PayloadHeader(headerChunks[0].toInt(), headerChunks[1].toInt(), headerChunks[2].toInt())
            true
        } catch (e: java.lang.Exception) {
            false
        }
    }

    fun verifyPayload(olympiaWalletDataChunks: Collection<String>): Boolean {
        return olympiaWalletDataChunks.firstOrNull()
            ?.split(HeaderSeparator)?.getOrNull(0)?.split(InnerSeparator)?.getOrNull(0)?.toInt() == olympiaWalletDataChunks.size
    }
}

fun Collection<ByteArray>.containsWithEqualityCheck(value: ByteArray): Boolean {
    return this.any { it.contentEquals(value) }
}

data class OlympiaWalletData(val mnemonicWordCount: Int, val accountData: List<OlympiaAccountDetails>)

data class PayloadHeader(
    val payloadCount: Int,
    val payloadIndex: Int,
    val mnemonicWordCount: Int
)

data class ChunkInfo(
    val scanned: Int,
    val total: Int
)

data class OlympiaAccountDetails(
    val index: Int,
    val type: OlympiaAccountType,
    val address: String,
    val publicKey: String,
    val accountName: String,
    val derivationPath: DerivationPath,
    val alreadyImported: Boolean = false
)

enum class OlympiaAccountType {
    Hardware, Software;

    companion object {
        fun from(value: String): OlympiaAccountType? {
            return when (value) {
                "S" -> {
                    Software
                }
                "H" -> {
                    Hardware
                }
                else -> null
            }
        }
    }
}

var olympiaTestSeedPhrase =
    "private sight rather cloud lock pelican barrel whisper spy more artwork crucial abandon among grow guilt control wrist memory group churn hen program sauce"
