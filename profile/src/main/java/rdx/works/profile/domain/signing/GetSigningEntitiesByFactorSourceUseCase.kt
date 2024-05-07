package rdx.works.profile.domain.signing

import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.kind
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

/**
 * This use case return signing entities that are either ledger or device factor source instances
 *
 */
class GetSigningEntitiesByFactorSourceUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) {

    @Suppress("NestedBlockDepth", "UnsafeCallOnNullableType")
    suspend operator fun invoke(signers: List<ProfileEntity>): Map<FactorSource, List<ProfileEntity>> {
        val result = mutableMapOf<FactorSource, List<ProfileEntity>>()
        val profile = getProfileUseCase()
        signers.forEach { signer ->
            when (val securityState = signer.securityState) {
                is EntitySecurityState.Unsecured -> {
                    val factorSourceId = securityState.value.transactionSigning.factorSourceId.asGeneral()
                    val factorSource = requireNotNull(profile.factorSourceById(factorSourceId))
                    if (factorSource.kind != FactorSourceKind.TRUSTED_CONTACT) { // trusted contact cannot sign!
                        if (result[factorSource] != null) {
                            result[factorSource] = result[factorSource].orEmpty() + listOf(signer)
                        } else {
                            result[factorSource] = listOf(signer)
                        }
                    }
                }
            }
        }
        return result.keys.sortedBy { it.kind.signingOrder() }.associateWith { result[it]!! }
    }
}

fun FactorSourceKind.signingOrder(): Int {
    return when (this) {
        FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> 1
        FactorSourceKind.DEVICE -> 0 // DEVICE should always go first since we authorize KeyStore encryption key for 30s
        else -> Int.MAX_VALUE // it doesn't matter because we add only the ledger or device factor sources (see line 24)
    }
}
