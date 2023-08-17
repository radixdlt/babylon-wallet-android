package rdx.works.profile.domain.signing

import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.factorSourceById
import javax.inject.Inject

/**
 * This use case return signing entities that are either ledger or device factor source instances
 *
 */
class GetSigningEntitiesByFactorSourceUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) {

    @Suppress("NestedBlockDepth", "UnsafeCallOnNullableType")
    suspend operator fun invoke(signers: List<Entity>): Map<FactorSource, List<Entity>> {
        val result = mutableMapOf<FactorSource, List<Entity>>()
        signers.forEach { signer ->
            when (val securityState = signer.securityState) {
                is SecurityState.Unsecured -> {
                    val factorSourceId = securityState.unsecuredEntityControl.transactionSigning.factorSourceId
                    val factorSource = requireNotNull(getProfileUseCase.factorSourceById(factorSourceId))
                    if (factorSource.id.kind != FactorSourceKind.TRUSTED_CONTACT) { // trusted contact cannot sign!
                        if (result[factorSource] != null) {
                            result[factorSource] = result[factorSource].orEmpty() + listOf(signer)
                        } else {
                            result[factorSource] = listOf(signer)
                        }
                    }
                }
            }
        }
        return result.keys.sortedBy { it.id.kind.signingOrder() }.associateWith { result[it]!! }
    }
}

fun FactorSourceKind.signingOrder(): Int {
    return when (this) {
        FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> 1
        FactorSourceKind.DEVICE -> 0 // DEVICE should always go first since we authorize KeyStore encryption key for 5s
        else -> Int.MAX_VALUE // it doesn't matter because we add only the ledger or device factor sources (see line 24)
    }
}
