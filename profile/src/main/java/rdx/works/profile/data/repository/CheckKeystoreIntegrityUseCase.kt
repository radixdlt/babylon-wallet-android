package rdx.works.profile.data.repository

import com.radixdlt.sargon.extensions.asGeneral
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import rdx.works.core.logNonFatalException
import rdx.works.core.sargon.deviceFactorSources
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckKeystoreIntegrityUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository
) {

    private val _didMnemonicIntegrityChange = MutableSharedFlow<Boolean>(replay = 1).apply { tryEmit(false) }
    val didMnemonicIntegrityChange: Flow<Boolean> = _didMnemonicIntegrityChange.asSharedFlow()

    suspend operator fun invoke() {
        val profile = getProfileUseCase.finishedOnboardingProfile() ?: return

        val deviceFactorSources = profile.deviceFactorSources
        if (deviceFactorSources.isEmpty()) return

        try {
            mnemonicRepository.clearInvalidatedMnemonics().getOrThrow()
            val missing = deviceFactorSources.any {
                !mnemonicRepository.mnemonicExist(it.value.id.asGeneral())
            }
            // Refresh per-mnemonic status even when the aggregate result has not changed.
            _didMnemonicIntegrityChange.emit(missing)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logNonFatalException(error)
            Timber.d(error, "Failed to inspect mnemonic records; records preserved")
        }
    }
}
