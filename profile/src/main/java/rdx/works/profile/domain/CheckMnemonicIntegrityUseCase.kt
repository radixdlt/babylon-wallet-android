package rdx.works.profile.domain

import kotlinx.coroutines.flow.firstOrNull
import rdx.works.core.KeySpec
import rdx.works.core.KeystoreManager
import rdx.works.core.UUIDGenerator
import rdx.works.core.checkIfKeyWasPermanentlyInvalidated
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceFlag
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
        val deviceFactorSources = getProfileUseCase.deviceFactorSources.firstOrNull().orEmpty()
        if (deviceFactorSources.isEmpty()) return
        // try to encrypt random string
        val keyInvalid = checkIfKeyWasPermanentlyInvalidated(UUIDGenerator.uuid().toString(), KeySpec.Mnemonic())
        if (keyInvalid) {
            // if we have invalid mnemonic encryption key we delete all mnemonics which we can no longer decrypt
            deviceFactorSources.forEach { deviceFactorSource ->
                mnemonicRepository.deleteMnemonic(deviceFactorSource.id)
            }
            // just for safety, removing key, although it seem that Android system delete it so it is always null
            keystoreManager.removeMnemonicEncryptionKey().onFailure {
                Timber.d(it, "Failed to delete encryption key")
            }
        }
    }

    suspend fun babylonMnemonicNeedsRecovery(): FactorSource.FactorSourceID.FromHash? {
        val babylonFactorSources =
            getProfileUseCase.deviceFactorSources.firstOrNull()?.filter { it.supportsBabylon }
                ?: return null
        val mainBabylonFactorSourceToRecover = if (babylonFactorSources.size == 1) {
            babylonFactorSources.first()
        } else {
            babylonFactorSources.firstOrNull { it.common.flags.contains(FactorSourceFlag.Main) }
        } ?: return null
        return if (mnemonicRepository.mnemonicExist(mainBabylonFactorSourceToRecover.id).not()) {
            mainBabylonFactorSourceToRecover.id
        } else {
            null
        }
    }
}
