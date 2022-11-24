package rdx.works.profile

import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.model.pernetwork.PerNetwork
import rdx.works.profile.model.pernetwork.Persona

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