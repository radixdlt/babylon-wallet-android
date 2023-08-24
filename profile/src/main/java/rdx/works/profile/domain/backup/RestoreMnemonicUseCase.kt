package rdx.works.profile.domain.backup

import com.radixdlt.extensions.removeLeadingZero
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.toHexString
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.repository.MnemonicRepository
import javax.inject.Inject

class RestoreMnemonicUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(
        factorInstance: FactorInstance,
        mnemonicWithPassphrase: MnemonicWithPassphrase
    ): Result<Unit> {
        val derivationPath = factorInstance.derivationPath ?: return Result.failure(
            Exception("Factor instance is not related to a device factor source")
        )

        val factorSourceId = (factorInstance.factorSourceId as FactorSource.FactorSourceID.FromHash)
        val isFactorSourceIdValid = FactorSource.factorSourceId(
            mnemonicWithPassphrase = mnemonicWithPassphrase
        ) == factorSourceId.body.value

        val isPublicKeyValid = mnemonicWithPassphrase.compressedPublicKey(
            derivationPath = derivationPath,
            curve = factorInstance.publicKey.curve
        ).removeLeadingZero().toHexString() == factorInstance.publicKey.compressedData

        return if (!isFactorSourceIdValid || !isPublicKeyValid) {
            Result.failure(Exception("Invalid mnemonic with passphrase"))
        } else {
            mnemonicRepository.saveMnemonic(factorSourceId, mnemonicWithPassphrase)
            preferencesManager.markFactorSourceBackedUp(factorSourceId.body.value)
            Result.success(Unit)
        }
    }
}
