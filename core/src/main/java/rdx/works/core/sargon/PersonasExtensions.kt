package rdx.works.core.sargon

import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.EntityFlag
import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.HdPathComponent
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.extensions.EntityFlags
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.path

fun Collection<Persona>.notHiddenPersonas(): List<Persona> = filter { !it.isHidden }
fun Collection<Persona>.hiddenPersonas(): List<Persona> = filter { it.isHidden }

val Persona.factorSourceId: FactorSourceId
    get() = securityState.factorSourceId

val Persona.derivationPathScheme: DerivationPathScheme
    get() = securityState.derivationPathScheme

val Persona.derivationPathEntityIndex: HdPathComponent
    get() = securityState.transactionSigningFactorInstance.publicKey.derivationPath.path.components.last()

val Persona.hasAuthSigning: Boolean
    get() = securityState.hasAuthSigning

val Persona.usesEd25519: Boolean
    get() = securityState.usesEd25519

val Persona.isHidden: Boolean
    get() = EntityFlag.DELETED_BY_USER in flags

@Suppress("LongParameterList")
fun Persona.Companion.init(
    networkId: NetworkId,
    hdPublicKey: HierarchicalDeterministicPublicKey,
    displayName: DisplayName,
    factorSourceId: FactorSourceId.Hash,
    personaData: PersonaData = PersonaData.empty()
): Persona {
    return Persona(
        address = IdentityAddress.init(publicKey = hdPublicKey.publicKey, networkId = networkId),
        displayName = displayName,
        networkId = networkId,
        securityState = EntitySecurityState.unsecured(
            factorSourceId = factorSourceId,
            hdPublicKey = hdPublicKey
        ),
        personaData = personaData,
        flags = EntityFlags().asList()
    )
}
