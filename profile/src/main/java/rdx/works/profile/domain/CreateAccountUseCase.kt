package rdx.works.profile.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.changeGateway
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.OnNetwork.Account.Companion.init
import rdx.works.profile.data.model.pernetwork.addAccount
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val getMnemonicUseCase: GetMnemonicUseCase,
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

            val factorSource = profile.babylonDeviceFactorSource

            // Construct new account
            val newAccount = init(
                displayName = displayName,
                mnemonicWithPassphrase = getMnemonicUseCase(mnemonicKey = factorSource.id),
                factorSource = factorSource,
                networkId = networkID
            )

            // Add account to the profile
            var updatedProfile = profile.addAccount(
                account = newAccount,
                withFactorSourceId = factorSource.id,
                onNetwork = networkID
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
