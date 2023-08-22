package rdx.works.profile.domain

import kotlinx.coroutines.flow.firstOrNull
import rdx.works.core.KeySpec
import rdx.works.core.KeystoreManager
import rdx.works.core.UUIDGenerator
import rdx.works.core.checkIfKeyWasPermanentlyInvalidated
import rdx.works.profile.data.repository.MnemonicRepository
import timber.log.Timber
import javax.inject.Inject

class CheckMnemonicIntegrityUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val keystoreManager: KeystoreManager
) {

    suspend operator fun invoke() {
        val deviceFactorSources = getProfileUseCase.deviceFactorSources.firstOrNull().orEmpty()
        // try to encrypt random string
        if (deviceFactorSources.isEmpty()) return
        val keyInvalid = checkIfKeyWasPermanentlyInvalidated(UUIDGenerator.uuid().toString(), KeySpec.Mnemonic())
        if (keyInvalid) {
            Timber.d("Mnemonic security: invalid key")
            // if we have invalid mnemonic encryption key we delete all mnemonics which we can no longer decrypt
            deviceFactorSources.forEach { deviceFactorSource ->
                mnemonicRepository.deleteMnemonic(deviceFactorSource.id)
            }
            // just for safety, removing key, although it seem that Android system delete it so it is always null
            keystoreManager.removeMnemonicEncryptionKey()
        }
    }
}
