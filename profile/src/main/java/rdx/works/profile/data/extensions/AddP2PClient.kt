package rdx.works.profile.data.extensions

import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.P2PClient

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