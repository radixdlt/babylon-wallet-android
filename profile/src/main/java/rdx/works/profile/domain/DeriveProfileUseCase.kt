package rdx.works.profile.domain

import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.PrivateHierarchicalDeterministicFactorSource
import com.radixdlt.sargon.ProfileState
import com.radixdlt.sargon.extensions.Accounts
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.babylon
import com.radixdlt.sargon.extensions.factorSourceId
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.KeystoreManager
import rdx.works.core.di.DefaultDispatcher
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class DeriveProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager,
    private val sargonOsManager: SargonOsManager,
    private val keystoreManager: KeystoreManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        mnemonicWithPassphrase: MnemonicWithPassphrase,
        accounts: Accounts
    ): Result<Unit> = when (profileRepository.profileState.first()) {
        is ProfileState.Loaded -> Result.success(Unit)
        else -> withContext(defaultDispatcher) {
            val sargonOs = sargonOsManager.sargonOs

            keystoreManager.resetMnemonicKeySpecWhenInvalidated()

            runCatching {
                val hostInfo = sargonOs.resolveHostInfo()

                sargonOs.newWalletWithDerivedBdfs(
                    hdFactorSource = PrivateHierarchicalDeterministicFactorSource.babylon(
                        mnemonicWithPassphrase = mnemonicWithPassphrase,
                        hostInfo = hostInfo
                    ),
                    accounts = accounts.asList()
                )
            }.onSuccess {
                val factorSourceId = mnemonicWithPassphrase.factorSourceId(kind = FactorSourceKind.DEVICE).asGeneral()
                preferencesManager.markFactorSourceBackedUp(factorSourceId)
            }
        }
    }
}
