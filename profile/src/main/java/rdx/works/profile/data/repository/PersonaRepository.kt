package rdx.works.profile.data.repository

import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.pernetwork.PerNetwork
import rdx.works.profile.data.model.pernetwork.Persona
import javax.inject.Inject

interface PersonaRepository {
    suspend fun getPersonas(): List<Persona>
    suspend fun getPersonaByAddress(address: String): Persona?
}

class PersonaRepositoryImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource
) : PersonaRepository {

    override suspend fun getPersonas(): List<Persona> {
        return getPerNetwork()?.personas.orEmpty()
    }

    override suspend fun getPersonaByAddress(address: String): Persona? {
        return getPerNetwork()?.personas?.firstOrNull { persona ->
            persona.entityAddress.address == address
        }
    }

    private suspend fun getPerNetwork(): PerNetwork? {
        return profileDataSource.readProfile()
            ?.perNetwork
            ?.firstOrNull { perNetwork ->
                perNetwork.networkID == getCurrentNetwork().networkId().value
            }
    }

    private suspend fun getCurrentNetwork(): Network {
        return profileDataSource.readProfile()
            ?.appPreferences
            ?.networkAndGateway?.network
            ?: NetworkAndGateway.betanet.network
    }
}
