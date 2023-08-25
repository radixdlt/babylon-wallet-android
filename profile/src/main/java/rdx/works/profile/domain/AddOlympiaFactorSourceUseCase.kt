package rdx.works.profile.domain

import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.updateProfile
import javax.inject.Inject

class AddOlympiaFactorSourceUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val mnemonicRepository: MnemonicRepository
) {

    suspend operator fun invoke(mnemonicWithPassphrase: MnemonicWithPassphrase): FactorSource.FactorSourceID.FromHash {
        val olympiaFactorSource = DeviceFactorSource.olympia(mnemonicWithPassphrase)

        val mnemonicExist = mnemonicRepository.mnemonicExist(olympiaFactorSource.id)
        if (mnemonicExist) {
            return olympiaFactorSource.id
        }

        mnemonicRepository.saveMnemonic(
            key = olympiaFactorSource.id,
            mnemonicWithPassphrase = mnemonicWithPassphrase
        )
        profileRepository.updateProfile { profile ->
            profile.copy(
                factorSources = profile.factorSources + listOf(olympiaFactorSource)
            )
        }
        return olympiaFactorSource.id
    }
}
