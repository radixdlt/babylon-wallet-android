package rdx.works.profile

import com.radixdlt.bip39.model.MnemonicWords
import rdx.works.profile.data.model.factorsources.FactorSource

fun factorSourceId(fromPhrase: String): String{
    val mnemonic = MnemonicWords(fromPhrase)
    return FactorSource.factorSourceId(mnemonic = mnemonic)
}

fun factorSource(fromPhrase: String): FactorSource {
    return FactorSource.babylon(mnemonic = MnemonicWords(fromPhrase))
}

fun factorSources(fromPhrase: String) = listOf(factorSource(fromPhrase))
