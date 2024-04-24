package rdx.works.core.sargon

import com.radixdlt.sargon.Cap26KeyKind
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.EntityFlag
import com.radixdlt.sargon.EntityFlags
import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.extensions.HDPathValue
import com.radixdlt.sargon.extensions.contains
import com.radixdlt.sargon.extensions.identity
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.nonHardenedIndex

fun Collection<Persona>.notHiddenPersonas(): List<Persona> = filter { !it.isHidden }
fun Collection<Persona>.hiddenPersonas(): List<Persona> = filter { it.isHidden }

val Persona.factorSourceId: FactorSourceId
    get() = securityState.factorSourceId

val Persona.derivationPathScheme: DerivationPathScheme
    get() = securityState.derivationPathScheme

val Persona.derivationPathEntityIndex: HDPathValue
    get() = securityState.transactionSigningFactorInstance.publicKey.derivationPath.nonHardenedIndex

val Persona.hasAuthSigning: Boolean
    get() = securityState.hasAuthSigning

val Persona.usesEd25519: Boolean
    get() = securityState.usesEd25519

val Persona.isHidden: Boolean
    get() = EntityFlag.DELETED_BY_USER in flags

fun Persona.Companion.init(
    mnemonicWithPassphrase: MnemonicWithPassphrase,
    networkId: NetworkId,
    entityIndex: HDPathValue,
    displayName: DisplayName,
    factorSourceId: FactorSourceId.Hash,
    personaData: PersonaData = PersonaData.empty()
): Persona {
    val derivationPath = DerivationPath.Cap26.identity(
        networkId = networkId,
        keyKind = Cap26KeyKind.TRANSACTION_SIGNING,
        index = entityIndex
    )
    val publicKey = mnemonicWithPassphrase.derivePublicKey(derivationPath = derivationPath)

    return Persona(
        address = IdentityAddress.init(publicKey = publicKey, networkId = networkId),
        displayName = displayName,
        networkId = networkId,
        securityState = EntitySecurityState.unsecured(
            factorSourceId = factorSourceId,
            publicKey = publicKey,
            derivationPath = derivationPath
        ),
        personaData = personaData,
        flags = EntityFlags.init()
    )
}
