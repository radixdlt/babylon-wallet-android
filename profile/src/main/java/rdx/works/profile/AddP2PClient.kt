package rdx.works.profile

import rdx.works.profile.model.apppreferences.AppPreferences
import rdx.works.profile.model.apppreferences.Connection
import rdx.works.profile.model.apppreferences.P2PClients
import java.util.Date

fun Profile.addP2PClient(
    connectionPassword: String,
    displayName: String
): Profile {
    val now = Date().toString()
    val updatedP2PClients = appPreferences.p2pClients.connections.toMutableList()
    updatedP2PClients.add(
        Connection(
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
        p2pClients = P2PClients(
            connections = updatedP2PClients.toList()
        )
    )

    return this.copy(
        appPreferences = newAppPreferences,
        factorSources = factorSources,
        perNetwork = perNetwork,
    )
}