package rdx.works.profile.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.pernetwork.OnNetwork
import javax.inject.Inject

interface DAppConnectionRepository {

    suspend fun getAuthorizedDapp(dAppDefinitionAddress: String): OnNetwork.AuthorizedDapp?
    fun getAuthorizedDapps(): Flow<List<OnNetwork.AuthorizedDapp>>

    suspend fun updateOrCreateAuthorizedDApp(authorizedDApp: OnNetwork.AuthorizedDapp)

    suspend fun getDAppConnectedPersona(
        dAppDefinitionAddress: String,
        personaAddress: String
    ): OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple?

    suspend fun dAppAuthorizedPersonaAccountAddresses(
        dAppDefinitionAddress: String,
        personaAddress: String,
        numberOfAccounts: Int,
        quantifier: OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier
    ): List<String>

    suspend fun updateDappAuthorizedPersonaSharedAccounts(
        dAppDefinitionAddress: String,
        personaAddress: String,
        sharedAccounts: OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts
    ): OnNetwork.AuthorizedDapp

    suspend fun deletePersonaForDapp(
        dAppDefinitionAddress: String,
        personaAddress: String
    )

    fun getAuthorizedDappsByPersona(personaAddress: String): Flow<List<OnNetwork.AuthorizedDapp>>

    fun getAuthorizedDappFlow(dAppDefinitionAddress: String): Flow<OnNetwork.AuthorizedDapp?>
    suspend fun deleteAuthorizedDapp(dAppDefinitionAddress: String)
    suspend fun resetPersonaPermissions(
        dAppDefinitionAddress: String,
        personaAddress: String,
        personaData: Boolean,
        accounts: Boolean
    )
}

class DAppConnectionRepositoryImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource
) : DAppConnectionRepository {

    override fun getAuthorizedDappFlow(dAppDefinitionAddress: String): Flow<OnNetwork.AuthorizedDapp?> {
        return profileDataSource.profile.map {
            it?.getAuthorizedDapp(dAppDefinitionAddress)
        }
    }

    override suspend fun getAuthorizedDapp(dAppDefinitionAddress: String): OnNetwork.AuthorizedDapp? {
        return profileDataSource.readProfile()?.getAuthorizedDapp(dAppDefinitionAddress)
    }

    override fun getAuthorizedDapps(): Flow<List<OnNetwork.AuthorizedDapp>> {
        return profileDataSource.profile.map { profile -> profile?.getAuthorizedDapps().orEmpty() }
    }

    override suspend fun updateOrCreateAuthorizedDApp(authorizedDApp: OnNetwork.AuthorizedDapp) {
        val profile = profileDataSource.readProfile()

        requireNotNull(profile)

        val updatedProfile = profile.createOrUpdateAuthorizedDapp(authorizedDApp)

        profileDataSource.saveProfile(updatedProfile)
    }

    override suspend fun deleteAuthorizedDapp(dAppDefinitionAddress: String) {
        val profile = profileDataSource.readProfile()

        requireNotNull(profile)

        getAuthorizedDapp(dAppDefinitionAddress)?.let {
            val updatedProfile = profile.deleteAuthorizedDapp(it)
            profileDataSource.saveProfile(updatedProfile)
        }
    }

    override suspend fun getDAppConnectedPersona(
        dAppDefinitionAddress: String,
        personaAddress: String
    ): OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple? {
        return getAuthorizedDapp(dAppDefinitionAddress)?.referencesToAuthorizedPersonas?.firstOrNull {
            it.identityAddress == personaAddress
        }
    }

    override suspend fun dAppAuthorizedPersonaAccountAddresses(
        dAppDefinitionAddress: String,
        personaAddress: String,
        numberOfAccounts: Int,
        quantifier: OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier
    ): List<String> {
        val sharedAccounts = getAuthorizedDapp(
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
        sharedAccounts: OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts
    ): OnNetwork.AuthorizedDapp {
        val dapp = getAuthorizedDapp(dAppDefinitionAddress)
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
        getAuthorizedDapp(dAppDefinitionAddress)?.let { dapp ->
            val updatedDapp = dapp.copy(
                referencesToAuthorizedPersonas = dapp.referencesToAuthorizedPersonas.filter {
                    it.identityAddress != personaAddress
                }
            )
            if (updatedDapp.referencesToAuthorizedPersonas.isEmpty()) {
                deleteAuthorizedDapp(dapp.dAppDefinitionAddress)
            } else {
                updateOrCreateAuthorizedDApp(updatedDapp)
            }
        }
    }

    override suspend fun resetPersonaPermissions(
        dAppDefinitionAddress: String,
        personaAddress: String,
        personaData: Boolean,
        accounts: Boolean
    ) {
        if (personaData) {
            // TODO implement when we have personaDataOngoing and personaDataOneTime requests
        } else if (accounts) {
            deletePersonaForDapp(dAppDefinitionAddress, personaAddress)
        }
    }

    override fun getAuthorizedDappsByPersona(personaAddress: String): Flow<List<OnNetwork.AuthorizedDapp>> {
        return getAuthorizedDapps().map { authorizedDapps ->
            authorizedDapps.filter { dapp ->
                dapp.referencesToAuthorizedPersonas.any { it.identityAddress == personaAddress }
            }
        }
    }
}

private fun Profile.getAuthorizedDapp(dAppDefinitionAddress: String): OnNetwork.AuthorizedDapp? {
    return getAuthorizedDapps().firstOrNull { it.dAppDefinitionAddress == dAppDefinitionAddress }
}

private fun Profile.getAuthorizedDapps(): List<OnNetwork.AuthorizedDapp> {
    val networkId = appPreferences.gateways.current().network.networkId().value
    return networks.firstOrNull { it.networkID == networkId }?.authorizedDapps.orEmpty()
}

fun Profile.createOrUpdateAuthorizedDapp(
    unverifiedAuthorizedDapp: OnNetwork.AuthorizedDapp
): Profile {
    val updatedOnNetwork = networks.map { network ->
        if (network.networkID == unverifiedAuthorizedDapp.networkID) {
            // Check if this dapp exists in the profile and if NOT, throw exception
            val existingDapp = network.authorizedDapps.find { dAppInProfile ->
                dAppInProfile.dAppDefinitionAddress == unverifiedAuthorizedDapp.dAppDefinitionAddress
            }
            if (existingDapp == null) {
                network.copy(
                    accounts = network.accounts,
                    authorizedDapps = network.authorizedDapps + listOf(unverifiedAuthorizedDapp),
                    networkID = network.networkID,
                    personas = network.personas
                )
            } else {
                val authorizedDapp = network.validateAuthorizedPersonas(unverifiedAuthorizedDapp)
                // Remove old authorizedDapp
                val updatedDapps = network.authorizedDapps.toMutableList().apply {
                    set(network.authorizedDapps.indexOf(existingDapp), authorizedDapp)
                }
                network.copy(
                    accounts = network.accounts,
                    authorizedDapps = updatedDapps,
                    networkID = network.networkID,
                    personas = network.personas
                )
            }
        } else {
            network
        }
    }

    return copy(networks = updatedOnNetwork)
}

private fun Profile.deleteAuthorizedDapp(
    dapp: OnNetwork.AuthorizedDapp
): Profile {
    val updatedOnNetwork = networks.map { network ->
        if (network.networkID == dapp.networkID) {
            network.copy(
                accounts = network.accounts,
                authorizedDapps = network.authorizedDapps.filter {
                    it.dAppDefinitionAddress != dapp.dAppDefinitionAddress
                },
                networkID = network.networkID,
                personas = network.personas
            )
        } else {
            network
        }
    }

    return copy(networks = updatedOnNetwork)
}

fun OnNetwork.AuthorizedDapp.updateAuthorizedDappPersonas(
    authorizedDAppPersonas: List<OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple>
): OnNetwork.AuthorizedDapp {
    val updatedAuthPersonas =
        (authorizedDAppPersonas + referencesToAuthorizedPersonas).distinctBy { it.identityAddress }
    return copy(
        networkID = networkID,
        dAppDefinitionAddress = dAppDefinitionAddress,
        displayName = displayName,
        referencesToAuthorizedPersonas = updatedAuthPersonas
    )
}

fun OnNetwork.AuthorizedDapp.addOrUpdateAuthorizedDappPersona(
    persona: OnNetwork.Persona,
    lastUsed: String
): OnNetwork.AuthorizedDapp {
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
                OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple(
                    identityAddress = persona.address,
                    fieldIDs = emptyList(),
                    lastUsedOn = lastUsed,
                    sharedAccounts = OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts(
                        emptyList(),
                        request = OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                            OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.Exactly,
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

fun OnNetwork.AuthorizedDapp.getExistingAuthorizedPersona(
    personaAddress: String
): OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple? {
    return referencesToAuthorizedPersonas.firstOrNull { it.identityAddress == personaAddress }
}

fun OnNetwork.AuthorizedDapp.updateDappAuthorizedPersonaSharedAccounts(
    personaAddress: String,
    sharedAccounts: OnNetwork.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts
): OnNetwork.AuthorizedDapp {
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

private fun OnNetwork.validateAuthorizedPersonas(authorizedDapp: OnNetwork.AuthorizedDapp): OnNetwork.AuthorizedDapp {
    require(networkID == authorizedDapp.networkID)

// Validate that all Personas are known and that every Field.ID is known for each Persona.
    for (personaNeedle in authorizedDapp.referencesToAuthorizedPersonas) {
        val persona = personas.first {
            it.address == personaNeedle.identityAddress
        }
        val fieldIDNeedles = personaNeedle.fieldIDs.toSet()
        val fieldIDHaystack = persona.fields.map { it.id }.toSet()

        require(fieldIDHaystack.containsAll(fieldIDNeedles))
    }

// Validate that all Accounts are known
    val accountAddressNeedles = authorizedDapp.referencesToAuthorizedPersonas.flatMap {
        it.sharedAccounts.accountsReferencedByAddress
    }.toSet()

    val accountAddressHaystack = accounts.map { it.address }.toSet()

    require(accountAddressHaystack.containsAll(accountAddressNeedles))

// All good
    return authorizedDapp
}
