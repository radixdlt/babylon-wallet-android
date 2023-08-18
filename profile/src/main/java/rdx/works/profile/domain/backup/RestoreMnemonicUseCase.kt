package rdx.works.profile.domain.backup

import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.repository.MnemonicRepository
import javax.inject.Inject

class RestoreMnemonicUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager
) {

    suspend operator fun invoke(
        factorSourceId: FactorSource.FactorSourceID.FromHash,
        mnemonicWithPassphrase: MnemonicWithPassphrase
    ) {
        mnemonicRepository.saveMnemonic(factorSourceId, mnemonicWithPassphrase)
        preferencesManager.markFactorSourceBackedUp(factorSourceId.body.value)
    }
}
