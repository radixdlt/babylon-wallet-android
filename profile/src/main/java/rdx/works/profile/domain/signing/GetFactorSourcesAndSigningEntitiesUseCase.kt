package rdx.works.profile.domain.signing

import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.SigningEntity
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.factorSource
import javax.inject.Inject

class GetFactorSourcesAndSigningEntitiesUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(
        signers: List<SigningEntity>
    ): Map<FactorSource, List<SigningEntity>> {
        val result = mutableMapOf<FactorSource, List<SigningEntity>>()
        signers.forEach { signer ->
            when (val securityState = signer.securityState) {
                is SecurityState.Unsecured -> {
                    val factorSourceId = securityState.unsecuredEntityControl.transactionSigning.factorSourceId
                    val factorSource = requireNotNull(getProfileUseCase.factorSource(factorSourceId))
                    if (result[factorSource] != null) {
                        result[factorSource] = result[factorSource].orEmpty() + listOf(signer)
                    } else {
                        result[factorSource] = listOf(signer)
                    }
                }
            }
        }
        return result.toSortedMap(comparator = { l, r ->
            l.kind.signingOrder().compareTo(r.kind.signingOrder())
        })
    }
}

fun FactorSourceKind.signingOrder(): Int {
    return when (this) {
        FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> 0
        FactorSourceKind.DEVICE -> 1
    }
}
