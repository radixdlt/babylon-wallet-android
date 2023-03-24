package rdx.works.profile.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

@Suppress("TooManyFunctions")
interface DAppConnectionRepository {

    suspend fun getAuthorizedDapp(dAppDefinitionAddress: String): Network.AuthorizedDapp?
    fun getAuthorizedDapps(): Flow<List<Network.AuthorizedDapp>>

    suspend fun updateOrCreateAuthorizedDApp(authorizedDApp: Network.AuthorizedDapp)

    suspend fun getDAppConnectedPersona(
        dAppDefinitionAddress: String,
        personaAddress: String
    ): Network.AuthorizedDapp.AuthorizedPersonaSimple?

    suspend fun dAppAuthorizedPersonaAccountAddresses(
        dAppDefinitionAddress: String,
        personaAddress: String,
        numberOfAccounts: Int,
        quantifier: Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier
    ): List<String>

    suspend fun dAppAuthorizedPersonaHasAllDataFields(
        dAppDefinitionAddress: String,
        personaAddress: String,
        fieldIds: List<String>
    ): Boolean

    suspend fun updateDappAuthorizedPersonaSharedAccounts(
        dAppDefinitionAddress: String,
        personaAddress: String,
        sharedAccounts: Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts
    ): Network.AuthorizedDapp

    suspend fun deletePersonaForDapp(
        dAppDefinitionAddress: String,
        personaAddress: String
    )

    fun getAuthorizedDappsByPersona(personaAddress: String): Flow<List<Network.AuthorizedDapp>>

    fun getAuthorizedDappFlow(dAppDefinitionAddress: String): Flow<Network.AuthorizedDapp?>
    suspend fun deleteAuthorizedDapp(dAppDefinitionAddress: String)
    suspend fun resetPersonaPermissions(
        dAppDefinitionAddress: String,
        personaAddress: String,
        personaData: Boolean,
        accounts: Boolean
    )
}

