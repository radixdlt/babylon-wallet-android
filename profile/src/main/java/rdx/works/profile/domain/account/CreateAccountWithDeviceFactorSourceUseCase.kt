package rdx.works.profile.domain.account

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Account.Companion.initAccountWithDeviceFactorSource
import rdx.works.profile.data.model.pernetwork.addAccount
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class CreateAccountWithDeviceFactorSourceUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        displayName: String,
        networkID: NetworkId? = null
    ): Network.Account {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()

            val factorSource = profile.babylonDeviceFactorSource

            // Construct new account
            val networkId = networkID ?: profile.currentNetwork.knownNetworkId ?: Radix.Gateway.default.network.networkId()
            val totalAccountsOnNetwork = profile.currentNetwork.accounts.size
            val newAccount = initAccountWithDeviceFactorSource(
                displayName = displayName,
                mnemonicWithPassphrase = mnemonicRepository(mnemonicKey = factorSource.id),
                deviceFactorSource = factorSource,
                networkId = networkId,
                appearanceID = totalAccountsOnNetwork % Network.Account.AppearanceIdGradient.values().count()
            )
            // Add account to the profile
            val updatedProfile = profile.addAccount(
                account = newAccount,
                withFactorSourceId = factorSource.id,
                onNetwork = networkId
            )
            // Save updated profile
            profileRepository.saveProfile(updatedProfile)
            // Return new account
            newAccount
        }
    }
}
