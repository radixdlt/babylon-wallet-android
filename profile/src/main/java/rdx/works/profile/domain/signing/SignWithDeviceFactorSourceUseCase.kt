package rdx.works.profile.domain.signing

import com.radixdlt.ret.SignatureWithPublicKey
import kotlinx.coroutines.flow.first
import rdx.works.profile.ret.toEngineModel
import rdx.works.profile.data.model.deriveExtendedKey
import rdx.works.profile.data.model.extensions.updateLastUsed
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.SigningPurpose
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.domain.ProfileException
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
    ): Result<List<SignatureWithPublicKey>> {
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
                    val mnemonicExist = mnemonicRepository.mnemonicExist(deviceFactorSource.id)
                    if (mnemonicExist.not()) return Result.failure(ProfileException.NoMnemonic)
                    val mnemonic = requireNotNull(mnemonicRepository.readMnemonic(deviceFactorSource.id).getOrNull())
                    val hierarchicalDeterministicVirtualSource = factorInstance.badge
                        as? FactorInstance.Badge.VirtualSource.HierarchicalDeterministic ?: return@forEach
                    val extendedKey = mnemonic.deriveExtendedKey(
                        virtualSource = hierarchicalDeterministicVirtualSource
                    )
                    val privateKeyRET = extendedKey.keyPair.privateKey.toEngineModel()
                    val signatureWithPublicKey = privateKeyRET.signToSignatureWithPublicKey(dataToSign)
                    result.add(signatureWithPublicKey)
                    val profile = profileRepository.profile.first()
                    profileRepository.saveProfile(profile.updateLastUsed(deviceFactorSource.id))
                }
            }
        }
        return Result.success(result)
    }
}
