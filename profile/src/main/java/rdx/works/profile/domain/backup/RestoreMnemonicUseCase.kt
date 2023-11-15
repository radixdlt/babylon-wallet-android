package rdx.works.profile.domain.backup

import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.pernetwork.validateAgainst
import rdx.works.profile.data.repository.MnemonicRepository
import javax.inject.Inject

class RestoreMnemonicUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(
        factorSource: DeviceFactorSource,
        mnemonicWithPassphrase: MnemonicWithPassphrase
    ): Result<Unit> {
        val isValid = factorSource.validateAgainst(mnemonicWithPassphrase)
        return if (isValid) {
            mnemonicRepository.saveMnemonic(factorSource.id, mnemonicWithPassphrase)
            preferencesManager.markFactorSourceBackedUp(factorSource.id.body.value)
            Result.success(Unit)
        } else {
            Result.failure(Exception("Invalid mnemonic with passphrase"))
        }
    }
}
