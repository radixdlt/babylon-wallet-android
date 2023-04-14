@file:Suppress("TopLevelPropertyNaming", "MagicNumber", "MaximumLineLength", "MaxLineLength")

package rdx.works.profile.olympiaimport

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.bip39.toSeed
import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.slip10.toKey
import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.crypto.PublicKey
import com.radixdlt.toolkit.models.request.DeriveOlympiaAddressFromPublicKeyRequest
import com.radixdlt.toolkit.models.request.OlympiaNetwork
import okio.ByteString.Companion.decodeBase64
import rdx.works.core.blake2Hash
import rdx.works.core.toHexString
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.derivation.LegacyOlympiaBIP44LikeDerivationPath
import timber.log.Timber

private const val HeaderSeparator = "]"
private const val InnerSeparator = "^"
private const val OuterSeparator = "~"
private const val EndOfAccountName = "}"
private const val AccountNameForbiddenCharsReplacement = "_"

fun Collection<String>.parseOlympiaWalletAccountData(existingAccountHashes: Set<ByteArray> = emptySet()): OlympiaWalletData? {
    val headerToPayloadList = map { payloadChunk ->
        val headerAndPayload = payloadChunk.split(HeaderSeparator)
        val headerChunks = headerAndPayload[0].split(InnerSeparator)
        PayloadHeader(headerChunks[0].toInt(), headerChunks[1].toInt(), headerChunks[2].toInt()) to headerAndPayload[1]
    }.sortedBy { it.first.payloadIndex }
    val fullPayload = headerToPayloadList.joinToString(separator = "") { it.second }
    val header = headerToPayloadList.first().first
    return if (size == header.payloadCount) {
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
                    derivationPath = LegacyOlympiaBIP44LikeDerivationPath(index),
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

fun Collection<ByteArray>.containsWithEqualityCheck(value: ByteArray): Boolean {
    return this.any { it.contentEquals(value) }
}

fun Collection<String>.chunkInfo(): ChunkInfo? {
    val headerChunks = firstOrNull()?.split(HeaderSeparator)?.getOrNull(0)?.split(InnerSeparator) ?: return null
    return ChunkInfo(size, headerChunks[0].toInt())
}

@Suppress("SwallowedException")
fun String.isProperQrPayload(): Boolean {
    return try {
        val headerAndPayload = split(HeaderSeparator)
        val headerChunks = headerAndPayload[0].split(InnerSeparator)
        PayloadHeader(headerChunks[0].toInt(), headerChunks[1].toInt(), headerChunks[2].toInt())
        true
    } catch (e: java.lang.Exception) {
        false
    }
}

fun Collection<String>.verifyPayload(): Boolean {
    return firstOrNull()?.split(HeaderSeparator)?.getOrNull(0)?.split(InnerSeparator)?.getOrNull(0)?.toInt() == size
}

fun MnemonicWithPassphrase.validatePublicKeysOf(accounts: List<OlympiaAccountDetails>): Boolean {
    val words = MnemonicWords(mnemonic)
    val seed = words.toSeed(passphrase = bip39Passphrase)
    accounts.forEach { account ->
        val derivedPublicKey =
            seed.toKey(account.derivationPath.path, EllipticCurveType.Secp256k1).keyPair.getCompressedPublicKey().toHexString()
        if (derivedPublicKey != account.publicKey) {
            return false
        }
    }
    return true
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
    val derivationPath: LegacyOlympiaBIP44LikeDerivationPath,
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
