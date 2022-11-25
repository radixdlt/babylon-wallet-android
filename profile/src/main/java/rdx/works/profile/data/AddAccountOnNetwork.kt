package rdx.works.profile.data

import rdx.works.profile.data.model.Profile
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.PerNetwork

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