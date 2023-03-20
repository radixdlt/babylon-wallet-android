package rdx.works.profile.data.extensions

import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.crypto.PublicKey
import com.radixdlt.toolkit.models.request.DeriveVirtualAccountAddressRequest
import com.radixdlt.toolkit.models.request.DeriveVirtualIdentityAddressRequest
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.WasNotDeviceFactorSource
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.derivation.model.NetworkId

fun Profile.updatePersona(
    persona: OnNetwork.Persona
): Profile {
    val networkId = appPreferences.gateways.current().network.networkId()

    return copy(
        onNetwork = onNetwork.mapWhen(
            predicate = { it.networkID == networkId.value },
            mutation = { network ->
                network.copy(
                    personas = network.personas.mapWhen(
                        predicate = { it.address == persona.address },
                        mutation = { persona }
                    )
                )
            }
        )
    )
}

fun Profile.addPersona(
    persona: OnNetwork.Persona,
    withFactorSourceId: FactorSource.ID,
    onNetwork: NetworkId
): Profile {
    val personaExists = this.onNetwork.find {
        it.networkID == onNetwork.value
    }?.personas?.any { it.address == persona.address } ?: false

    if (personaExists) {
        return this
    }

    return copy(
        onNetwork = this.onNetwork.mapWhen(
            predicate = { it.networkID == onNetwork.value },
            mutation = { network ->
                network.copy(personas = network.personas + persona)
            }
        ),
        factorSources = factorSources.mapWhen(
            predicate = { it.id == withFactorSourceId },
            mutation = { factorSource ->
                val deviceStorage = factorSource.storage as? FactorSource.Storage.Device
                    ?: throw WasNotDeviceFactorSource()

                factorSource.copy(
                    storage = deviceStorage.incrementIdentity(forNetworkId = onNetwork)
                )
            }
        )
    )
}

fun Profile.addAccount(
    account: OnNetwork.Account,
    withFactorSourceId: FactorSource.ID,
    onNetwork: NetworkId
): Profile {
    val networkExist = this.onNetwork.any { onNetwork.value == it.networkID }
    val newOnNetworks = if (networkExist) {
        this.onNetwork.map { network ->
            if (network.networkID == onNetwork.value) {
                val updatedAccounts = network.accounts.toMutableList()
                updatedAccounts.add(account)
                OnNetwork(
                    accounts = updatedAccounts.toList(),
                    authorizedDapps = network.authorizedDapps,
                    networkID = network.networkID,
                    personas = network.personas
                )
            } else {
                network
            }
        }
    } else {
        this.onNetwork + OnNetwork(
            accounts = listOf(account),
            authorizedDapps = listOf(),
            networkID = onNetwork.value,
            personas = listOf()
        )
    }

    return copy(
        onNetwork = newOnNetworks,
    ).incrementFactorSourceNextAccountIndex(
        forNetwork = onNetwork,
        factorSourceId = withFactorSourceId
    )
}

fun Profile.incrementFactorSourceNextAccountIndex(
    forNetwork: NetworkId,
    factorSourceId: FactorSource.ID
): Profile {
    return copy(
        factorSources = factorSources.map { factorSource ->
            if (factorSource.id == factorSourceId) {
                val deviceStorage = factorSource.storage as? FactorSource.Storage.Device
                    ?: throw WasNotDeviceFactorSource()

                factorSource.copy(
                    storage = deviceStorage.incrementAccount(forNetworkId = forNetwork)
                )
            } else {
                factorSource
            }
        }
    )
}

fun Profile.changeGateway(
    gateway: Gateway
): Profile {
    val gateways = appPreferences.gateways.changeCurrent(gateway)
    val appPreferences = appPreferences.copy(gateways = gateways)
    return copy(appPreferences = appPreferences)
}

fun Profile.addGateway(
    gateway: Gateway
): Profile {
    val updatedGateways = appPreferences.gateways.add(gateway)
    return copy(appPreferences = appPreferences.copy(gateways = updatedGateways))
}

fun Profile.deleteGateway(
    gateway: Gateway
): Profile {
    val updatedGateways = appPreferences.gateways.delete(gateway)
    return copy(appPreferences = appPreferences.copy(gateways = updatedGateways))
}

fun Profile.addP2PLink(
    p2pLink: P2PLink
): Profile {
    val updatedP2PLinks = appPreferences.p2pLinks.toMutableList()
    updatedP2PLinks.add(
        p2pLink
    )

    val newAppPreferences = AppPreferences(
        display = appPreferences.display,
        gateways = appPreferences.gateways,
        p2pLinks = updatedP2PLinks.toList()
    )

    return this.copy(
        appPreferences = newAppPreferences,
        onNetwork = onNetwork,
    )
}

fun Profile.deleteP2PLink(connectionPassword: String): Profile {
    val updatedP2PLinks = appPreferences.p2pLinks.toMutableList()
    updatedP2PLinks.removeIf { p2pLink ->
        p2pLink.connectionPassword == connectionPassword
    }

    val newAppPreferences = AppPreferences(
        display = appPreferences.display,
        gateways = appPreferences.gateways,
        p2pLinks = updatedP2PLinks.toList()
    )

    return this.copy(
        appPreferences = newAppPreferences,
        onNetwork = onNetwork,
    )
}

fun deriveAccountAddress(
    networkID: NetworkId,
    publicKey: PublicKey
): String {
    val request = DeriveVirtualAccountAddressRequest(networkID.value.toUByte(), publicKey)
    // TODO handle error
    val response = RadixEngineToolkit.deriveVirtualAccountAddress(request).getOrThrow()
    return response.virtualAccountAddress.address.componentAddress
}

fun deriveIdentityAddress(
    networkID: NetworkId,
    publicKey: PublicKey
): String {
    val request = DeriveVirtualIdentityAddressRequest(networkID.value.toUByte(), publicKey)
    // TODO handle error
    val response = RadixEngineToolkit.deriveVirtualIdentityAddress(request).getOrThrow()
    return response.virtualIdentityAddress.address.componentAddress
}
