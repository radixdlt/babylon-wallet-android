package rdx.works.profile.domain

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class AddOlympiaFactorSourceUseCase @Inject constructor(
    private val dataSource: ProfileRepository,
    private val mnemonicRepository: MnemonicRepository
) {

    suspend operator fun invoke(mnemonic: String, passphrase: String = "") {
        val mnemonicWithPassphrase = MnemonicWithPassphrase(mnemonic, passphrase)
        val olympiaFactorSource = FactorSource.olympia(mnemonicWithPassphrase)
        val existingMnemonic = mnemonicRepository.readMnemonic(olympiaFactorSource.id)
        if (existingMnemonic != null) return
        mnemonicRepository.saveMnemonic(olympiaFactorSource.id, mnemonicWithPassphrase)
        val profile = dataSource.profile.first()
        val updatedProfile = profile.copy(factorSources = profile.factorSources + listOf(olympiaFactorSource))
        dataSource.saveProfile(updatedProfile)
    }
}
