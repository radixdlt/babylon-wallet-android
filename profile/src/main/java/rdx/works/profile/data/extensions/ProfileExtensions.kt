package rdx.works.profile.data.extensions

import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.crypto.PublicKey
import com.radixdlt.toolkit.models.request.DeriveVirtualAccountAddressRequest
import com.radixdlt.toolkit.models.request.DeriveVirtualIdentityAddressRequest
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.derivation.model.NetworkId

fun Profile.createOrUpdatePersonaOnNetwork(
    persona: OnNetwork.Persona
): Profile {
    val networkId = appPreferences.gateways.current().network.networkId()
    val newOnNetwork = onNetwork.map { network ->
        if (network.networkID == networkId.value) {
            val personaExist = network.personas.any { it.address == persona.address }
            if (personaExist) {
                OnNetwork(
                    accounts = network.accounts,
                    authorizedDapps = network.authorizedDapps,
                    networkID = network.networkID,
                    personas = network.personas.map {
                        if (it.address == persona.address) {
                            persona
                        } else {
                            it
                        }
                    }
                )
            } else {
                OnNetwork(
                    accounts = network.accounts,
                    authorizedDapps = network.authorizedDapps,
                    networkID = network.networkID,
                    personas = network.personas + persona
                )
            }
        } else {
            network
        }
    }
    return this.copy(
        appPreferences = appPreferences,
        onNetwork = newOnNetwork,
    )
}

fun Profile.addAccountOnNetwork(
    account: OnNetwork.Account,
    networkID: NetworkId
): Profile {
    val networkExist = onNetwork.any { networkID.value == it.networkID }
    val newOnNetworks = if (networkExist) {
        onNetwork.map { network ->
            if (network.networkID == networkID.value) {
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
        onNetwork + OnNetwork(
            accounts = listOf(account),
            authorizedDapps = listOf(),
            networkID = networkID.value,
            personas = listOf()
        )
    }

    return this.copy(
        appPreferences = appPreferences,
        onNetwork = newOnNetworks,
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

fun Profile.addP2PClient(
    p2pClient: P2PClient
): Profile {
    val updatedP2PClients = appPreferences.p2pClients.toMutableList()
    updatedP2PClients.add(
        p2pClient
    )

    val newAppPreferences = AppPreferences(
        display = appPreferences.display,
        gateways = appPreferences.gateways,
        p2pClients = updatedP2PClients.toList()
    )

    return this.copy(
        appPreferences = newAppPreferences,
        onNetwork = onNetwork,
    )
}

fun Profile.deleteP2PClient(connectionPassword: String): Profile {
    val updatedP2PClients = appPreferences.p2pClients.toMutableList()
    updatedP2PClients.removeIf { p2pClient ->
        p2pClient.connectionPassword == connectionPassword
    }

    val newAppPreferences = AppPreferences(
        display = appPreferences.display,
        gateways = appPreferences.gateways,
        p2pClients = updatedP2PClients.toList()
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
