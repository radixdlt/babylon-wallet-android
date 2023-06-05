package rdx.works.profile.domain

import com.radixdlt.extensions.removeLeadingZero
import kotlinx.coroutines.flow.first
import rdx.works.core.toHexString
import rdx.works.profile.data.model.compressedPublicKey
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.repository.MnemonicRepository
import javax.inject.Inject

class GenerateAuthSigningFactorInstanceUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository
) {

    suspend operator fun invoke(entity: Entity): FactorInstance {
        val factorSourceId: FactorSource.ID
        val authSigningDerivationPath = when (val securityState = entity.securityState) {
            is SecurityState.Unsecured -> {
                if (securityState.unsecuredEntityControl.authenticationSigning != null) {
                    throw ProfileException.AuthenticationSigningAlreadyExist(entity)
                }
                val transactionSigning = securityState.unsecuredEntityControl.transactionSigning
                val signingEntityDerivationPath = transactionSigning.derivationPath
                requireNotNull(signingEntityDerivationPath)
                factorSourceId = transactionSigning.factorSourceId
                if (transactionSigning.publicKey.curve == Slip10Curve.CURVE_25519) {
                    DerivationPath.authSigningDerivationPathFromCap26Path(signingEntityDerivationPath)
                } else {
                    val profile = getProfileUseCase.invoke().first()
                    val networkId = requireNotNull(profile.currentNetwork.knownNetworkId)
                    DerivationPath.authSigningDerivationPathFromBip44LikePath(networkId, signingEntityDerivationPath)
                }
            }
        }
        val factorSource = getProfileUseCase.factorSource(factorSourceId)
        requireNotNull(factorSource)
        val mnemonic = mnemonicRepository.readMnemonic(factorSource.id)
        requireNotNull(mnemonic)
        val authSigningPublicKey = mnemonic.compressedPublicKey(
            curve = Slip10Curve.CURVE_25519,
            derivationPath = authSigningDerivationPath
        ).removeLeadingZero().toHexString()
        return FactorInstance(
            authSigningDerivationPath,
            factorSourceId,
            FactorInstance.PublicKey.curve25519PublicKey(authSigningPublicKey)
        )
    }
}
