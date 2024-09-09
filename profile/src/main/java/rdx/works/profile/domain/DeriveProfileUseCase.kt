package rdx.works.profile.domain

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.ProfileState
import com.radixdlt.sargon.extensions.Accounts
import com.radixdlt.sargon.extensions.asGeneral
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import rdx.works.core.di.DefaultDispatcher
import rdx.works.core.mapError
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.os.SargonOsManager
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class DeriveProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager,
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        deviceFactorSource: FactorSource.Device,
        mnemonicWithPassphrase: MnemonicWithPassphrase,
        accounts: Accounts
    ): Result<Unit> = when (profileRepository.profileState.first()) {
        is ProfileState.Loaded -> Result.success(Unit)
        else -> withContext(defaultDispatcher) {
            val sargonOs = sargonOsManager.sargonOs.firstOrNull() ?:
                return@withContext Result.failure(RuntimeException("Sargon os not booted"))

            mnemonicRepository.saveMnemonic(
                deviceFactorSource.value.id.asGeneral(),
                mnemonicWithPassphrase
            ).mapError {
                ProfileException.SecureStorageAccess
            }.mapCatching {
                sargonOs.deriveWallet(
                    deviceFactorSource = deviceFactorSource.value,
                    accounts = accounts.asList()
                )
            }.onSuccess {
                preferencesManager.markFactorSourceBackedUp(deviceFactorSource.value.id.asGeneral())
            }
        }
    }
}
