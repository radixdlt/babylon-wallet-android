package rdx.works.profile.domain

import com.radixdlt.bip39.model.MnemonicWords
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.extensions.addAccountOnNetwork
import rdx.works.profile.data.extensions.changeGateway
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.OnNetwork.Account.Companion.createNewVirtualAccount
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.di.coroutines.DefaultDispatcher

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

            // TODO(ABW-1023)
            val factorSource = profile.factorSources.first()

            // Get the next index to derive the new account based in this factor source
            val entityIndex = factorSource.getNextAccountDerivationIndex(forNetworkId = networkID)

            // Construct new account
            val newAccount = createNewVirtualAccount(
                displayName = displayName,
                entityIndex = entityIndex,
                mnemonic = MnemonicWords(
                    phrase = generateMnemonicUseCase(mnemonicKey = factorSource.id)
                ),
                factorSource = factorSource,
                networkId = networkID
            )

            // Add account to the profile
            var updatedProfile = profile.addAccountOnNetwork(
                account = newAccount,
                factorSourceId = factorSource.id,
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
