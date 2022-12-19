package rdx.works.profile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.extensions.setNetworkAndGateway
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.di.coroutines.DefaultDispatcher
import java.io.IOException
import javax.inject.Inject

// TODO will have to add encryption
interface ProfileRepository {

    suspend fun saveProfileSnapshot(profileSnapshot: ProfileSnapshot)

    suspend fun readProfileSnapshot(): ProfileSnapshot?

    val p2pClient: Flow<P2PClient?>
    suspend fun getCurrentNetworkId(): NetworkId
    suspend fun setNetworkAndGateway(newUrl: String, networkName: String)
    suspend fun hasAccountOnNetwork(newUrl: String, networkName: String): Boolean
    val profileSnapshot: Flow<ProfileSnapshot?>
    suspend fun getCurrentNetworkBaseUrl(): String
}

class ProfileRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ProfileRepository {

    override val p2pClient: Flow<P2PClient?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val profileJsonString = preferences[PROFILE_PREFERENCES_KEY] ?: ""
            if (profileJsonString.isNotEmpty()) {
                val profileSnapshot = Json.decodeFromString<ProfileSnapshot>(profileJsonString)
                profileSnapshot.toProfile().appPreferences.p2pClients.firstOrNull()
            } else { // profile doesn't exist
                null
            }
        }

    override val profileSnapshot: Flow<ProfileSnapshot?> = dataStore.data.map { preferences ->
        val profileContent = preferences[PROFILE_PREFERENCES_KEY] ?: ""
        if (profileContent.isEmpty()) {
            null
        } else {
            Json.decodeFromString<ProfileSnapshot>(profileContent)
        }
    }

    override suspend fun saveProfileSnapshot(profileSnapshot: ProfileSnapshot) {
        withContext(defaultDispatcher) {
            val profileContent = Json.encodeToString(profileSnapshot)
            dataStore.edit { preferences ->
                preferences[PROFILE_PREFERENCES_KEY] = profileContent
            }
        }
    }

    private suspend fun getNetworkAndGateway(): NetworkAndGateway {
        return readProfileSnapshot()?.appPreferences?.networkAndGateway ?: NetworkAndGateway.hammunet
    }

    override suspend fun setNetworkAndGateway(newUrl: String, networkName: String) {
        readProfileSnapshot()?.toProfile()?.let { profile ->
            val updatedProfile = profile.setNetworkAndGateway(
                NetworkAndGateway(
                    newUrl,
                    Network.allKnownNetworks().first { it.name == networkName }
                )
            )
            saveProfileSnapshot(updatedProfile.snapshot())
        }
    }

    override suspend fun hasAccountOnNetwork(newUrl: String, networkName: String): Boolean {
        val knownNetwork = Network.allKnownNetworks().firstOrNull { it.name == networkName } ?: return false
        val newNetworkAndGateway = NetworkAndGateway(
            newUrl, knownNetwork
        )
        return readProfileSnapshot()?.toProfile()?.let { profile ->
            profile.perNetwork.any { it.networkID == newNetworkAndGateway.network.id }
        } ?: false
    }

    override suspend fun getCurrentNetworkId(): NetworkId {
        return getNetworkAndGateway().network.networkId()
    }

    override suspend fun getCurrentNetworkBaseUrl(): String {
        return getNetworkAndGateway().gatewayAPIEndpointURL
    }

    override suspend fun readProfileSnapshot(): ProfileSnapshot? {
        return withContext(defaultDispatcher) {
            profileSnapshot.first()
        }
    }

    companion object {
        private val PROFILE_PREFERENCES_KEY = stringPreferencesKey("profile_preferences_key")
    }
}
