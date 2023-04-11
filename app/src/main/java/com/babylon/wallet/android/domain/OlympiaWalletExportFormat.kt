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
    val reservedCharacters = setOf(headerSeparator, innerSeparator, outerSeparator, endOfAccountName)
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
                val byte64decoded = singleAccountDataChunks[1].replace("\\", "").decodeBase64()?.hex()
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

val olympiaTestPayloadChunks = listOf(
    "2|0|12^S|AvZppDAk2Q/eaTUczFMCLC+GcI2bPEJpNkBzPFd4I12l|0|With forbidden char in name}~H|A/YzLtwqoPA1w8VNdKOs7HbZtZherdzamVuX1BF3Bdez|",
    "2|1|12^1|}~H|A1STin2yF+LmEPU4mZarY6Bw+0QUZk350V4nWupv5JfG|2|Third account}"
)

val olympiaTestPayloadChunks2 = listOf(
    "35^0^15]H^A91WZuV+X+T02ZHd1I/1P0YDUCFZxn7WD1SCRdAChjUL^0^Numbers are allowed 0123456789}~H^A/tBQEpt83z2+3oVR",
    "35^1^15]pyDE78C6C2+EtWaNORbCyxIiMfT^1^}~H^A4sYTa6CmTonf0Nj7lZ/WWjkf3+kHOYJfi4LQcOxxLKy^2^}~H^AhsOFbv/pmTQD38",
    "35^2^15]68VOBtXWompvbSoa0VJicZLDnj4me^3^_}~H^A1uE9vKLr3KhBdrREK/pAtiXZRqrvrQpfGRVNcZsrAVs^4^Main account.}~H",
    "35^3^15]^ArMOTtWVkq4hpO4RrBNo2QvLN4StyiKxRLSoODNuF5hH^5^Saving's account.}~H^AjZ7GPQmRfkPyFWx8JXHyj/heTGX0cR",
    "35^4^15]ExKQ4YpP7Mddz^6^Olympia is a small town in Eli}~H^AygkOvTWhbkEIULZakuYM7Sd7qybWHhfnL71ixpQRC7s^7^Ale",
    "35^5^15]xandria is the second large}~H^Am5+qfiUP5DEvZDPfKzZsJLYm0Ax3l/czbb+q/3r/656^8^Forbidden _|_|_|_}~H^A",
    "35^6^15]gxU2XrLURRMF2caN4PR0vgnpeUY2lgVTd5z9Woz/D96^9^OK +?-,.\\)[\\(!#$%{&/*<>=OK}~H^Azt9CV6Le0Si4Gr3QsymV7oY",
    "35^7^15]WQ1NlndGEliCTUQ+uEmk^10^Numbers are allowed 0123456789}~H^AhoJkUm9q3S5bb2mk3E9/6xQcsrNGI5w02HE/YxU/I",
    "35^8^15]0o^11^}~H^AgM3T2iqxZAEvsIH93rli2JUfYnKyvYTDBWnooZMJOgZ^12^}~H^Aoy+92nxm2Kxjs9dNO0SkwFMBVD2hh9Id8CF4Q",
    "35^9^15]faqrww^13^_}~H^A9XvkFRIvRTW4nqYuCoYSN0RacYU4t4hbefD1swK6y26^14^Main account.}~H^AvDV8N0OPkSxbNk3izXC",
    "35^10^15]0FVyrPdRY2ROXajCMD3pBuhk^15^Saving's account.}~H^A4Vx5gD4GTQNWf9igZDn/+UTbpInE6yPx4Cq0Bxf7MAN^16^Oly",
    "35^11^15]mpia is a small town in Eli}~H^AxKxb88CoOFaiEhoPDpSqXzLlnHkq3ETcSGsWTpVmSoN^17^Alexandria is the sec",
    "35^12^15]ond large}~H^Au1S883ZgVguQFyJQn8eCoBCDc5YYNurM316aaE1ZqtX^18^Forbidden _|_|_|_}~H^AnhW5wvIYkZ36KHwjz",
    "35^13^15]Y6KqvUGlsxqPRLP5edZOUxF9gS^19^OK +?-,.\\)[\\(!#$%{&/*<>=OK}~H^AyPj6KeJqmH6yjmIjdNYA0V9pvcQojuIrVBiUVDi",
    "35^14^15]XloL^20^Numbers are allowed 0123456789}~H^An7CdznBuX9tA/KY/EXP+Hv8QaAHJnixw9wlhC5+WxWl^21^}~H^A9Mhfl",
    "35^15^15]msyJ6g+S/zBp5oMCWW0UJPorZd4A4lPobtnyZW^22^}~H^Ap3GBlYDOUrIQo1jGj4s+WaCC+Hi3FUzoGpJjXYU0EDa^23^_}~H^A",
    "35^16^15]xVWPUtjU36+V1mtrA1qLAteBe15xgZz5jdQu87K7ixS^24^Main account.}~H^A56OgSAKAplPrBIHHuBjOZ6UihPzjo1u4Vta",
    "35^17^15]Ac1TJfYa^25^Saving's account.}~H^Auc2l7479yCj9EqL6AM4h/aiLIc3e/xBvkv6WHcZFMcE^26^Olympia is a small ",
    "35^18^15]town in Eli}~H^A3iLhFiZxRGbPJ4mGOOBRdn7Nncu1aF23OGjcm6UHZMU^27^Alexandria is the second large}~H^Ay3",
    "35^19^15]SQAnL/2w7owoiJmaRdzkqyI5HCO7LEZlrFttcfMJt^28^Forbidden _|_|_|_}~H^AgbxRo9r01at8My9Rx/aN2uvJtvYVOVS+g",
    "35^20^15]VqmUSWzF7F^29^OK +?-,.\\)[\\(!#$%{&/*<>=OK}~H^A7I/WmM9oCzqE7NG/lI5LWKeCKtSgnlRLB4qCIyR+wLk^30^Numbers ",
    "35^21^15]are allowed 0123456789}~H^Al+7thFVUM2QiC4V/x0al9GH8Q6Roa0sJTPHKgKHje4f^31^}~H^An+gCOkmDJczYOceIF9HXp",
    "35^22^15]TRnanscKLm6Mq5aQ9r8zz2^32^}~H^Azdt96RVU4krwHtAM2+oCZIsmVlMsVmQCJsYiq9OY+3C^33^_}~H^AlFhUcuSi2wQQaMYE",
    "35^23^15]lNBdDULs4BthwgEVn16gFEEB2F9^34^Main account.}~H^AuVMRbeG4bPI32tmIgIcNbcXNYmKtYAwaPBzhtJ4inDK^35^Savi",
    "35^24^15]ng's account.}~H^A3UclZo0kAsm0J+JoVVTqufq03dQ+zqsjMiTDK8rGAjm^36^Olympia is a small town in Eli}~H^A",
    "35^25^15]90nMDwcwo560EkZmLUdQLv7kOlOYXxbmctx2oJMFlge^37^Alexandria is the second large}~H^A9Fv0zV9p1zM5SFMZan",
    "35^26^15]ChjMHS5VlnPy3G1a4APa1qNW7^38^Forbidden _|_|_|_}~H^AsH/id4D/UeFrJ+DT87+9GsGs6BPVSrbgysjfC7EmeEb^39^OK",
    "35^27^15] +?-,.\\)[\\(!#$%{&/*<>=OK}~H^AsWMSuO7T+eNzWbn66iz3NX88YzVmKa7gPKKGPXK1uL6^40^Numbers are allowed 0123",
    "35^28^15]456789}~H^A5HFH/o0qj5dw//O6iLmZtjMgwiUVPgo8ywOkzBeYjpA^41^}~H^A3C1Be6m9a0IhW8Zejg3HI/Mioh4XhnkW7uxtC",
    "35^29^15]79TSfP^42^}~H^Axwz//E04aSFnM48XYtsE6Q2hfD+T+7L02960wb9CiBH^43^_}~H^AriZ5GItpD8RZGeFqTsmzl6yKjn4BY412",
    "35^30^15]plZxEJ39Q6f^44^Main account.}~H^AgnCkFbLkV4qGiKyIoJ54qOq2IWbkWk2KyYnGzAUpbXr^45^Saving's account.}~H",
    "35^31^15]^A8yvIEjllMzr8uF+hLSCiD9gNy6N0QQyCn3wJ2+UNaZ0^46^Olympia is a small town in Eli}~H^AuJ7/l1+BWLPUu2w7",
    "35^32^15]iSW7X9Ge/eU0qyWXnJBVGbUKkXk^47^Alexandria is the second large}~H^Ajwl0P/Ix0dQdmptAli/U6NYCVgNELN17pC",
    "35^33^15]q15+XageT^48^Forbidden _|_|_|_}~H^Ai3a4hVA1yKM+M9UG6IPe2gjiGD1P1xasGUfcOEjxntY^49^OK +?-,.\\)[\\(!#$%{",
    "35^34^15]&/*<>=OK}"
)

val olympiaTestSeedPhrase = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong"