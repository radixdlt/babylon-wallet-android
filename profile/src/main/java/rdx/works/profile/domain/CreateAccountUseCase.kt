package rdx.works.profile.domain

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.extensions.addAccountOnNetwork
import rdx.works.profile.data.extensions.setNetworkAndGateway
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.createNewVirtualAccount
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.utils.accountsPerNetworkCount
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val generateMnemonicUseCase: GetMnemonicUseCase,
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        displayName: String,
        networkUrl: String? = null,
        networkName: String? = null,
        switchNetwork: Boolean = false
    ): Account {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.readProfile()
            checkNotNull(profile) {
                "Profile does not exist"
            }

            var networkAndGateway: NetworkAndGateway? = null
            if (networkUrl != null && networkName != null) {
                networkAndGateway =
                    NetworkAndGateway(networkUrl, Network.allKnownNetworks().first { it.name == networkName })
            }
            val networkID = networkAndGateway?.network?.networkId() ?: profileRepository.getCurrentNetworkId()
            // Construct new account
            val newAccount = createNewVirtualAccount(
                displayName = displayName,
                entityIndex = profile.perNetwork.accountsPerNetworkCount(networkID),
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
            if (switchNetwork && networkAndGateway != null) {
                updatedProfile = updatedProfile.setNetworkAndGateway(networkAndGateway)
            }
            // Save updated profile
            profileRepository.saveProfile(updatedProfile)
            // Return new account
            newAccount
        }
    }
}
