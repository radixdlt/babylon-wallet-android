package rdx.works.profile.data.model.extensions

import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.P2PLink

fun Profile.addP2PLink(
    p2pLink: P2PLink
): Profile {
    val updatedP2PLinks = appPreferences.p2pLinks.toMutableList()
    updatedP2PLinks.add(
        p2pLink
    )

    val newAppPreferences = AppPreferences(
        transaction = appPreferences.transaction,
        display = appPreferences.display,
        security = appPreferences.security,
        gateways = appPreferences.gateways,
        p2pLinks = updatedP2PLinks.toList()
    )

    return this.copy(
        appPreferences = newAppPreferences,
        networks = networks,
    )
}

fun Profile.deleteP2PLink(connectionPassword: String): Profile {
    val updatedP2PLinks = appPreferences.p2pLinks.toMutableList()
    updatedP2PLinks.removeIf { p2pLink ->
        p2pLink.connectionPassword == connectionPassword
    }

    val newAppPreferences = AppPreferences(
        transaction = appPreferences.transaction,
        display = appPreferences.display,
        security = appPreferences.security,
        gateways = appPreferences.gateways,
        p2pLinks = updatedP2PLinks.toList()
    )

    return this.copy(
        appPreferences = newAppPreferences,
        networks = networks,
    )
}