@Suppress("TooManyFunctions")
class DAppConnectionRepositoryImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource
) : DAppConnectionRepository {

    override fun getAuthorizedDappFlow(dAppDefinitionAddress: String): Flow<Network.AuthorizedDapp?> {
        return profileDataSource.profile.map {
            it?.getAuthorizedDapp(dAppDefinitionAddress)
        }
    }

    override suspend fun getAuthorizedDapp(dAppDefinitionAddress: String): Network.AuthorizedDapp? {
        return profileDataSource.readProfile()?.getAuthorizedDapp(dAppDefinitionAddress)
    }

    override fun getAuthorizedDapps(): Flow<List<Network.AuthorizedDapp>> {
        return profileDataSource.profile.map { profile -> profile?.getAuthorizedDapps().orEmpty() }
    }

    override suspend fun updateOrCreateAuthorizedDApp(authorizedDApp: Network.AuthorizedDapp) {
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
    ): Network.AuthorizedDapp.AuthorizedPersonaSimple? {
        return getAuthorizedDapp(dAppDefinitionAddress)?.referencesToAuthorizedPersonas?.firstOrNull {
            it.identityAddress == personaAddress
        }
    }

    override suspend fun dAppAuthorizedPersonaAccountAddresses(
        dAppDefinitionAddress: String,
        personaAddress: String,
        numberOfAccounts: Int,
        quantifier: Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier
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

    override suspend fun dAppAuthorizedPersonaHasAllDataFields(
        dAppDefinitionAddress: String,
        personaAddress: String,
        fieldIds: List<String>
    ): Boolean {
        val alreadyGrantedIds = getAuthorizedDapp(
            dAppDefinitionAddress
        )?.referencesToAuthorizedPersonas?.firstOrNull {
            it.identityAddress == personaAddress
        }?.fieldIDs?.toSet().orEmpty()
        return alreadyGrantedIds.containsAll(fieldIds)
    }

    override suspend fun updateDappAuthorizedPersonaSharedAccounts(
        dAppDefinitionAddress: String,
        personaAddress: String,
        sharedAccounts: Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts
    ): Network.AuthorizedDapp {
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

    private suspend fun deleteDataForPersona(dAppDefinitionAddress: String, personaAddress: String) {
        getAuthorizedDapp(dAppDefinitionAddress)?.let { dapp ->
            val updatedDapp = dapp.copy(
                referencesToAuthorizedPersonas = dapp.referencesToAuthorizedPersonas.mapWhen(
                    predicate = { it.identityAddress == personaAddress }
                ) {
                    it.copy(fieldIDs = emptyList())
                }
            )
            updateOrCreateAuthorizedDApp(updatedDapp)
        }
    }

    override suspend fun resetPersonaPermissions(
        dAppDefinitionAddress: String,
        personaAddress: String,
        personaData: Boolean,
        accounts: Boolean
    ) {
        if (personaData) {
            deleteDataForPersona(dAppDefinitionAddress, personaAddress)
        } else if (accounts) {
            deletePersonaForDapp(dAppDefinitionAddress, personaAddress)
        }
    }

    override fun getAuthorizedDappsByPersona(personaAddress: String): Flow<List<Network.AuthorizedDapp>> {
        return getAuthorizedDapps().map { authorizedDapps ->
            authorizedDapps.filter { dapp ->
                dapp.referencesToAuthorizedPersonas.any { it.identityAddress == personaAddress }
            }
        }
    }
}

private fun Profile.getAuthorizedDapp(dAppDefinitionAddress: String): Network.AuthorizedDapp? {
    return getAuthorizedDapps().firstOrNull { it.dAppDefinitionAddress == dAppDefinitionAddress }
}

private fun Profile.getAuthorizedDapps(): List<Network.AuthorizedDapp> {
    val networkId = appPreferences.gateways.current().network.networkId().value
    return networks.firstOrNull { it.networkID == networkId }?.authorizedDapps.orEmpty()
}

fun Profile.createOrUpdateAuthorizedDapp(
    unverifiedAuthorizedDapp: Network.AuthorizedDapp
): Profile {
    val updatedNetwork = networks.map { network ->
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

    return copy(networks = updatedNetwork)
}

private fun Profile.deleteAuthorizedDapp(
    dapp: Network.AuthorizedDapp
): Profile {
    val updatedNetwork = networks.map { network ->
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

    return copy(networks = updatedNetwork)
}

fun Network.AuthorizedDapp.updateAuthorizedDappPersonas(
    authorizedDAppPersonas: List<Network.AuthorizedDapp.AuthorizedPersonaSimple>
): Network.AuthorizedDapp {
    val updatedAuthPersonas =
        (authorizedDAppPersonas + referencesToAuthorizedPersonas).distinctBy { it.identityAddress }
    return copy(
        networkID = networkID,
        dAppDefinitionAddress = dAppDefinitionAddress,
        displayName = displayName,
        referencesToAuthorizedPersonas = updatedAuthPersonas
    )
}

fun Network.AuthorizedDapp.updateAuthorizedDappPersonaFields(
    personaAddress: String,
    fieldIds: List<String>
): Network.AuthorizedDapp {
    val updatedAuthPersonas = referencesToAuthorizedPersonas.mapWhen(predicate = { it.identityAddress == personaAddress }) {
        it.copy(fieldIDs = fieldIds)
    }
    return copy(
        networkID = networkID,
        dAppDefinitionAddress = dAppDefinitionAddress,
        displayName = displayName,
        referencesToAuthorizedPersonas = updatedAuthPersonas
    )
}

fun Network.AuthorizedDapp.addOrUpdateAuthorizedDappPersona(
    persona: Network.Persona,
    lastUsed: String
): Network.AuthorizedDapp {
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
                Network.AuthorizedDapp.AuthorizedPersonaSimple(
                    identityAddress = persona.address,
                    fieldIDs = emptyList(),
                    lastUsedOn = lastUsed,
                    sharedAccounts = Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts(
                        emptyList(),
                        request = Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                            Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.Exactly,
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

fun Network.AuthorizedDapp.getExistingAuthorizedPersona(
    personaAddress: String
): Network.AuthorizedDapp.AuthorizedPersonaSimple? {
    return referencesToAuthorizedPersonas.firstOrNull { it.identityAddress == personaAddress }
}

fun Network.AuthorizedDapp.updateDappAuthorizedPersonaSharedAccounts(
    personaAddress: String,
    sharedAccounts: Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts
): Network.AuthorizedDapp {
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

private fun Network.validateAuthorizedPersonas(authorizedDapp: Network.AuthorizedDapp): Network.AuthorizedDapp {
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
