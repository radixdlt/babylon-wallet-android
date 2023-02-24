package rdx.works.profile.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import rdx.works.profile.data.model.pernetwork.OnNetwork
import javax.inject.Inject

interface PersonaRepository {

    val personas: Flow<List<OnNetwork.Persona>>
    suspend fun getPersonas(): List<OnNetwork.Persona>
    suspend fun getPersonaByAddress(address: String): OnNetwork.Persona?
    fun getPersonaByAddressFlow(address: String): Flow<OnNetwork.Persona>
}

class PersonaRepositoryImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource
) : PersonaRepository {

    override val personas: Flow<List<OnNetwork.Persona>> = profileDataSource.profile.map { profile ->
        profile?.onNetwork?.firstOrNull { perNetwork ->
            perNetwork.networkID == profileDataSource.getCurrentNetwork().networkId().value
        }?.personas.orEmpty()
    }

    override suspend fun getPersonas(): List<OnNetwork.Persona> {
        return getPerNetwork()?.personas.orEmpty()
    }

    override fun getPersonaByAddressFlow(address: String): Flow<OnNetwork.Persona> {
        return personas.mapNotNull { persona -> persona.firstOrNull { it.address == address } }
    }

    override suspend fun getPersonaByAddress(address: String): OnNetwork.Persona? {
        return getPerNetwork()?.personas?.firstOrNull { persona ->
            persona.address == address
        }
    }

    private suspend fun getPerNetwork(): OnNetwork? {
        return profileDataSource.readProfile()
            ?.onNetwork
            ?.firstOrNull { perNetwork ->
                perNetwork.networkID == profileDataSource.getCurrentNetwork().networkId().value
            }
    }
}
