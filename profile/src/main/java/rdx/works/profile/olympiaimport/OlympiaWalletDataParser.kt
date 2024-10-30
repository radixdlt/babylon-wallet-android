@file:Suppress("TopLevelPropertyNaming", "MagicNumber", "MaximumLineLength", "MaxLineLength")

package rdx.works.profile.olympiaimport

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.Bip39WordCount
import com.radixdlt.sargon.Bip44LikePath
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.HdPathComponent
import com.radixdlt.sargon.KeySpace
import com.radixdlt.sargon.LegacyOlympiaAccountAddress
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toBabylonAddress
import com.radixdlt.sargon.extensions.wasMigratedFromLegacyOlympia
import okio.ByteString.Companion.decodeBase64
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.from
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

private const val HeaderSeparator = "]"
private const val InnerSeparator = "^"
private const val OuterSeparator = "~"
private const val EndOfAccountName = "}"
private const val AccountNameForbiddenCharsReplacement = "_"

class OlympiaWalletDataParser @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend fun parseOlympiaWalletAccountData(
        olympiaWalletDataChunks: Collection<String>
    ): OlympiaWalletData? {
        val headerToPayloadList = olympiaWalletDataChunks.map { payloadChunk ->
            val headerAndPayload = payloadChunk.split(HeaderSeparator)
            val headerChunks = headerAndPayload[0].split(InnerSeparator)
            val wordCount = Bip39WordCount.init(wordCount = headerChunks[2].toInt())
            PayloadHeader(headerChunks[0].toInt(), headerChunks[1].toInt(), wordCount) to headerAndPayload[1]
        }.sortedBy { it.first.payloadIndex }
        val fullPayload = headerToPayloadList.joinToString(separator = "") { it.second }
        val header = headerToPayloadList.first().first
        return if (olympiaWalletDataChunks.size == header.payloadCount) {
            try {
                val importedAccountAddresses = getProfileUseCase().activeAccountsOnCurrentNetwork.map { it.address }
                val accountsToMigrate = fullPayload.split(OuterSeparator).map { singleAccountData ->
                    parseSingleAccount(singleAccountData, importedAccountAddresses)
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
        importedAccountAddresses: List<AccountAddress>
    ): OlympiaAccountDetails {
        val singleAccountDataChunks = singleAccountData.split(InnerSeparator)
        val type = requireNotNull(OlympiaAccountType.from(singleAccountDataChunks[0]))
        val publicKeyHex = requireNotNull(singleAccountDataChunks[1].decodeBase64()?.hex())

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

        val publicKey = PublicKey.Secp256k1.init(hex = publicKeyHex)
        val olympiaAddress = LegacyOlympiaAccountAddress.init(publicKey)
        val newBabylonAddress = olympiaAddress.toBabylonAddress()
        val alreadyImported = importedAccountAddresses.any { it.wasMigratedFromLegacyOlympia(olympiaAddress) }

        return OlympiaAccountDetails(
            index = parsedIndex,
            type = type,
            address = olympiaAddress,
            publicKey = publicKey,
            accountName = name,
            derivationPath = DerivationPath.Bip44Like(
                Bip44LikePath.init(
                    HdPathComponent.init(
                        localKeySpace = parsedIndex.toUInt(),
                        keySpace = KeySpace.Unsecurified(isHardened = true)
                    )
                )
            ),
            alreadyImported = alreadyImported,
            newBabylonAddress = newBabylonAddress,
            appearanceId = AppearanceId.from(parsedIndex.toUInt())
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
            val wordCount = Bip39WordCount.init(wordCount = headerChunks[2].toInt())
            PayloadHeader(headerChunks[0].toInt(), headerChunks[1].toInt(), wordCount)
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

data class OlympiaWalletData(val mnemonicWordCount: Bip39WordCount, val accountData: Set<OlympiaAccountDetails>)

data class PayloadHeader(
    val payloadCount: Int,
    val payloadIndex: Int,
    val mnemonicWordCount: Bip39WordCount
)

data class ChunkInfo(
    val scanned: Int,
    val total: Int
)

data class OlympiaAccountDetails(
    val index: Int,
    val type: OlympiaAccountType,
    val address: LegacyOlympiaAccountAddress,
    val publicKey: PublicKey.Secp256k1,
    val accountName: String,
    val derivationPath: DerivationPath.Bip44Like,
    val newBabylonAddress: AccountAddress,
    val appearanceId: AppearanceId,
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
