package rdx.works.profile.data.repository

import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.pernetwork.OnNetwork
import javax.inject.Inject

interface DAppConnectionRepository {

    suspend fun getConnectedDapp(dAppDefinitionAddress: String): OnNetwork.ConnectedDapp?

    suspend fun updateOrCreateConnectedDApp(connectedDApp: OnNetwork.ConnectedDapp)

    suspend fun getDAppConnectedPersona(
        dAppDefinitionAddress: String,
        personaAddress: String
    ): OnNetwork.ConnectedDapp.AuthorizedPersonaSimple?

    suspend fun dAppConnectedPersonaAccountAddresses(
        dAppDefinitionAddress: String,
        personaAddress: String,
        numberOfAccounts: Int,
        quantifier: OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier
    ): List<String>

    suspend fun updateDappAuthorizedPersonaSharedAccounts(
        dAppDefinitionAddress: String,
        personaAddress: String,
        sharedAccounts: OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts
    ): OnNetwork.ConnectedDapp?
}

class DAppConnectionRepositoryImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource
) : DAppConnectionRepository {

    override suspend fun getConnectedDapp(dAppDefinitionAddress: String): OnNetwork.ConnectedDapp? {
        val networkId = profileDataSource.getCurrentNetwork().networkId().value
        return profileDataSource.readProfile()?.getConnectedDapp(dAppDefinitionAddress, networkId)
    }

    override suspend fun updateOrCreateConnectedDApp(connectedDApp: OnNetwork.ConnectedDapp) {
        val profile = profileDataSource.readProfile()

        requireNotNull(profile)

        val updatedProfile = profile.createOrUpdateConnectedDapp(connectedDApp)

        profileDataSource.saveProfile(updatedProfile)
    }

    override suspend fun getDAppConnectedPersona(
        dAppDefinitionAddress: String,
        personaAddress: String
    ): OnNetwork.ConnectedDapp.AuthorizedPersonaSimple? {
        return getConnectedDapp(dAppDefinitionAddress)?.referencesToAuthorizedPersonas?.firstOrNull {
            it.identityAddress == personaAddress
        }
    }

    override suspend fun dAppConnectedPersonaAccountAddresses(
        dAppDefinitionAddress: String,
        personaAddress: String,
        numberOfAccounts: Int,
        quantifier: OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier
    ): List<String> {
        val sharedAccounts = getConnectedDapp(
            dAppDefinitionAddress
        )?.referencesToAuthorizedPersonas?.firstOrNull {
            it.identityAddress == personaAddress
        }?.sharedAccounts
        return if (quantifier == sharedAccounts?.request?.quantifier && numberOfAccounts == sharedAccounts.request.quantity) {
            sharedAccounts.accountsReferencedByAddress
        } else {
            emptyList()
        }
    }

    override suspend fun updateDappAuthorizedPersonaSharedAccounts(
        dAppDefinitionAddress: String,
        personaAddress: String,
        sharedAccounts: OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts
    ): OnNetwork.ConnectedDapp {
        val dapp = getConnectedDapp(dAppDefinitionAddress)
        val persona = dapp?.referencesToAuthorizedPersonas?.firstOrNull {
            it.identityAddress == personaAddress
        }
        requireNotNull(persona)
        return dapp.copy(
            referencesToAuthorizedPersonas = dapp.referencesToAuthorizedPersonas.toMutableList().apply {
                set(indexOf(persona), persona.copy(sharedAccounts = sharedAccounts))
            }
        )
    }
}

fun Profile.getConnectedDapp(dAppDefinitionAddress: String, networkId: Int): OnNetwork.ConnectedDapp? {
    return onNetwork.firstOrNull { it.networkID == networkId }?.let { onNetwork ->
        onNetwork.connectedDapps.firstOrNull { it.dAppDefinitionAddress == dAppDefinitionAddress }
    }
}

fun Profile.createOrUpdateConnectedDapp(
    unverifiedConnectedDapp: OnNetwork.ConnectedDapp
): Profile {
    val updatedOnNetwork = onNetwork.map { network ->
        if (network.networkID == unverifiedConnectedDapp.networkID) {
            // Check if this dapp exists in the profile and if NOT, throw exception
            val existingDapp = network.connectedDapps.find { dAppInProfile ->
                dAppInProfile.dAppDefinitionAddress == unverifiedConnectedDapp.dAppDefinitionAddress
            }
            if (existingDapp == null) {
                network.copy(
                    accounts = network.accounts,
                    connectedDapps = network.connectedDapps + listOf(unverifiedConnectedDapp),
                    networkID = network.networkID,
                    personas = network.personas
                )
            } else {
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
            }
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

fun OnNetwork.ConnectedDapp.updateConnectedDappPersonas(
    connectedDAppPersonas: List<OnNetwork.ConnectedDapp.AuthorizedPersonaSimple>
): OnNetwork.ConnectedDapp {
    val updatedAuthPersonas = (connectedDAppPersonas + referencesToAuthorizedPersonas).distinctBy { it.identityAddress }
    return copy(
        networkID = networkID,
        dAppDefinitionAddress = dAppDefinitionAddress,
        displayName = displayName,
        referencesToAuthorizedPersonas = updatedAuthPersonas
    )
}

fun OnNetwork.ConnectedDapp.updateDappAuthorizedPersonaSharedAccounts(
    personaAddress: String,
    sharedAccounts: OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts
): OnNetwork.ConnectedDapp {
    val persona = referencesToAuthorizedPersonas.firstOrNull {
        it.identityAddress == personaAddress
    }
    requireNotNull(persona)
    return this.copy(
        referencesToAuthorizedPersonas = referencesToAuthorizedPersonas.toMutableList().apply {
            set(indexOf(persona), persona.copy(sharedAccounts = sharedAccounts))
        }
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
