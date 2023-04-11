package com.babylon.wallet.android.domain

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.bip39.toSeed
import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.slip10.toKey
import okio.ByteString.Companion.decodeBase64
import rdx.works.core.toHexString
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.derivation.LegacyOlympiaBIP44LikeDerivationPath
import timber.log.Timber

fun Collection<String>.parseOlympiaWalletAccountData(): OlympiaWalletData? {
    val headerSeparator = "]"
    val innerSeparator = "^"
    val outerSeparator = "~"
    val endOfAccountName = "}"
    setOf(headerSeparator, innerSeparator, outerSeparator, endOfAccountName)
    val accountNameReplacement = "_"
    val headerToPayloadList = map { payloadChunk ->
        val headerAndPayload = payloadChunk.split(headerSeparator)
        val headerChunks = headerAndPayload[0].split(innerSeparator)
        PayloadHeader(headerChunks[0].toInt(), headerChunks[1].toInt(), headerChunks[2].toInt()) to headerAndPayload[1]
    }.sortedBy { it.first.payloadIndex }
    val fullPayload = headerToPayloadList.map { it.second }.joinToString(separator = "")
    val header = headerToPayloadList.first().first
    return if (size == header.payloadCount) {
        try {
            val accountData = fullPayload.split(outerSeparator).map { singleAccountData ->
                val singleAccountDataChunks = singleAccountData.split(innerSeparator)
                val type = requireNotNull(OlympiaAccountType.from(singleAccountDataChunks[0]))
                val byte64decoded = singleAccountDataChunks[1].decodeBase64()?.hex()
                val publicKey = requireNotNull(byte64decoded)
                val index = requireNotNull(singleAccountDataChunks[2].toInt())
                val name = if (singleAccountDataChunks.size == 4) {
                    singleAccountDataChunks[3].replace(endOfAccountName, "").replace(Regex("[]^~]"), accountNameReplacement)
                } else {
                    ""
                }
                OlympiaAccountDetails(
                    index = index,
                    type = type,
                    address = "testAddress",
                    publicKey = publicKey,
                    accountName = name,
                    derivationPath = LegacyOlympiaBIP44LikeDerivationPath(index)
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

fun Collection<String>.verifyPayload(): Boolean {
    val innerSeparator = "|"
    return firstOrNull()?.split(innerSeparator)?.getOrNull(0)?.toInt() == size
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

data class OlympiaAccountDetails(
    val index: Int,
    val type: OlympiaAccountType,
    val address: String,
    val publicKey: String,
    val accountName: String,
    val derivationPath: LegacyOlympiaBIP44LikeDerivationPath
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

fun getOlympiaTestAccounts(): List<OlympiaAccountDetails> {
    val words = MnemonicWords(olympiaTestSeedPhrase)
    val seed = words.toSeed(passphrase = "")
    val accounts = (0..20).map { index ->
        val derivationPath = LegacyOlympiaBIP44LikeDerivationPath(index)
        val publicKey = seed.toKey(derivationPath.path, EllipticCurveType.Secp256k1).keyPair.getCompressedPublicKey().toHexString()
        OlympiaAccountDetails(
            index = index,
            type = if (index % 2 == 0) OlympiaAccountType.Software else OlympiaAccountType.Hardware,
            address = "1",
            publicKey = publicKey,
            accountName = "Olympia $index",
            derivationPath = derivationPath
        )
    }
    return accounts
}

val olympiaTestSeedPhrase = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong"