package rdx.works.profile.domain.backup

import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.init
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

class ImportMnemonicUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(
        factorSourceId: FactorSourceId.Hash,
        mnemonicWithPassphrase: MnemonicWithPassphrase
    ): Result<Unit> {
        val isValid = FactorSourceId.Hash.init(
            kind = FactorSourceKind.DEVICE,
            mnemonicWithPassphrase = mnemonicWithPassphrase
        ) == factorSourceId
        return if (isValid) {
            return mnemonicRepository.saveMnemonic(factorSourceId, mnemonicWithPassphrase).fold(onSuccess = {
                preferencesManager.markFactorSourceBackedUp(factorSourceId)
                Result.success(Unit)
            }, onFailure = {
                Result.failure(ProfileException.SecureStorageAccess)
            })
        } else {
            Result.failure(ProfileException.InvalidMnemonic)
        }
    }
}
