package rdx.works.profile.data.crypto

import com.radixdlt.bip39.generateMnemonic
import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.bip39.wordlists.WORDLIST_ENGLISH
import javax.inject.Inject

/**
 * This will depend on SLIP-10 cryptographic
 */
interface Crypto {
    suspend fun generateMnemonic(): MnemonicWords
}

class CryptoImpl @Inject constructor() : Crypto {

    override suspend fun generateMnemonic(): MnemonicWords {
        return MnemonicWords(
            phrase = generateMnemonic(
                strength = ENTROPY_STRENGTH,
                wordList = WORDLIST_ENGLISH
            )
        )
    }

    companion object {
        /**
         * This will tell you how random your entropy is, the more the better it has to be 128-256 bit, multiple of 32
         */
        private const val ENTROPY_STRENGTH = 256
    }
}
