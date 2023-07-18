package rdx.works.profile.domain.signing

import com.radixdlt.ret.SignatureWithPublicKey
import kotlinx.coroutines.flow.first
import rdx.works.core.ret.toEngineModel
import rdx.works.profile.data.model.deriveExtendedKey
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.SigningPurpose
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.utils.updateLastUsed
import javax.inject.Inject

class SignWithDeviceFactorSourceUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(
        deviceFactorSource: DeviceFactorSource,
        signers: List<Entity>,
        dataToSign: ByteArray,
        signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
    ): List<SignatureWithPublicKey> {
        val result = mutableListOf<SignatureWithPublicKey>()

        signers.forEach { signer ->
            when (val securityState = signer.securityState) {
                is SecurityState.Unsecured -> {
                    val factorInstance = when (signingPurpose) {
                        SigningPurpose.SignAuth ->
                            securityState.unsecuredEntityControl.authenticationSigning
                                ?: securityState.unsecuredEntityControl.transactionSigning
                        SigningPurpose.SignTransaction -> securityState.unsecuredEntityControl.transactionSigning
                    }
                    val mnemonic = requireNotNull(mnemonicRepository.readMnemonic(deviceFactorSource.id))
                    val extendedKey = mnemonic.deriveExtendedKey(
                        factorInstance = factorInstance
                    )
                    val privateKeyRET = extendedKey.keyPair.privateKey.toEngineModel()
                    val signatureWithPublicKey = privateKeyRET.signToSignatureWithPublicKey(dataToSign)
                    result.add(signatureWithPublicKey)
                    val profile = profileRepository.profile.first()
                    profileRepository.saveProfile(profile.updateLastUsed(deviceFactorSource.id))
                }
            }
        }
        return result
    }
}
