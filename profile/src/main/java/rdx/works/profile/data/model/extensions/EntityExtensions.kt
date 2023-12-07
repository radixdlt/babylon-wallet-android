package rdx.works.profile.data.model.extensions

import rdx.works.core.mapWhen
import rdx.works.core.toIdentifiedArrayList
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.data.model.factorsources.EntityFlag
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.derivationPathEntityIndex

val Entity.usesCurve25519: Boolean
    get() {
        val unsecuredEntityControl = (securityState as? SecurityState.Unsecured)?.unsecuredEntityControl
        return when (val virtualBadge = unsecuredEntityControl?.transactionSigning?.badge) {
            is FactorInstance.Badge.VirtualSource.HierarchicalDeterministic -> {
                virtualBadge.publicKey.curve == Slip10Curve.CURVE_25519
            }

            null -> false
        }
    }

val Entity.usesSecp256k1: Boolean
    get() {
        val unsecuredEntityControl = (securityState as? SecurityState.Unsecured)?.unsecuredEntityControl
        return when (val virtualBadge = unsecuredEntityControl?.transactionSigning?.badge) {
            is FactorInstance.Badge.VirtualSource.HierarchicalDeterministic -> {
                virtualBadge.publicKey.curve == Slip10Curve.SECP_256K1
            }

            null -> false
        }
    }

fun Entity.factorSourceId(): FactorSource.FactorSourceID {
    return (this.securityState as SecurityState.Unsecured).unsecuredEntityControl.transactionSigning.factorSourceId
}

fun Entity.derivationPathEntityIndex(): Int {
    val transactionSigning = (this.securityState as SecurityState.Unsecured).unsecuredEntityControl.transactionSigning
    return when (transactionSigning.badge) {
        is FactorInstance.Badge.VirtualSource.HierarchicalDeterministic -> {
            transactionSigning.badge.derivationPath.derivationPathEntityIndex()
        }
    }
}

fun Entity.hasAuthSigning(): Boolean {
    return when (val state = securityState) {
        is SecurityState.Unsecured -> {
            state.unsecuredEntityControl.authenticationSigning != null
        }
    }
}

fun Profile.hidePersona(address: String): Profile {
    val networkId = currentGateway.network.networkId()
    val updatedNetworks = networks.mapWhen(predicate = { it.networkID == networkId.value }, mutation = { network ->
        val updatedAuthorizedDapps = network.authorizedDapps.mapWhen(predicate = { authorizedDapp ->
            authorizedDapp.referencesToAuthorizedPersonas.any { it.identityAddress == address }
        }, mutation = { authorizedDapp ->
            val updatedReferences = authorizedDapp.referencesToAuthorizedPersonas.filter { it.identityAddress != address }
            authorizedDapp.copy(referencesToAuthorizedPersonas = updatedReferences)
        })
        network.copy(
            personas = network.personas.mapWhen(
                predicate = { it.address == address },
                mutation = { persona ->
                    persona.copy(flags = persona.flags + EntityFlag.DeletedByUser)
                }
            ).toIdentifiedArrayList(),
            authorizedDapps = updatedAuthorizedDapps.filter { it.referencesToAuthorizedPersonas.isNotEmpty() }
        )
    })
    return copy(networks = updatedNetworks).withUpdatedContentHint()
}

fun Profile.hideAccount(address: String): Profile {
    val networkId = currentGateway.network.networkId()
    val updatedNetworks = networks.mapWhen(predicate = { it.networkID == networkId.value }, mutation = { network ->
        val updatedAuthorizedDapps = network.authorizedDapps.mapWhen(predicate = { authorizedDapp ->
            authorizedDapp.referencesToAuthorizedPersonas.any { reference ->
                reference.sharedAccounts.ids.any { it == address }
            }
        }, mutation = { authorizedDapp ->
            val updatedReferences =
                authorizedDapp.referencesToAuthorizedPersonas.mapWhen(
                    predicate = { reference ->
                        reference.sharedAccounts.ids.any { it == address }
                    },
                    mutation = { reference ->
                        reference.copy(
                            sharedAccounts = reference.sharedAccounts.copy(
                                reference.sharedAccounts.ids.filter {
                                    it != address
                                }
                            )
                        )
                    }
                )
            authorizedDapp.copy(referencesToAuthorizedPersonas = updatedReferences)
        })
        val updatedAccounts = network.accounts.mapWhen(
            predicate = { it.address == address },
            mutation = { account ->
                account.copy(flags = account.flags + EntityFlag.DeletedByUser)
            }
        ).toSet()
        network.copy(
            accounts = updatedAccounts.toIdentifiedArrayList(),
            authorizedDapps = updatedAuthorizedDapps.filter { it.referencesToAuthorizedPersonas.isNotEmpty() }
        )
    })
    return copy(networks = updatedNetworks).withUpdatedContentHint()
}

fun Profile.unhideAllEntities(): Profile {
    val networkId = currentGateway.network.networkId()
    val updatedNetworks = networks.mapWhen(predicate = { it.networkID == networkId.value }, mutation = { network ->
        network.copy(
            personas = network.personas.map { persona ->
                persona.copy(flags = persona.flags - EntityFlag.DeletedByUser)
            }.toIdentifiedArrayList(),
            accounts = network.accounts.map { persona ->
                persona.copy(flags = persona.flags - EntityFlag.DeletedByUser)
            }.toIdentifiedArrayList()
        )
    })
    return copy(networks = updatedNetworks).withUpdatedContentHint()
}

fun Entity.isHidden(): Boolean {
    return flags.contains(EntityFlag.DeletedByUser)
}
