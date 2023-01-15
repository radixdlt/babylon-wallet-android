package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.mockdata.account1
import com.babylon.wallet.android.mockdata.account2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.apppreferences.P2PClient
import rdx.works.profile.data.model.factorsources.FactorSources
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.PerNetwork
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.derivation.model.NetworkId

class ProfileRepositoryFake : ProfileRepository {

    private val profileFake = Profile(
        appPreferences = AppPreferences(
            display = Display.default,
            networkAndGateway = NetworkAndGateway.nebunet,
            p2pClients = emptyList()
        ),
        factorSources = FactorSources(
            curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources = emptyList(),
            secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources = emptyList()
        ),
        perNetwork = listOf(
            PerNetwork(
                accounts = listOf(account1, account2),
                connectedDapps = emptyList(),
                networkID = NetworkId.Nebunet.value,
                personas = emptyList()
            )
        ),
        version = "version"
    )

    override suspend fun saveProfile(profile: Profile) {
        TODO("Not yet implemented")
    }

    override suspend fun readProfile(): Profile {
        return profileFake
    }

    override suspend fun readMnemonic(key: String): String? {
        TODO("Not yet implemented")
    }

    override val p2pClient: Flow<P2PClient?>
        get() = TODO("Not yet implemented")

    override val profile: Flow<Profile?> = flowOf(profileFake)

    override suspend fun getAccounts(): List<Account> {
        TODO("Not yet implemented")
    }

    override suspend fun getAccount(address: String): Account? {
        TODO("Not yet implemented")
    }

    override suspend fun clear() {
        TODO("Not yet implemented")
    }
}
