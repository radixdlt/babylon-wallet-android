package rdx.works.core.sargon

import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
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
