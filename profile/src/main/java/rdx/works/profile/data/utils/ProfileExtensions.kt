package rdx.works.profile.data.utils

import rdx.works.core.InstantGenerator
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.factorsources.WasNotDeviceFactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId

fun Profile.updateLastUsed(id: FactorSource.FactorSourceID): Profile {
    return copy(
        factorSources = this.factorSources.mapWhen(predicate = { it.id == id }) { factorSource ->
            factorSource.common.lastUsedOn = InstantGenerator()
            factorSource
        }
    )
}

fun Network.Account.isOlympiaAccount(): Boolean {
    return (securityState as? SecurityState.Unsecured)?.unsecuredEntityControl
        ?.transactionSigning?.publicKey?.curve == Slip10Curve.SECP_256K1
}

fun Entity.factorSourceId(): FactorSource.FactorSourceID.FromHash {
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

fun Network.Persona.filterFields(with: List<Network.Persona.Field.ID>) =
    fields.filter { with.contains(it.id) }

fun List<Network.NextDerivationIndices>?.getNextAccountDerivationIndex(forNetworkId: NetworkId): Int {
    if (this == null) throw WasNotDeviceFactorSource() // TODO not sure about it

    return this.find {
        it.networkId == forNetworkId.value
    }?.forAccount ?: 0
}

fun List<Network.NextDerivationIndices>?.getNextIdentityDerivationIndex(forNetworkId: NetworkId): Int {
    if (this == null) throw WasNotDeviceFactorSource() // TODO not sure about it

    return this.find {
        it.networkId == forNetworkId.value
    }?.forIdentity ?: 0
}

fun LedgerHardwareWalletFactorSource.getNextDerivationPathForAccount(
    networkId: NetworkId
): DerivationPath {
    val index = nextDerivationIndicesPerNetwork?.find {
        it.networkId == networkId.value
    }?.forAccount ?: 0

    return DerivationPath.forAccount(
        networkId = networkId,
        accountIndex = index,
        keyType = KeyType.TRANSACTION_SIGNING
    )
}
