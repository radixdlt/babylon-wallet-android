@file:Suppress("TooManyFunctions")

package rdx.works.profile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.model.PrivateKey
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
import rdx.works.profile.data.extensions.signerPrivateKey
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.notaryFactorSource
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.AccountSigner
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.di.coroutines.DefaultDispatcher
import rdx.works.profile.domain.GetMnemonicUseCase
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
    suspend fun getSignersForAddresses(networkId: Int, addresses: List<String>): List<AccountSigner>
    suspend fun getAccounts(): List<Account>
    suspend fun getPrivateKey(): PrivateKey
}

class ProfileRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val getMnemonicUseCase: GetMnemonicUseCase
) : ProfileRepository {

    override val p2pClient: Flow<P2PClient?> = dataStore.data.catch { exception ->
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

    override val profileSnapshot: Flow<ProfileSnapshot?> = dataStore.data
        .map { preferences ->
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

    override suspend fun setNetworkAndGateway(newUrl: String, networkName: String) {
        readProfileSnapshot()?.toProfile()?.let { profile ->
            val updatedProfile = profile.setNetworkAndGateway(
                NetworkAndGateway(
                    gatewayAPIEndpointURL = newUrl,
                    network = Network.allKnownNetworks().first { it.name == networkName }
                )
            )
            saveProfileSnapshot(updatedProfile.snapshot())
        }
    }

    override suspend fun hasAccountOnNetwork(newUrl: String, networkName: String): Boolean {
        val knownNetwork = Network.allKnownNetworks().firstOrNull { it.name == networkName } ?: return false
        val newNetworkAndGateway = NetworkAndGateway(
            gatewayAPIEndpointURL = newUrl,
            network = knownNetwork
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

    override suspend fun getSignersForAddresses(networkId: Int, addresses: List<String>): List<AccountSigner> {
        val profileSnapshot = readProfileSnapshot()
        val accounts = getSignerAccountsForAddresses(profileSnapshot, addresses, networkId)
        val factorSourceId = profileSnapshot?.notaryFactorSource()?.factorSourceID
        assert(factorSourceId != null)
        val mnemonic = getMnemonicUseCase(factorSourceId)
        assert(mnemonic.isNotEmpty())
        val mnemonicWords = MnemonicWords(mnemonic)
        val signers = mutableListOf<AccountSigner>()
        accounts.forEach {
            val privateKey = mnemonicWords.signerPrivateKey(derivationPath = it.derivationPath)
            signers.add(AccountSigner(it, privateKey, listOf(privateKey)))
        }
        return signers.toList()
    }

    override suspend fun getPrivateKey(): PrivateKey {
        val profileSnapshot = readProfileSnapshot()
        val networkId = getCurrentNetworkId()
        val factorSourceId = profileSnapshot?.notaryFactorSource()?.factorSourceID
        assert(factorSourceId != null)
        val mnemonic = getMnemonicUseCase(factorSourceId)
        assert(mnemonic.isNotEmpty())
        val mnemonicWords = MnemonicWords(mnemonic)
        val account = profileSnapshot?.perNetwork?.firstOrNull { it.networkID == networkId.value }?.accounts?.first()
        assert(account != null)
        return mnemonicWords.signerPrivateKey(derivationPath = account!!.derivationPath)
    }

    private suspend fun getSignerAccountsForAddresses(
        profileSnapshot: ProfileSnapshot?,
        addresses: List<String>,
        networkId: Int
    ): List<Account> {
        val accounts = if (addresses.isNotEmpty()) {
            addresses.mapNotNull { address ->
                getAccount(address)
            }
        } else {
            listOfNotNull(profileSnapshot?.perNetwork?.firstOrNull { it.networkID == networkId }?.accounts?.first())
        }
        return accounts
    }

    private suspend fun getNetworkAndGateway(): NetworkAndGateway {
        return readProfileSnapshot()?.appPreferences?.networkAndGateway ?: NetworkAndGateway.nebunet
    }

    override suspend fun getAccounts(): List<Account> {
        return readProfileSnapshot()?.perNetwork
            ?.firstOrNull { it.networkID == getCurrentNetworkId().value }?.accounts.orEmpty()
    }

    private suspend fun getAccount(address: String): Account? {
        val networkId = getCurrentNetworkId()
        val perNetwork = readProfileSnapshot()?.perNetwork?.firstOrNull { it.networkID == networkId.value }
        return perNetwork?.accounts?.firstOrNull { it.entityAddress.address == address }
    }

    companion object {
        private val PROFILE_PREFERENCES_KEY = stringPreferencesKey("profile_preferences_key")
    }
}
