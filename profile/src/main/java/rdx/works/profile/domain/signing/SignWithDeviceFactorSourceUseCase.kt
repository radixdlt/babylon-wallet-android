package rdx.works.profile.domain.signing

import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import kotlinx.coroutines.flow.first
import rdx.works.core.sargon.ProfileEntity
import rdx.works.core.sargon.derivePrivateKey
import rdx.works.core.sargon.updateLastUsed
import rdx.works.core.domain.SigningPurpose
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
        deviceFactorSource: FactorSource.Device,
        signers: List<ProfileEntity>,
        dataToSign: ByteArray,
        signingPurpose: SigningPurpose = SigningPurpose.SignTransaction
    ): Result<List<SignatureWithPublicKey>> {
        val result = mutableListOf<SignatureWithPublicKey>()

        signers.forEach { signer ->
            when (val securityState = signer.securityState) {
                is EntitySecurityState.Unsecured -> {
                    val factorInstance = when (signingPurpose) {
                        SigningPurpose.SignAuth -> securityState.value.authenticationSigning ?: securityState.value.transactionSigning
                        SigningPurpose.SignTransaction -> securityState.value.transactionSigning
                    }
                    val mnemonicExist = mnemonicRepository.mnemonicExist(deviceFactorSource.value.id.asGeneral())
                    if (mnemonicExist.not()) return Result.failure(ProfileException.NoMnemonic)
                    val mnemonic = requireNotNull(mnemonicRepository.readMnemonic(deviceFactorSource.value.id.asGeneral()).getOrNull())

                    val signatureWithPublicKey = mnemonic
                        .derivePrivateKey(hdPublicKey = factorInstance.publicKey)
                        .signToSignatureWithPublicKey(dataToSign)

                    result.add(signatureWithPublicKey)
                    val profile = profileRepository.profile.first()
                    profileRepository.saveProfile(profile.updateLastUsed(deviceFactorSource.id))
                }
            }
        }
        return Result.success(result)
    }
}
