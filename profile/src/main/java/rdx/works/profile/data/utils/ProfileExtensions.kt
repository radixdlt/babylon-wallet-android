package rdx.works.profile.data.utils

import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId

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

fun FactorSource.getNextDerivationPathForAccount(
    networkId: NetworkId
): DerivationPath {
    val index = getNextAccountDerivationIndex(forNetworkId = networkId)
    return DerivationPath.forAccount(
        networkId = networkId,
        accountIndex = index,
        keyType = KeyType.TRANSACTION_SIGNING
    )
}
