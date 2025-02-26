package rdx.works.core.sargon

import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.EntityFlag
import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.extensions.EntityFlags
import com.radixdlt.sargon.extensions.init

fun Collection<Persona>.active(): List<Persona> = filterNot { it.isHidden || it.isDeleted }

val Persona.isHidden: Boolean
    get() = EntityFlag.HIDDEN_BY_USER in flags

// Not implemented, the user cannot delete a persona.
// Keeping this for parity with account.
val Persona.isDeleted: Boolean
    get() = EntityFlag.TOMBSTONED_BY_USER in flags

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
