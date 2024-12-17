package rdx.works.profile.data.repository

import com.radixdlt.sargon.extensions.asGeneral
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.KeystoreManager
import rdx.works.core.logNonFatalException
import rdx.works.core.sargon.deviceFactorSources
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckKeystoreIntegrityUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val keystoreManager: KeystoreManager
) {

    private val _didMnemonicIntegrityChange = MutableStateFlow(false)
    val didMnemonicIntegrityChange: Flow<Boolean> = _didMnemonicIntegrityChange.asSharedFlow()

    suspend operator fun invoke() {
        val profile = getProfileUseCase.finishedOnboardingProfile() ?: return

        val deviceFactorSources = profile.deviceFactorSources
        if (deviceFactorSources.isEmpty()) return

        keystoreManager.resetMnemonicKeySpecWhenInvalidated()
            .onSuccess { invalidated ->
                if (invalidated) {
                    deviceFactorSources.forEach { deviceFactorSource ->
                        mnemonicRepository.deleteMnemonic(deviceFactorSource.value.id.asGeneral())
                    }

                    _didMnemonicIntegrityChange.update { true }
                }
            }.onFailure {
                logNonFatalException(it)
                Timber.d(it, "Failed to regenerate encryption key")
            }
    }
}
