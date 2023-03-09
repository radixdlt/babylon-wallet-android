package rdx.works.profile.domain

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.extensions.addAccountOnNetwork
import rdx.works.profile.data.extensions.changeGateway
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.OnNetwork.Account.Companion.createNewVirtualAccount
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.data.utils.accountsPerNetworkCount
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val generateMnemonicUseCase: GetMnemonicUseCase,
    private val profileDataSource: ProfileDataSource,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        displayName: String,
        networkUrl: String? = null,
        networkName: String? = null,
        switchNetwork: Boolean = false
    ): OnNetwork.Account {
        return withContext(defaultDispatcher) {
            val profile = profileDataSource.readProfile()
            checkNotNull(profile) {
                "Profile does not exist"
            }

            var gateway: Gateway? = null
            if (networkUrl != null && networkName != null) {
                gateway = Gateway(
                    url = networkUrl,
                    network = Network.allKnownNetworks().first { network ->
                        network.name == networkName
                    }
                )
            }
            val networkID = gateway?.network?.networkId() ?: profileDataSource.getCurrentNetworkId()

            // Construct new account
            val newAccount = createNewVirtualAccount(
                displayName = displayName,
                entityIndex = profile.onNetwork.accountsPerNetworkCount(networkID),
                mnemonic = MnemonicWords(
                    phrase = generateMnemonicUseCase(
                        mnemonicKey = profile.factorSources
                            .curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources
                            .first().factorSourceID
                    )
                ),
                factorSources = profile.factorSources,
                networkId = networkID
            )

            // Add account to the profile
            var updatedProfile = profile.addAccountOnNetwork(
                newAccount,
                networkID = networkID
            )
            if (switchNetwork && gateway != null) {
                updatedProfile = updatedProfile.changeGateway(gateway)
            }
            // Save updated profile
            profileDataSource.saveProfile(updatedProfile)
            // Return new account
            newAccount
        }
    }
}
