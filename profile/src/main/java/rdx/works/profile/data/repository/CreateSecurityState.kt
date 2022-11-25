package rdx.works.profile.data.repository

import rdx.works.profile.data.model.factorsources.FactorSources
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.SecurityState

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
