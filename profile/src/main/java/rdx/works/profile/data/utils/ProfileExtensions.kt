package rdx.works.profile.data.utils

import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState

fun Network.Account.accountFactorSourceId(): FactorSource.ID? {
    return (securityState as? SecurityState.Unsecured)?.unsecuredEntityControl?.genesisFactorInstance?.factorSourceId
}

fun Network.Account.isOlympiaAccount(): Boolean {
    return (securityState as? SecurityState.Unsecured)?.unsecuredEntityControl
        ?.genesisFactorInstance?.publicKey?.curve == Slip10Curve.SECP_256K1
}

fun Network.Persona.personaFactorSourceId(): FactorSource.ID? {
    return (securityState as? SecurityState.Unsecured)?.unsecuredEntityControl?.genesisFactorInstance?.factorSourceId
}
