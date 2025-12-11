package rdx.works.core.sargon

import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.TimePeriod
import com.radixdlt.sargon.UnsecuredEntityControl

fun EntitySecurityState.Companion.unsecured(
    factorSourceId: FactorSourceId.Hash,
    hdPublicKey: HierarchicalDeterministicPublicKey
) = EntitySecurityState.Unsecured(
    UnsecuredEntityControl(
        transactionSigning = HierarchicalDeterministicFactorInstance(
            factorSourceId = factorSourceId.value,
            publicKey = hdPublicKey
        ),
        provisionalSecurifiedConfig = null
    )
)

val EntitySecurityState.numberOfSignaturesForTransaction: Int
    get() = when (this) {
        is EntitySecurityState.Securified -> {
            value.securityStructure.matrixOfFactors.primaryRole.thresholdFactors.count()
        }

        is EntitySecurityState.Unsecured -> {
            1
        }
    }

val EntitySecurityState.timeUntilDelayedConfirmationIsCallable: TimePeriod?
    get() = when (this) {
        is EntitySecurityState.Securified -> {
            value.securityStructure.matrixOfFactors.timeUntilDelayedConfirmationIsCallable
        }

        is EntitySecurityState.Unsecured -> {
            null
        }
    }
