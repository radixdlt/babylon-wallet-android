package rdx.works.profile.data.repository

import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.os.storage.KeySpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.KeystoreManager
import rdx.works.core.sargon.deviceFactorSources
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MnemonicIntegrityRepository @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val keystoreManager: KeystoreManager
) {

    private val _didMnemonicIntegrityChange = MutableStateFlow(false)
    val didMnemonicIntegrityChange: Flow<Boolean> = _didMnemonicIntegrityChange.asSharedFlow()

    suspend fun checkIntegrity() {
        if (getProfileUseCase.isInitialized().not()) return
        val deviceFactorSources = getProfileUseCase().deviceFactorSources
        if (deviceFactorSources.isEmpty()) return
        // try to encrypt random string
        val keyInvalid = KeySpec.Mnemonic().checkIfPermanentlyInvalidated()
        if (keyInvalid) {
            // if we have invalid mnemonic encryption key we delete all mnemonics which we can no longer decrypt
            deviceFactorSources.forEach { deviceFactorSource ->
                mnemonicRepository.deleteMnemonic(deviceFactorSource.value.id.asGeneral())
            }
            // just for safety, removing key, although it seem that Android system delete it so it is always null
            keystoreManager.removeMnemonicEncryptionKey().onFailure {
                Timber.d(it, "Failed to delete encryption key")
            }
            _didMnemonicIntegrityChange.update { true }
        }
    }
}
