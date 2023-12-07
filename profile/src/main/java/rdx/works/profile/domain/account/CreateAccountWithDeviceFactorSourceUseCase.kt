package rdx.works.profile.domain.account

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Account.Companion.initAccountWithBabylonDeviceFactorSource
import rdx.works.profile.data.model.pernetwork.addAccounts
import rdx.works.profile.data.model.pernetwork.nextAccountIndex
import rdx.works.profile.data.model.pernetwork.nextAppearanceId
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.di.coroutines.DefaultDispatcher
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import javax.inject.Inject

class CreateAccountWithDeviceFactorSourceUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        displayName: String,
        networkID: NetworkId? = null
    ): Network.Account {
        return withContext(defaultDispatcher) {
            val profile = ensureBabylonFactorSourceExistUseCase()
            val factorSource = profile.babylonMainDeviceFactorSource

            // Construct new account
            val networkId = networkID ?: profile.currentNetwork.knownNetworkId ?: Radix.Gateway.default.network.networkId()
            val nextAccountIndex = profile.nextAccountIndex(networkId)
            val nextAppearanceId = profile.nextAppearanceId(networkId)
            val mnemonicWithPassphrase = requireNotNull(mnemonicRepository.readMnemonic(factorSource.id).getOrNull())
            val newAccount = initAccountWithBabylonDeviceFactorSource(
                entityIndex = nextAccountIndex,
                displayName = displayName,
                mnemonicWithPassphrase = mnemonicWithPassphrase,
                deviceFactorSource = factorSource,
                networkId = networkId,
                appearanceID = nextAppearanceId
            )
            // Add account to the profile
            val updatedProfile = profile.addAccounts(
                accounts = listOf(newAccount),
                onNetwork = networkId
            )
            // Save updated profile
            profileRepository.saveProfile(updatedProfile)
            // Return new account
            newAccount
        }
    }
}
