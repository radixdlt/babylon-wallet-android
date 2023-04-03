package rdx.works.profile.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

interface PersonaRepository {

    val personas: Flow<List<Network.Persona>>
    suspend fun getPersonas(): List<Network.Persona>
    suspend fun getPersonaDataFields(address: String, fields: List<Network.Persona.Field.Kind>): List<Network.Persona.Field>
    suspend fun getPersonaByAddress(address: String): Network.Persona?
    fun getPersonaByAddressFlow(address: String): Flow<Network.Persona>
}

class PersonaRepositoryImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource
) : PersonaRepository {

    override val personas: Flow<List<Network.Persona>> = profileDataSource.profile.map { profile ->
        profile.currentNetwork.personas
    }

    override suspend fun getPersonas(): List<Network.Persona> {
        return getPerNetwork().personas
    }

    override suspend fun getPersonaDataFields(address: String, fields: List<Network.Persona.Field.Kind>): List<Network.Persona.Field> {
        return getPersonaByAddress(address)?.fields?.filter { fields.contains(it.kind) }.orEmpty()
    }

    override fun getPersonaByAddressFlow(address: String): Flow<Network.Persona> {
        return personas.mapNotNull { persona -> persona.firstOrNull { it.address == address } }
    }

    override suspend fun getPersonaByAddress(address: String): Network.Persona? {
        return getPerNetwork().personas.firstOrNull { persona ->
            persona.address == address
        }
    }

    private suspend fun getPerNetwork(): Network {
        return profileDataSource.profile.first().currentNetwork
    }
}
