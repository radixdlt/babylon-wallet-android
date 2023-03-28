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
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.apppreferences.Gateways
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.addGateway
import rdx.works.profile.data.model.apppreferences.changeGateway
import rdx.works.profile.data.model.apppreferences.deleteGateway
import rdx.works.profile.data.model.apppreferences.updateDeveloperMode
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.di.coroutines.IoDispatcher
import javax.inject.Inject

@Suppress("TooManyFunctions")
interface ProfileDataSource {

    val profileState: Flow<Result<Profile?>>

    val profile: Flow<Profile?>

    val p2pLink: Flow<P2PLink?>

    val gateways: Flow<Gateways>

    suspend fun readProfile(): Profile?

    suspend fun saveProfile(profile: Profile)

    suspend fun clear()

    suspend fun getCurrentNetwork(): Radix.Network
    suspend fun getCurrentNetworkId(): NetworkId

    suspend fun getCurrentNetworkBaseUrl(): String

    suspend fun hasAccountForGateway(gateway: Radix.Gateway): Boolean

    suspend fun changeGateway(gateway: Radix.Gateway)

    suspend fun addGateway(gateway: Radix.Gateway)

    suspend fun deleteGateway(gateway: Radix.Gateway)

    suspend fun updateDeveloperMode(isEnabled: Boolean)

    suspend fun isInDeveloperMode(): Boolean
}

@Suppress("TooManyFunctions")
class ProfileDataSourceImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    private val relaxedJson: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ProfileDataSource {

    @Suppress("SwallowedException")
    override val profileState: Flow<Result<Profile?>> =
        encryptedPreferencesManager.encryptedProfile
            .map { profileContent ->
                profileContent?.let { profile ->
                    val profileVersion = relaxedJson.decodeFromString<ProfileSnapshot.ProfileVersionHolder>(profile)
                    val profileIncompatible = profileVersion.version < Profile.LATEST_PROFILE_VERSION
                    if (profileIncompatible) {
                        Result.failure(Exception("Profile incompatible, migration needed"))
                    } else {
                        Result.success(Json.decodeFromString<ProfileSnapshot>(profile).toProfile())
                    }
                } ?: run {
                    Result.success(null)
                }
            }

    override val profile: Flow<Profile?> =
        profileState.map {
            it.getOrNull()
        }

    override val p2pLink: Flow<P2PLink?> = profile.map { profile ->
        profile?.appPreferences?.p2pLinks?.firstOrNull()
    }.distinctUntilChanged()

    override val gateways = profile
        .filterNotNull()
        .map { profile ->
            profile.appPreferences.gateways
        }

    override suspend fun readProfile(): Profile? {
        return profile.firstOrNull()
    }

    override suspend fun saveProfile(profile: Profile) {
        withContext(ioDispatcher) {
            val profileContent = Json.encodeToString(profile.snapshot())
            encryptedPreferencesManager.putProfileSnapshot(profileContent)
        }
    }

    override suspend fun clear() {
        encryptedPreferencesManager.clear()
    }

    override suspend fun getCurrentNetwork(): Radix.Network = readProfile()
        ?.appPreferences
        ?.gateways?.current()?.network
        ?: Radix.Gateway.default.network

    override suspend fun getCurrentNetworkId(): NetworkId {
        return getGateway().network.networkId()
    }

    override suspend fun getCurrentNetworkBaseUrl(): String {
        return getGateway().url
    }

    override suspend fun hasAccountForGateway(gateway: Radix.Gateway): Boolean {
        val knownNetwork = Radix.Network
            .allKnownNetworks()
            .firstOrNull { network ->
                network.name == gateway.network.name
            } ?: return false

        return readProfile()?.let { profile ->
            profile.networks.any { perNetwork ->
                perNetwork.networkID == knownNetwork.id
            }
        } ?: false
    }

    override suspend fun changeGateway(gateway: Radix.Gateway) {
        readProfile()?.let { profile ->
            val updatedProfile = profile.changeGateway(gateway)
            saveProfile(updatedProfile)
        }
    }

    override suspend fun addGateway(gateway: Radix.Gateway) {
        readProfile()?.let { profile ->
            val updatedProfile = profile.addGateway(gateway)
            saveProfile(updatedProfile)
        }
    }

    override suspend fun deleteGateway(gateway: Radix.Gateway) {
        readProfile()?.let { profile ->
            val updatedProfile = profile.deleteGateway(gateway)
            saveProfile(updatedProfile)
        }
    }

    override suspend fun updateDeveloperMode(isEnabled: Boolean) {
        withContext(ioDispatcher) {
            readProfile()?.let { profile ->
                val updatedProfile = profile.updateDeveloperMode(isEnabled)
                saveProfile(updatedProfile)
            }
        }
    }

    override suspend fun isInDeveloperMode(): Boolean = withContext(ioDispatcher) {
        readProfile()?.appPreferences?.security?.isDeveloperModeEnabled ?: false
    }

    private suspend fun getGateway(): Radix.Gateway {
        return readProfile()
            ?.appPreferences
            ?.gateways?.current()
            ?: Radix.Gateway.default
    }
}
