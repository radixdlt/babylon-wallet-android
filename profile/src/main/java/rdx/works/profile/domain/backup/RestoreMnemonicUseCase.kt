package rdx.works.profile.domain.backup

import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.validateAgainst
import rdx.works.profile.data.repository.MnemonicRepository
import javax.inject.Inject

class RestoreMnemonicUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(
        factorInstance: FactorInstance,
        mnemonicWithPassphrase: MnemonicWithPassphrase
    ): Result<Unit> {
        val isValid = factorInstance.validateAgainst(mnemonicWithPassphrase)

        val factorSourceId = factorInstance.factorSourceId as? FactorSource.FactorSourceID.FromHash

        return if (isValid && factorSourceId != null) {
            mnemonicRepository.saveMnemonic(factorSourceId, mnemonicWithPassphrase)
            preferencesManager.markFactorSourceBackedUp(factorSourceId.body.value)
            Result.success(Unit)
        } else {
            Result.failure(Exception("Invalid mnemonic with passphrase"))
        }
    }
}
