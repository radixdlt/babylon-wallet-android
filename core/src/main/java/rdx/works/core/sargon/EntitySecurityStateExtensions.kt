package rdx.works.core.sargon

import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.PublicKey
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
        authenticationSigning = null
    )
)

val EntitySecurityState.factorSourceId: FactorSourceId
    get() = when (this) {
        is EntitySecurityState.Unsecured -> FactorSourceId.Hash(value.transactionSigning.factorSourceId)
        is EntitySecurityState.Securified -> TODO()
    }

val EntitySecurityState.usesEd25519: Boolean
    get() = when (this) {
        is EntitySecurityState.Unsecured -> {
            val badge = value.transactionSigning.publicKey.publicKey

            badge is PublicKey.Ed25519
        }
        is EntitySecurityState.Securified -> TODO("Securified state is not yet supported.")
    }

val EntitySecurityState.usesSECP256k1: Boolean
    get() = when (this) {
        is EntitySecurityState.Unsecured -> {
            val badge = value.transactionSigning.publicKey.publicKey

            badge is PublicKey.Secp256k1
        }
        is EntitySecurityState.Securified -> TODO("Securified state is not yet supported.")
    }

val EntitySecurityState.derivationPathScheme: DerivationPathScheme
    get() = when (this) {
        is EntitySecurityState.Unsecured -> {
            when (value.transactionSigning.publicKey.derivationPath) {
                is DerivationPath.Bip44Like -> DerivationPathScheme.BIP44_OLYMPIA
                is DerivationPath.Cap26 -> DerivationPathScheme.CAP26
            }
        }
        is EntitySecurityState.Securified -> TODO("Securified state is not yet supported.")
    }

val EntitySecurityState.transactionSigningFactorInstance: HierarchicalDeterministicFactorInstance
    get() = when (this) {
        is EntitySecurityState.Unsecured -> value.transactionSigning
        is EntitySecurityState.Securified -> TODO("Securified state is not yet supported.")
    }

val EntitySecurityState.authenticationSigningFactorInstance: HierarchicalDeterministicFactorInstance?
    get() = when (this) {
        is EntitySecurityState.Unsecured -> value.authenticationSigning
        is EntitySecurityState.Securified -> TODO("Securified state is not yet supported.")
    }

val EntitySecurityState.hasAuthSigning: Boolean
    get() = authenticationSigningFactorInstance != null
