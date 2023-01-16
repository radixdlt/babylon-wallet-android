package rdx.works.profile.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.extensions.setNetworkAndGateway
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.derivation.model.NetworkId
import javax.inject.Inject

interface NetworkRepository {

    val networkAndGateway: Flow<NetworkAndGateway>

    suspend fun getCurrentNetworkId(): NetworkId

    suspend fun getCurrentNetworkBaseUrl(): String

    suspend fun hasAccountOnNetwork(
        newUrl: String,
        networkName: String
    ): Boolean

    suspend fun setNetworkAndGateway(
        newUrl: String,
        networkName: String
    )
}

class NetworkRepositoryImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource
) : NetworkRepository {

    override val networkAndGateway = profileDataSource.profile
        .filterNotNull()
        .map { profile ->
            profile.appPreferences.networkAndGateway
        }

    override suspend fun getCurrentNetworkId(): NetworkId {
        return getNetworkAndGateway().network.networkId()
    }

    override suspend fun getCurrentNetworkBaseUrl(): String {
        return getNetworkAndGateway().gatewayAPIEndpointURL
    }

    override suspend fun hasAccountOnNetwork(
        newUrl: String,
        networkName: String
    ): Boolean {
        val knownNetwork = Network
            .allKnownNetworks()
            .firstOrNull { network ->
                network.name == networkName
            } ?: return false

        val newNetworkAndGateway = NetworkAndGateway(
            gatewayAPIEndpointURL = newUrl,
            network = knownNetwork
        )

        return profileDataSource.readProfile()?.let { profile ->
            profile.perNetwork.any { perNetwork ->
                perNetwork.networkID == newNetworkAndGateway.network.id
            }
        } ?: false
    }

    override suspend fun setNetworkAndGateway(
        newUrl: String,
        networkName: String
    ) {
        profileDataSource.readProfile()?.let { profile ->
            val updatedProfile = profile.setNetworkAndGateway(
                NetworkAndGateway(
                    gatewayAPIEndpointURL = newUrl,
                    network = Network.allKnownNetworks()
                        .first { network ->
                            network.name == networkName
                        }
                )
            )
            profileDataSource.saveProfile(updatedProfile)
        }
    }

    private suspend fun getNetworkAndGateway(): NetworkAndGateway {
        return profileDataSource.readProfile()
            ?.appPreferences
            ?.networkAndGateway
            ?: NetworkAndGateway.nebunet
    }
}
