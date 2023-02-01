package rdx.works.profile.data.repository

import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.pernetwork.OnNetwork
import javax.inject.Inject

interface DAppConnectionRepository {

    suspend fun addConnectedDApp(connectedDApp: OnNetwork.ConnectedDapp)

    suspend fun updateConnectedDApp(connectedDApp: OnNetwork.ConnectedDapp)
}

class DAppConnectionRepositoryImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource
) : DAppConnectionRepository {

    override suspend fun addConnectedDApp(connectedDApp: OnNetwork.ConnectedDapp) {
        val profile = profileDataSource.readProfile()

        requireNotNull(profile)

        val updatedProfile = profile.addConnectedDapp(connectedDApp)

        profileDataSource.saveProfile(updatedProfile)
    }

    override suspend fun updateConnectedDApp(connectedDApp: OnNetwork.ConnectedDapp) {
        val profile = profileDataSource.readProfile()

        requireNotNull(profile)

        val updatedProfile = profile.updateConnectedDapp(connectedDApp)

        profileDataSource.saveProfile(updatedProfile)
    }
}

fun Profile.addConnectedDapp(
    connectedDapp: OnNetwork.ConnectedDapp
): Profile {
    val updatedOnNetwork = onNetwork.map { network ->
        if (network.networkID == connectedDapp.networkID) {
            // Check if this dapp exists in the profile and if so throw exception
            val dAppExists = network.connectedDapps.any { dApp ->
                dApp.dAppDefinitionAddress == connectedDapp.dAppDefinitionAddress
            }
            require(dAppExists.not()) {
                throw IllegalArgumentException("Dapp already exists")
            }

            val validatedConnectedDapp = network.validateAuthorizedPersonas(connectedDapp)
            network.copy(
                accounts = network.accounts,
                connectedDapps = network.connectedDapps + validatedConnectedDapp,
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

fun Profile.updateConnectedDapp(
    unverifiedConnectedDapp: OnNetwork.ConnectedDapp
): Profile {
    val updatedOnNetwork = onNetwork.map { network ->
        if (network.networkID == unverifiedConnectedDapp.networkID) {
            // Check if this dapp exists in the profile and if NOT, throw exception
            val existingDapp = network.connectedDapps.find { dAppInProfile ->
                dAppInProfile.dAppDefinitionAddress == unverifiedConnectedDapp.dAppDefinitionAddress
            }
            requireNotNull(existingDapp) {
                throw IllegalArgumentException("Dapp does NOT exist")
            }

            val connectedDapp = network.validateAuthorizedPersonas(unverifiedConnectedDapp)
            // Remove old connectedDapp
            val updatedDapps = network.connectedDapps.toMutableList().apply {
                set(network.connectedDapps.indexOf(existingDapp), connectedDapp)
            }

            network.copy(
                accounts = network.accounts,
                connectedDapps = updatedDapps,
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
    // Instead of using SortedSet/Set, i use List which is ordered and make sure duplicates are gone
    val referencesToAuthorizedPersonaNoDuplicates = connectedDapp.referencesToAuthorizedPersonas.toSet().toList()
    // Validate that all Personas are known and that every Field.ID is known for each Persona
    referencesToAuthorizedPersonaNoDuplicates.map { authorizedPersona ->
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
    referencesToAuthorizedPersonaNoDuplicates.flatMap {
        it.sharedAccounts.accountsReferencedByAddress
    }.map { accountAddressReference ->
        require(accounts.any { it.address == accountAddressReference }) {
            throw IllegalArgumentException("Unknown account reference")
        }
    }

    // All good
    return connectedDapp
}
