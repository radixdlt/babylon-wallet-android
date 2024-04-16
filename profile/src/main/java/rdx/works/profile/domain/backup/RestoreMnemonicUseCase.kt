package rdx.works.profile.domain.backup

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

class RestoreMnemonicUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(
        factorSource: FactorSource.Device,
        mnemonicWithPassphrase: MnemonicWithPassphrase
    ): Result<Unit> {
        val isValid = FactorSourceId.Hash.init(
            kind = FactorSourceKind.DEVICE,
            mnemonicWithPassphrase = mnemonicWithPassphrase
        ) == factorSource.id
        return if (isValid) {
            return mnemonicRepository.saveMnemonic(factorSource.value.id.asGeneral(), mnemonicWithPassphrase).fold(onSuccess = {
                preferencesManager.markFactorSourceBackedUp(factorSource.value.id.asGeneral())
                Result.success(Unit)
            }, onFailure = {
                Result.failure(ProfileException.SecureStorageAccess)
            })
        } else {
            Result.failure(ProfileException.InvalidMnemonic)
        }
    }
}
