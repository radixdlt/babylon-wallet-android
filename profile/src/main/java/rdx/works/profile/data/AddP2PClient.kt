package rdx.works.profile.data

import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.P2PClients
import java.util.Date

fun Profile.addP2PClient(
    connectionPassword: String,
    displayName: String
): Profile {
    val now = Date().toString()
    val updatedP2PClients = appPreferences.p2pClients.toMutableList()
    updatedP2PClients.add(
        P2PClients(
            connectionPassword = connectionPassword,
            displayName = displayName,
            firstEstablishedOn = now,
            lastUsedOn = now
        )
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