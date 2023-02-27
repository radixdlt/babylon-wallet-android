package rdx.works.profile.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.pernetwork.OnNetwork
import javax.inject.Inject

interface DAppConnectionRepository {

    suspend fun getConnectedDapp(dAppDefinitionAddress: String): OnNetwork.ConnectedDapp?
    fun getConnectedDapps(): Flow<List<OnNetwork.ConnectedDapp>>

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
    ): OnNetwork.ConnectedDapp

    suspend fun deletePersonaForDapp(
        dAppDefinitionAddress: String,
        personaAddress: String
    )

    fun getConnectedDappsByPersona(personaAddress: String): Flow<List<OnNetwork.ConnectedDapp>>

    fun getConnectedDappFlow(dAppDefinitionAddress: String): Flow<OnNetwork.ConnectedDapp?>
    suspend fun deleteDapp(dAppDefinitionAddress: String)
}

class DAppConnectionRepositoryImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource
) : DAppConnectionRepository {

    override fun getConnectedDappFlow(dAppDefinitionAddress: String): Flow<OnNetwork.ConnectedDapp?> {
        return profileDataSource.profile.map {
            it?.getConnectedDapp(dAppDefinitionAddress)
        }
    }

    override suspend fun getConnectedDapp(dAppDefinitionAddress: String): OnNetwork.ConnectedDapp? {
        return profileDataSource.readProfile()?.getConnectedDapp(dAppDefinitionAddress)
    }

    override fun getConnectedDapps(): Flow<List<OnNetwork.ConnectedDapp>> {
        return profileDataSource.profile.map { profile -> profile?.getConnectedDapps().orEmpty() }
    }

    override suspend fun updateOrCreateConnectedDApp(connectedDApp: OnNetwork.ConnectedDapp) {
        val profile = profileDataSource.readProfile()

        requireNotNull(profile)

        val updatedProfile = profile.createOrUpdateConnectedDapp(connectedDApp)

        profileDataSource.saveProfile(updatedProfile)
    }

    override suspend fun deleteDapp(dAppDefinitionAddress: String) {
        val profile = profileDataSource.readProfile()

        requireNotNull(profile)

        getConnectedDapp(dAppDefinitionAddress)?.let {
            val updatedProfile = profile.deleteConnectedDapp(it)
            profileDataSource.saveProfile(updatedProfile)
        }
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

    override suspend fun deletePersonaForDapp(dAppDefinitionAddress: String, personaAddress: String) {
        getConnectedDapp(dAppDefinitionAddress)?.let { dapp ->
            val updatedDapp = dapp.copy(
                referencesToAuthorizedPersonas = dapp.referencesToAuthorizedPersonas.filter {
                    it.identityAddress != personaAddress
                }
            )
            if (updatedDapp.referencesToAuthorizedPersonas.isEmpty()) {
                deleteDapp(dapp.dAppDefinitionAddress)
            } else {
                updateOrCreateConnectedDApp(updatedDapp)
            }
        }
    }

    override fun getConnectedDappsByPersona(personaAddress: String): Flow<List<OnNetwork.ConnectedDapp>> {
        return getConnectedDapps().map { connectedDapps ->
            connectedDapps.filter { dapp ->
                dapp.referencesToAuthorizedPersonas.any { it.identityAddress == personaAddress }
            }
        }
    }
}

private fun Profile.getConnectedDapp(dAppDefinitionAddress: String): OnNetwork.ConnectedDapp? {
    return getConnectedDapps().firstOrNull { it.dAppDefinitionAddress == dAppDefinitionAddress }
}

private fun Profile.getConnectedDapps(): List<OnNetwork.ConnectedDapp> {
    val networkId = appPreferences.networkAndGateway.network.networkId().value
    return onNetwork.firstOrNull { it.networkID == networkId }?.connectedDapps.orEmpty()
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

private fun Profile.deleteConnectedDapp(
    dapp: OnNetwork.ConnectedDapp
): Profile {
    val updatedOnNetwork = onNetwork.map { network ->
        if (network.networkID == dapp.networkID) {
            network.copy(
                accounts = network.accounts,
                connectedDapps = network.connectedDapps.filter {
                    it.dAppDefinitionAddress != dapp.dAppDefinitionAddress
                },
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

fun OnNetwork.ConnectedDapp.addOrUpdateConnectedDappPersona(
    persona: OnNetwork.Persona,
    lastUsed: String
): OnNetwork.ConnectedDapp {
    val existing = getExistingAuthorizedPersona(persona.address)
    val updatedAuthPersonas = if (existing != null) {
        referencesToAuthorizedPersonas.toMutableList().apply {
            val index = indexOf(existing)
            if (index != -1) {
                removeAt(index)
                add(index, existing.copy(lastUsedOn = lastUsed))
            }
        }
    } else {
        (
            listOf(
                OnNetwork.ConnectedDapp.AuthorizedPersonaSimple(
                    identityAddress = persona.address,
                    fieldIDs = emptyList(),
                    lastUsedOn = lastUsed,
                    sharedAccounts = OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts(
                        emptyList(),
                        request = OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                            OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.Exactly,
                            0
                        )
                    )
                )
            ) + referencesToAuthorizedPersonas
            ).distinctBy { it.identityAddress }
    }

    return copy(
        networkID = networkID,
        dAppDefinitionAddress = dAppDefinitionAddress,
        displayName = displayName,
        referencesToAuthorizedPersonas = updatedAuthPersonas
    )
}

fun OnNetwork.ConnectedDapp.getExistingAuthorizedPersona(
    personaAddress: String
): OnNetwork.ConnectedDapp.AuthorizedPersonaSimple? {
    return referencesToAuthorizedPersonas.firstOrNull { it.identityAddress == personaAddress }
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
    require(networkID == connectedDapp.networkID)

// Validate that all Personas are known and that every Field.ID is known for each Persona.
    for (personaNeedle in connectedDapp.referencesToAuthorizedPersonas) {
        val persona = personas.first {
            it.address == personaNeedle.identityAddress
        }
        val fieldIDNeedles = personaNeedle.fieldIDs.toSet()
        val fieldIDHaystack = persona.fields.map { it.id }.toSet()

        require(fieldIDHaystack.containsAll(fieldIDNeedles))
    }

// Validate that all Accounts are known
    val accountAddressNeedles = connectedDapp.referencesToAuthorizedPersonas.flatMap {
        it.sharedAccounts.accountsReferencedByAddress
    }.toSet()

    val accountAddressHaystack = accounts.map { it.address }.toSet()

    require(accountAddressHaystack.containsAll(accountAddressNeedles))

// All good
    return connectedDapp
}
