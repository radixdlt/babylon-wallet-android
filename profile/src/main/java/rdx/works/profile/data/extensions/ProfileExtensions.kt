package rdx.works.profile.data.extensions

import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.crypto.PublicKey
import com.radixdlt.toolkit.models.request.DeriveVirtualAccountAddressRequest
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.derivation.model.NetworkId

fun Profile.addPersonaOnNetwork(
    persona: OnNetwork.Persona,
    networkID: NetworkId
): Profile {
    val newOnNetwork = onNetwork.map { network ->
        if (network.networkID == networkID.value) {
            val updatedPersonas = network.personas.toMutableList()
            updatedPersonas.add(persona)
            OnNetwork(
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
                    connectedDapps = network.connectedDapps,
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
            connectedDapps = listOf(),
            networkID = networkID.value,
            personas = listOf()
        )
    }

    return this.copy(
        appPreferences = appPreferences,
        factorSources = factorSources,
        onNetwork = newOnNetworks,
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
        networkAndGateway = appPreferences.networkAndGateway,
        p2pClients = updatedP2PClients.toList()
    )

    return this.copy(
        appPreferences = newAppPreferences,
        factorSources = factorSources,
        onNetwork = onNetwork,
    )
}

fun Profile.addConnectedDapp(
    dAppDefinitionAddress: String,
    dAppDisplayName: String,
    networkId: Int,
    referencesToAuthorizedPersona: List<OnNetwork.ConnectedDapp.AuthorizedPersonaSimple>
): Profile {
    // Instead of using SortedSet/Set, i use List which is ordered and make sure duplicates are gone
    val referencesToAuthorizedPersonaNoDuplicates = referencesToAuthorizedPersona.toSet().toList()
    val unverifiedConnectedDapp = OnNetwork.ConnectedDapp(
        networkID = networkId,
        dAppDefinitionAddress = dAppDefinitionAddress,
        displayName = dAppDisplayName,
        referencesToAuthorizedPersonas = referencesToAuthorizedPersonaNoDuplicates
    )

    val updatedOnNetwork = onNetwork.map { network ->

        if (network.networkID == unverifiedConnectedDapp.networkID) {
            val connectedDapp = network.validateAuthorizedPersonas(unverifiedConnectedDapp)
            network.copy(
                accounts = network.accounts,
                connectedDapps = network.connectedDapps + connectedDapp,
                networkID = network.networkID,
                personas = network.personas
            )
        } else {
            network
        }
    }

    return this.copy(
        appPreferences = appPreferences,
        factorSources = factorSources,
        onNetwork = updatedOnNetwork,
    )
}

private fun OnNetwork.validateAuthorizedPersonas(connectedDapp: OnNetwork.ConnectedDapp): OnNetwork.ConnectedDapp {
    // Validate that all Personas are known and that every Field.ID is known for each Persona
    connectedDapp.referencesToAuthorizedPersonas.map { authorizedPersona ->
        val persona = personas.find {
            it.address == authorizedPersona.identityAddress
        }
        requireNotNull(persona)
        persona.fields.map { field ->
            require(authorizedPersona.fieldIDs.contains(field.id)) {
                throw IllegalArgumentException("Unknown persona field")
            }
        }
    }

    // Validate that all Accounts are known
    connectedDapp.referencesToAuthorizedPersonas.flatMap {
        it.sharedAccounts.accountsReferencedByAddress
    }.map { accountAddressReference ->
        require(accounts.any { it.address == accountAddressReference }) {
            throw IllegalArgumentException("Unknown account reference")
        }
    }

    // All good
    return connectedDapp
}

fun deriveAddress(
    networkID: NetworkId,
    publicKey: PublicKey
): String {
    val request = DeriveVirtualAccountAddressRequest(networkID.value.toUByte(), publicKey)
    // TODO handle error
    val response = RadixEngineToolkit.deriveVirtualAccountAddress(request).getOrThrow()
    return response.virtualAccountAddress.address.componentAddress
}
