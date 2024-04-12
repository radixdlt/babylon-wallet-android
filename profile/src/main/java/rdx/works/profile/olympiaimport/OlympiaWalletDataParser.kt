@file:Suppress("TopLevelPropertyNaming", "MagicNumber", "MaximumLineLength", "MaxLineLength")

package rdx.works.profile.olympiaimport

import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import okio.ByteString.Companion.decodeBase64
import rdx.works.core.Identified
import rdx.works.core.compressedPublicKeyHashBytes
import rdx.works.core.decodeHex
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import rdx.works.core.AddressHelper
import rdx.works.profile.ret.crypto.PublicKey
import timber.log.Timber
import javax.inject.Inject

private const val HeaderSeparator = "]"
private const val InnerSeparator = "^"
private const val OuterSeparator = "~"
private const val EndOfAccountName = "}"
private const val AccountNameForbiddenCharsReplacement = "_"

class OlympiaWalletDataParser @Inject constructor(
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase
) {

    suspend fun parseOlympiaWalletAccountData(
        olympiaWalletDataChunks: Collection<String>,
        existingAccountHashes: Set<ByteArray> = emptySet()
    ): OlympiaWalletData? {
        val currentNetworkId = getCurrentGatewayUseCase.invoke().network.networkId()
        val headerToPayloadList = olympiaWalletDataChunks.map { payloadChunk ->
            val headerAndPayload = payloadChunk.split(HeaderSeparator)
            val headerChunks = headerAndPayload[0].split(InnerSeparator)
            PayloadHeader(headerChunks[0].toInt(), headerChunks[1].toInt(), headerChunks[2].toInt()) to headerAndPayload[1]
        }.sortedBy { it.first.payloadIndex }
        val fullPayload = headerToPayloadList.joinToString(separator = "") { it.second }
        val header = headerToPayloadList.first().first
        return if (olympiaWalletDataChunks.size == header.payloadCount) {
            try {
                val accountsToMigrate = fullPayload.split(OuterSeparator).map { singleAccountData ->
                    parseSingleAccount(singleAccountData, currentNetworkId, existingAccountHashes)
                }.toSet()
                return OlympiaWalletData(header.mnemonicWordCount, accountsToMigrate)
            } catch (e: Exception) {
                Timber.d(e)
                return null
            }
        } else {
            null
        }
    }

    private fun parseSingleAccount(
        singleAccountData: String,
        currentNetworkId: NetworkId,
        existingAccountHashes: Set<ByteArray>
    ): OlympiaAccountDetails {
        val singleAccountDataChunks = singleAccountData.split(InnerSeparator)
        val type = requireNotNull(OlympiaAccountType.from(singleAccountDataChunks[0]))
        val publicKeyHex = requireNotNull(singleAccountDataChunks[1].decodeBase64()?.hex())
        val publicKey = PublicKey.Secp256k1(publicKeyHex.decodeHex())
        val publicKeyHash = publicKeyHex.compressedPublicKeyHashBytes()
        val parsedIndex = requireNotNull(singleAccountDataChunks[2].toInt())
        val name = if (singleAccountDataChunks.size == 4) {
            singleAccountDataChunks[3]
                .replace(
                    oldValue = EndOfAccountName,
                    newValue = ""
                )
                .replace(
                    regex = Regex("[$HeaderSeparator$InnerSeparator$OuterSeparator]"),
                    replacement = AccountNameForbiddenCharsReplacement
                )
        } else {
            ""
        }.ifEmpty { "Unnamed Olympia account $parsedIndex" }

        val olympiaAddress = publicKey.deriveOlympiaAccountAddress()
        val newBabylonAddress = AddressHelper.accountAddressFromOlympia(
            olympiaAddress = olympiaAddress,
            forNetworkId = currentNetworkId.value
        )

        return OlympiaAccountDetails(
            index = parsedIndex,
            type = type,
            address = olympiaAddress,
            publicKey = publicKeyHex,
            accountName = name,
            derivationPath = DerivationPath.forLegacyOlympia(accountIndex = parsedIndex),
            alreadyImported = existingAccountHashes.containsWithEqualityCheck(publicKeyHash),
            newBabylonAddress = newBabylonAddress,
            appearanceId = parsedIndex % AccountGradientList.size
        )
    }

    fun chunkInfo(olympiaWalletDataChunks: Collection<String>): ChunkInfo? {
        val headerChunks = olympiaWalletDataChunks.firstOrNull()?.split(HeaderSeparator)?.getOrNull(0)?.split(InnerSeparator) ?: return null
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
        return olympiaWalletDataChunks.firstOrNull()?.split(HeaderSeparator)?.getOrNull(0)?.split(InnerSeparator)?.getOrNull(0)
            ?.toInt() == olympiaWalletDataChunks.size
    }
}

fun Collection<ByteArray>.containsWithEqualityCheck(value: ByteArray): Boolean {
    return this.any { it.contentEquals(value) }
}

data class OlympiaWalletData(val mnemonicWordCount: Int, val accountData: Set<OlympiaAccountDetails>)

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
    val newBabylonAddress: String,
    val appearanceId: Int,
    val alreadyImported: Boolean = false
) : Identified {
    override val identifier: String
        get() = newBabylonAddress
}

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
