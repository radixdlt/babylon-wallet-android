package rdx.works.profile.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.extensions.setNetworkAndGateway
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.di.coroutines.IoDispatcher
import javax.inject.Inject

interface ProfileDataSource {

    val profile: Flow<Profile?>

    val p2pClient: Flow<P2PClient?>

    val networkAndGateway: Flow<NetworkAndGateway>

    suspend fun readProfile(): Profile?

    suspend fun saveProfile(profile: Profile)

    suspend fun clear()

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

class ProfileDataSourceImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ProfileDataSource {

    override val profile: Flow<Profile?> = encryptedPreferencesManager.encryptedProfile
        .map { profileContent ->
            profileContent?.let { profile ->
                Json.decodeFromString<ProfileSnapshot>(profile).toProfile()
            }
        }

    override val p2pClient: Flow<P2PClient?> = profile.map { profile ->
        profile?.appPreferences?.p2pClients?.firstOrNull()
    }

    override val networkAndGateway = profile
        .filterNotNull()
        .map { profile ->
            profile.appPreferences.networkAndGateway
        }

    override suspend fun readProfile(): Profile? {
        return profile.firstOrNull()
    }

    override suspend fun saveProfile(profile: Profile) {
        withContext(ioDispatcher) {
            val profileContent = Json.encodeToString(profile.snapshot())
            encryptedPreferencesManager.putProfileBytes(profileContent.toByteArray())
        }
    }

    override suspend fun clear() {
        encryptedPreferencesManager.clear()
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

        return readProfile()?.let { profile ->
            profile.perNetwork.any { perNetwork ->
                perNetwork.networkID == newNetworkAndGateway.network.id
            }
        } ?: false
    }

    override suspend fun setNetworkAndGateway(
        newUrl: String,
        networkName: String
    ) {
        readProfile()?.let { profile ->
            val updatedProfile = profile.setNetworkAndGateway(
                NetworkAndGateway(
                    gatewayAPIEndpointURL = newUrl,
                    network = Network.allKnownNetworks()
                        .first { network ->
                            network.name == networkName
                        }
                )
            )
            saveProfile(updatedProfile)
        }
    }

    private suspend fun getNetworkAndGateway(): NetworkAndGateway {
        return readProfile()
            ?.appPreferences
            ?.networkAndGateway
            ?: NetworkAndGateway.betanet
    }
}
