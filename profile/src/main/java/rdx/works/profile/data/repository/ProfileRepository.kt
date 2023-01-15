package rdx.works.profile.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.PerNetwork
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.di.coroutines.IoDispatcher
import javax.inject.Inject

interface ProfileRepository {

    val profile: Flow<Profile?>

    val p2pClient: Flow<P2PClient?>

    suspend fun readProfile(): Profile?

    suspend fun saveProfile(profile: Profile)

    suspend fun getAccounts(): List<Account>

    suspend fun getAccount(address: String): Account?

    suspend fun readMnemonic(key: String): String?

    suspend fun clear()
}

class ProfileRepositoryImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ProfileRepository {

    override val profile: Flow<Profile?> = encryptedPreferencesManager.encryptedProfile
        .map { profileContent ->
            profileContent?.let { profile ->
                Json.decodeFromString<ProfileSnapshot>(profile).toProfile()
            }
        }

    override val p2pClient: Flow<P2PClient?> = profile.map { profile ->
        profile?.appPreferences?.p2pClients?.firstOrNull()
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

    override suspend fun getAccounts(): List<Account> {
        val perNetwork = getPerNetwork()
        return perNetwork?.accounts.orEmpty()
    }

    override suspend fun getAccount(address: String): Account? {
        val perNetwork = getPerNetwork()
        return perNetwork
            ?.accounts
            ?.firstOrNull { account ->
                account.entityAddress.address == address
            }
    }

    override suspend fun readMnemonic(key: String): String? {
        return encryptedPreferencesManager.readMnemonic(key)
    }

    override suspend fun clear() {
        encryptedPreferencesManager.clear()
    }

    private suspend fun getPerNetwork(): PerNetwork? {
        return readProfile()
            ?.perNetwork
            ?.firstOrNull { perNetwork ->
                perNetwork.networkID == getCurrentNetwork().networkId().value
            }
    }

    private suspend fun getCurrentNetwork(): Network {
        return readProfile()
            ?.appPreferences
            ?.networkAndGateway?.network
            ?: NetworkAndGateway.nebunet.network
    }
}
