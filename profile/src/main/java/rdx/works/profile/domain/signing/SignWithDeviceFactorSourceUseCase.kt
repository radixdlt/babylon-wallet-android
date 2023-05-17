package rdx.works.profile.domain.signing

import com.radixdlt.toolkit.models.crypto.SignatureWithPublicKey
import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.SigningEntity
import rdx.works.profile.data.model.deriveExtendedKey
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.updateLastUsed
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.utils.toEngineModel
import javax.inject.Inject

class SignWithDeviceFactorSourceUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(
        deviceFactorSource: FactorSource,
        signers: List<SigningEntity>,
        dataToSign: ByteArray
    ): List<SignatureWithPublicKey> {
        val result = mutableListOf<SignatureWithPublicKey>()
        val mnemonic = requireNotNull(mnemonicRepository.readMnemonic(deviceFactorSource.id))
        signers.forEach { signer ->
            when (val securityState = signer.securityState) {
                is SecurityState.Unsecured -> {
                    val extendedKey = mnemonic.deriveExtendedKey(
                        factorInstance = securityState.unsecuredEntityControl.genesisFactorInstance
                    )
                    val privateKeyRET = extendedKey.keyPair.privateKey.toEngineModel()
                    val signatureWithPublicKey = privateKeyRET.signToSignatureWithPublicKey(dataToSign)
                    result.add(signatureWithPublicKey)
                }
            }
        }
        val profile = profileRepository.profile.first()
        profileRepository.saveProfile(profile.updateLastUsed(deviceFactorSource.id))
        return result
    }
}
