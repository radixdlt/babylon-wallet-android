package rdx.works.profile.data.extensions

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
 * TODO Once engine toolkit ready, we will used it derive address, for now we use mocked account address for hammunet
 */
private val addresses = mutableListOf(
    "account_tdx_22_1qdapljk0tmkpj98erq8hm20zwjm35y9uhahev7mejd9q05mkdl",
    "account_tdx_22_1qm66zgzp33rctqe6uug4x3sed58sjuk67fhzwaqxhzss43g0lh",
    "account_tdx_22_1qcf0fhxrd80x077lh5camqug4uuscd9gw2d4m26pxe7sndsaxr",
    "account_tdx_22_1qe6j28ycnh4jwahcpje9chzzlkdn2w2ww4metq6z6qyqg8wwy9",
    "account_tdx_22_1qctml9909t7ln2wnqhsh2d7elvz98rq3zm966huyr2vquxs0he",
    "account_tdx_22_1qcw4z5rprtxy2kle9n9xj5g6nxarhvzkxe807d0l9jyq6mvqd9",
    "account_tdx_22_1qmtr80h3ycf9tf5pzspal8clf6kh6kxqhsa02qsun8cskq4vq0"

)
@Suppress("UnusedPrivateMember")
fun deriveAddress(
    compressedPublicKey: ByteArray
): EntityAddress {
    // TODO For now pick any of valid account addresses to successfully fetch entityResources from backend
    val randomAddress = addresses.random()
    addresses.remove(randomAddress)
    return EntityAddress(randomAddress)
}
