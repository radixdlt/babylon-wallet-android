package rdx.works.profile.domain.backup

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.asGeneral
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.validateAgainst
import rdx.works.profile.data.repository.MnemonicRepository
import javax.inject.Inject

class RestoreMnemonicUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(
        factorSource: FactorSource.Device,
        mnemonicWithPassphrase: MnemonicWithPassphrase
    ): Result<Unit> {
        val isValid = mnemonicWithPassphrase.validateAgainst(factorSource)
        return if (isValid) {
            mnemonicRepository.saveMnemonic(factorSource.value.id.asGeneral(), mnemonicWithPassphrase)
            preferencesManager.markFactorSourceBackedUp(factorSource.value.id.asGeneral())
            Result.success(Unit)
        } else {
            Result.failure(Exception("Invalid mnemonic with passphrase"))
        }
    }
}
