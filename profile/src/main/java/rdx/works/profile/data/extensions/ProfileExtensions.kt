package rdx.works.profile.data.extensions

import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.crypto.PublicKey
import com.radixdlt.toolkit.models.request.DeriveVirtualAccountAddressRequest
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.EntityAddress
import rdx.works.profile.data.model.pernetwork.PerNetwork
import rdx.works.profile.data.model.pernetwork.Persona
import rdx.works.profile.derivation.model.NetworkId

fun Profile.addPersonaOnNetwork(
    persona: Persona,
    networkID: NetworkId
): Profile {
    val newPerNetwork = perNetwork.map { network ->
        if (network.networkID == networkID.value) {
            val updatedPersonas = network.personas.toMutableList()
            updatedPersonas.add(persona)
            PerNetwork(
                accounts = network.accounts,
                connectedDapps = network.connectedDapps,
                networkID = network.networkID,
                personas = updatedPersonas.toList()
            )
        } else {
            network
        }
    }
    return this.copy(
        appPreferences = appPreferences,
        factorSources = factorSources,
        perNetwork = newPerNetwork,
    )
}

fun Profile.addAccountOnNetwork(
    account: Account,
    networkID: NetworkId
): Profile {
    val networkExist = perNetwork.any { networkID.value == it.networkID }
    val newPerNetwork = if (networkExist) {
        perNetwork.map { network ->
            if (network.networkID == networkID.value) {
                val updatedAccounts = network.accounts.toMutableList()
                updatedAccounts.add(account)
                PerNetwork(
                    accounts = updatedAccounts.toList(),
                    connectedDapps = network.connectedDapps,
                    networkID = network.networkID,
                    personas = network.personas
                )
            } else {
                network
            }
        }
    } else {
        perNetwork + PerNetwork(
            accounts = listOf(account),
            connectedDapps = listOf(),
            networkID = networkID.value,
            personas = listOf()
        )
    }

    return this.copy(
        appPreferences = appPreferences,
        factorSources = factorSources,
        perNetwork = newPerNetwork,
    )
}

fun Profile.setNetworkAndGateway(
    networkAndGateway: NetworkAndGateway
): Profile {
    val appPreferences = appPreferences.copy(networkAndGateway = networkAndGateway)
    return copy(appPreferences = appPreferences)
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
        networkAndGateway = appPreferences.networkAndGateway,
        p2pClients = updatedP2PClients.toList()
    )

    return this.copy(
        appPreferences = newAppPreferences,
        factorSources = factorSources,
        perNetwork = perNetwork,
    )
}

fun Profile.deleteP2PClient(connectionPassword: String): Profile {
    val updatedP2PClients = appPreferences.p2pClients.toMutableList()
    updatedP2PClients.removeIf { p2pClient ->
        p2pClient.connectionPassword == connectionPassword
    }

    val newAppPreferences = AppPreferences(
        display = appPreferences.display,
        networkAndGateway = appPreferences.networkAndGateway,
        p2pClients = updatedP2PClients.toList()
    )

    return this.copy(
        appPreferences = newAppPreferences,
        factorSources = factorSources,
        perNetwork = perNetwork,
    )
}

@SuppressWarnings("UnusedPrivateMember")
fun deriveAddress(
    networkID: NetworkId,
    publicKey: PublicKey
): EntityAddress {
    val request = DeriveVirtualAccountAddressRequest(networkID.value.toUByte(), publicKey)
    //TODO handle error
    val response = RadixEngineToolkit.deriveVirtualAccountAddress(request).getOrThrow()
    return EntityAddress(response.virtualAccountAddress.address.componentAddress)
}
