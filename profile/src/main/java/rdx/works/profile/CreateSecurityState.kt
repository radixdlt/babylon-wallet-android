package rdx.works.profile

import rdx.works.profile.model.factorsources.FactorSources
import rdx.works.profile.model.pernetwork.DerivationPath
import rdx.works.profile.model.pernetwork.SecurityState

interface CreateSecurityState {
    fun create(
        derivationPath: DerivationPath,
        compressedPublicKey: ByteArray
    ): SecurityState
}

class UnsecuredSecurityState(
    private val factorSources: FactorSources
) : CreateSecurityState {

    override fun create(
        derivationPath: DerivationPath,
        compressedPublicKey: ByteArray
    ): SecurityState {
        return SecurityState.unsecuredSecurityState(
            compressedPublicKey = compressedPublicKey,
            derivationPath = derivationPath,
            factorSources = factorSources
        )
    }
}
