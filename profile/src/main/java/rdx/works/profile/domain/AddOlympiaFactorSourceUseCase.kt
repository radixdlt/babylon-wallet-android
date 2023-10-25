package rdx.works.profile.domain

import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.toIdentifiedArrayList
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.updateProfile
import javax.inject.Inject

class AddOlympiaFactorSourceUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val profileRepository: ProfileRepository,
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager
) {

    suspend operator fun invoke(mnemonicWithPassphrase: MnemonicWithPassphrase): FactorSource.FactorSourceID.FromHash {
        val olympiaFactorSource = DeviceFactorSource.olympia(mnemonicWithPassphrase)
        val existingFactorSource = getProfileUseCase.factorSourceById(olympiaFactorSource.id)
        val mnemonicExist = mnemonicRepository.mnemonicExist(olympiaFactorSource.id)
        if (mnemonicExist) {
            return olympiaFactorSource.id
        }
        // factor source exist without mnemonic, update mnemonic
        mnemonicRepository.saveMnemonic(
            key = olympiaFactorSource.id,
            mnemonicWithPassphrase = mnemonicWithPassphrase
        )
        // Seed phrase was just typed by the user, mark it as backed up
        preferencesManager.markFactorSourceBackedUp(olympiaFactorSource.id.body.value)
        // we save factor source id only if it does not exist
        if (existingFactorSource == null) {
            profileRepository.updateProfile { profile ->
                profile.copy(
                    factorSources = (profile.factorSources + olympiaFactorSource).toIdentifiedArrayList()
                )
            }
        }
        return olympiaFactorSource.id
    }
}
