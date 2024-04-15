package rdx.works.core.sargon

import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.EntityFlag
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.Persona

fun Collection<Persona>.notHiddenPersonas(): List<Persona> = filter { !it.isHidden }
fun Collection<Persona>.hiddenPersonas(): List<Persona> = filter { it.isHidden }

val Persona.factorSourceId: FactorSourceId
    get() = securityState.factorSourceId

val Persona.derivationPathScheme: DerivationPathScheme
    get() = securityState.derivationPathScheme

val Persona.derivationPathEntityIndex: UInt
    get() = securityState.transactionFactorInstance.publicKey.derivationPath.entityIndex ?: 0u

val Persona.hasAuthSigning: Boolean
    get() = securityState.hasAuthSigning

val Persona.usesEd25519: Boolean
    get() = securityState.usesEd25519

val Persona.isHidden: Boolean
    get() = EntityFlag.DELETED_BY_USER in flags