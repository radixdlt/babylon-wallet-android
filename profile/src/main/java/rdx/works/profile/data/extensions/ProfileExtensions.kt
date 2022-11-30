package rdx.works.profile.data.extensions

import com.radixdlt.hex.extensions.toHexString
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
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
    val newPerNetwork = perNetwork.map { network ->
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
    return this.copy(
        appPreferences = appPreferences,
        factorSources = factorSources,
        perNetwork = newPerNetwork,
    )
}

fun Profile.addP2PClient(
    p2pClient: P2PClient
): Profile {
    val updatedP2PClients = appPreferences.p2pClients.toMutableList()

    updatedP2PClients.add(
        p2pClient
    )

    val newAppPreferences =
        AppPreferences(
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

/**
 * TODO Once engine toolkit ready, we will used it derive address
 */
fun deriveAddress(
    compressedPublicKey: ByteArray
): EntityAddress {
    val shortenedPublicKey = compressedPublicKey.toHexString().subSequence(0, 25)
    return EntityAddress("mocked_account_address_$shortenedPublicKey)")
}
