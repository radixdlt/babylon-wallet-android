package rdx.works.profile.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

interface PersonaRepository {

    val personas: Flow<List<Network.Persona>>
    suspend fun getPersonas(): List<Network.Persona>
    suspend fun getPersonaByAddress(address: String): Network.Persona?
    fun getPersonaByAddressFlow(address: String): Flow<Network.Persona>
}

class PersonaRepositoryImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource
) : PersonaRepository {

    override val personas: Flow<List<Network.Persona>> = profileDataSource.profile.map { profile ->
        profile?.networks?.firstOrNull { perNetwork ->
            perNetwork.networkID == profileDataSource.getCurrentNetwork().networkId().value
        }?.personas.orEmpty()
    }

    override suspend fun getPersonas(): List<Network.Persona> {
        return getPerNetwork()?.personas.orEmpty()
    }

    override fun getPersonaByAddressFlow(address: String): Flow<Network.Persona> {
        return personas.mapNotNull { persona -> persona.firstOrNull { it.address == address } }
    }

    override suspend fun getPersonaByAddress(address: String): Network.Persona? {
        return getPerNetwork()?.personas?.firstOrNull { persona ->
            persona.address == address
        }
    }

    private suspend fun getPerNetwork(): Network? {
        return profileDataSource.readProfile()
            ?.networks
            ?.firstOrNull { perNetwork ->
                perNetwork.networkID == profileDataSource.getCurrentNetwork().networkId().value
            }
    }
}
