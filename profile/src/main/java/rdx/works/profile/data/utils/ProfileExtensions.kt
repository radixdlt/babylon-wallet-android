package rdx.works.profile.data.utils

import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId

fun Network.Account.isOlympiaAccount(): Boolean {
    return (securityState as? SecurityState.Unsecured)?.unsecuredEntityControl
        ?.transactionSigning?.publicKey?.curve == Slip10Curve.SECP_256K1
}

fun Entity.factorSourceId(): FactorSource.ID {
    return (this.securityState as SecurityState.Unsecured).unsecuredEntityControl.transactionSigning.factorSourceId
}

fun Entity.hasAuthSigning(): Boolean {
    return when (val state = securityState) {
        is SecurityState.Unsecured -> {
            state.unsecuredEntityControl.authenticationSigning != null
        }
    }
}

fun Entity.networkId() {
    this.networkID
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
