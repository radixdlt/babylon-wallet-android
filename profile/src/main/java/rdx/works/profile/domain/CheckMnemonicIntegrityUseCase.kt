package rdx.works.profile.domain

import com.radixdlt.sargon.extensions.asGeneral
import rdx.works.core.KeySpec
import rdx.works.core.KeystoreManager
import rdx.works.core.UUIDGenerator
import rdx.works.core.checkIfKeyWasPermanentlyInvalidated
import rdx.works.core.sargon.deviceFactorSources
import rdx.works.profile.data.repository.MnemonicRepository
import timber.log.Timber
import javax.inject.Inject

class CheckMnemonicIntegrityUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val keystoreManager: KeystoreManager
) {

    suspend operator fun invoke() {
        if (getProfileUseCase.isInitialized().not()) return
        val deviceFactorSources = getProfileUseCase().deviceFactorSources
        if (deviceFactorSources.isEmpty()) return
        // try to encrypt random string
        val keyInvalid = checkIfKeyWasPermanentlyInvalidated(UUIDGenerator.uuid().toString(), KeySpec.Mnemonic())
        if (keyInvalid) {
            // if we have invalid mnemonic encryption key we delete all mnemonics which we can no longer decrypt
            deviceFactorSources.forEach { deviceFactorSource ->
                mnemonicRepository.deleteMnemonic(deviceFactorSource.value.id.asGeneral())
            }
            // just for safety, removing key, although it seem that Android system delete it so it is always null
            keystoreManager.removeMnemonicEncryptionKey().onFailure {
                Timber.d(it, "Failed to delete encryption key")
            }
        }
    }
}
