@file:Suppress("TooManyFunctions")

package rdx.works.profile.data.repository

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.model.PrivateKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.extensions.setNetworkAndGateway
import rdx.works.profile.data.extensions.signerPrivateKey
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileSnapshot
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.AccountSigner
import rdx.works.profile.datastore.EncryptedPreferencesManager
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.di.coroutines.IoDispatcher
import rdx.works.profile.domain.GetMnemonicUseCase
import javax.inject.Inject

interface ProfileRepository {

    suspend fun saveProfile(profile: Profile)
    suspend fun readProfile(): Profile?
    suspend fun readMnemonic(key: String): String?
    val p2pClient: Flow<P2PClient?>
    suspend fun getCurrentNetworkId(): NetworkId
    suspend fun setNetworkAndGateway(newUrl: String, networkName: String)
    suspend fun hasAccountOnNetwork(newUrl: String, networkName: String): Boolean
    val profile: Flow<Profile?>
    suspend fun getCurrentNetworkBaseUrl(): String
    suspend fun getSignersForAddresses(networkId: Int, addresses: List<String>): List<AccountSigner>
    suspend fun getAccounts(): List<Account>
    suspend fun getPrivateKey(): PrivateKey
}

class ProfileRepositoryImpl @Inject constructor(
    private val encryptedPreferencesManager: EncryptedPreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getMnemonicUseCase: GetMnemonicUseCase,
) : ProfileRepository {

    override val profile: Flow<Profile?> = encryptedPreferencesManager.encryptedProfile
        .map { profileContent ->
            profileContent?.let { profile ->
                Json.decodeFromString<ProfileSnapshot>(profile).toProfile()
            } ?: run {
                null
            }
        }
        .flowOn(ioDispatcher)

    override val p2pClient: Flow<P2PClient?> = profile.map { profile ->
        profile?.appPreferences?.p2pClients?.firstOrNull()
    }

    override suspend fun readMnemonic(key: String): String? {
        return encryptedPreferencesManager.readMnemonic(key)
    }

    override suspend fun saveProfile(profile: Profile) {
        withContext(ioDispatcher) {
            val profileContent = Json.encodeToString(profile.snapshot())
            encryptedPreferencesManager.putProfileBytes(profileContent.toByteArray())
        }
    }

    override suspend fun setNetworkAndGateway(newUrl: String, networkName: String) {
        readProfile()?.let { profile ->
            val updatedProfile = profile.setNetworkAndGateway(
                NetworkAndGateway(
                    gatewayAPIEndpointURL = newUrl,
                    network = Network.allKnownNetworks().first { it.name == networkName }
                )
            )
            saveProfile(updatedProfile)
        }
    }

    override suspend fun hasAccountOnNetwork(newUrl: String, networkName: String): Boolean {
        val knownNetwork = Network.allKnownNetworks().firstOrNull { it.name == networkName } ?: return false
        val newNetworkAndGateway = NetworkAndGateway(
            gatewayAPIEndpointURL = newUrl,
            network = knownNetwork
        )
        return readProfile()?.let { profile ->
            profile.perNetwork.any { it.networkID == newNetworkAndGateway.network.id }
        } ?: false
    }

    override suspend fun getCurrentNetworkId(): NetworkId {
        return getNetworkAndGateway().network.networkId()
    }

    override suspend fun getCurrentNetworkBaseUrl(): String {
        return getNetworkAndGateway().gatewayAPIEndpointURL
    }

    override suspend fun readProfile(): Profile? {
        return profile.firstOrNull()
    }

    override suspend fun getSignersForAddresses(networkId: Int, addresses: List<String>): List<AccountSigner> {
        val profile = readProfile()
        val accounts = getSignerAccountsForAddresses(profile, addresses, networkId)
        val factorSourceId = profile?.notaryFactorSource()?.factorSourceID
        assert(factorSourceId != null)
        val mnemonic = getMnemonicUseCase(factorSourceId)
        assert(mnemonic.isNotEmpty())
        val mnemonicWords = MnemonicWords(mnemonic)
        val signers = mutableListOf<AccountSigner>()
        accounts.forEach {
            val privateKey = mnemonicWords.signerPrivateKey(derivationPath = it.derivationPath)
            signers.add(AccountSigner(it, privateKey))
        }
        return signers.toList()
    }

    override suspend fun getPrivateKey(): PrivateKey {
        val profile = readProfile()
        val networkId = getCurrentNetworkId()
        val factorSourceId = profile?.notaryFactorSource()?.factorSourceID
        assert(factorSourceId != null)
        val mnemonic = getMnemonicUseCase(factorSourceId)
        assert(mnemonic.isNotEmpty())
        val mnemonicWords = MnemonicWords(mnemonic)
        val account = profile?.perNetwork?.firstOrNull { it.networkID == networkId.value }?.accounts?.first()
            ?: error("No account for network!")
        return mnemonicWords.signerPrivateKey(derivationPath = account.derivationPath)
    }

    private suspend fun getSignerAccountsForAddresses(
        profile: Profile?,
        addresses: List<String>,
        networkId: Int,
    ): List<Account> {
        val accounts = if (addresses.isNotEmpty()) {
            addresses.mapNotNull { address ->
                getAccount(address)
            }
        } else {
            listOfNotNull(profile?.perNetwork?.firstOrNull { it.networkID == networkId }?.accounts?.first())
        }
        return accounts
    }

    private suspend fun getNetworkAndGateway(): NetworkAndGateway {
        return readProfile()?.appPreferences?.networkAndGateway ?: NetworkAndGateway.nebunet
    }

    override suspend fun getAccounts(): List<Account> {
        return readProfile()?.perNetwork
            ?.firstOrNull { it.networkID == getCurrentNetworkId().value }?.accounts.orEmpty()
    }

    private suspend fun getAccount(address: String): Account? {
        val networkId = getCurrentNetworkId()
        val perNetwork = readProfile()?.perNetwork?.firstOrNull { it.networkID == networkId.value }
        return perNetwork?.accounts?.firstOrNull { it.entityAddress.address == address }
    }
}
