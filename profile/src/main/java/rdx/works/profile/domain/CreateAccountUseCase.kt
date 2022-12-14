package rdx.works.profile.domain

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.extensions.addAccountOnNetwork
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.createNewVirtualAccount
import rdx.works.profile.data.repository.AccountDerivationPath
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.utils.accountsPerNetworkCount
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val generateMnemonicUseCase: GetMnemonicUseCase,
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        displayName: String
    ): Account {
        return withContext(defaultDispatcher) {
            val profileSnapshot = profileRepository.readProfileSnapshot()
            checkNotNull(profileSnapshot) {
                "Profile does not exist"
            }

            val profile = profileSnapshot.toProfile()

            val networkID = profile.appPreferences.networkAndGateway.network.networkId()
            // Construct new account
            val newAccount = createNewVirtualAccount(
                displayName = displayName,
                entityDerivationPath = AccountDerivationPath(
                    perNetwork = profile.perNetwork,
                    networkId = networkID
                ),
                entityIndex = profile.perNetwork.accountsPerNetworkCount(networkID),
                mnemonic = MnemonicWords(
                    phrase = generateMnemonicUseCase(
                        mnemonicKey = profile.factorSources
                            .curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources
                            .first().factorSourceID
                    )
                ),
                factorSources = profile.factorSources
            )

            // Add account to the profile
            val updatedProfile = profile.addAccountOnNetwork(
                newAccount,
                networkID = NetworkId.Hammunet
            )

            // Save updated profile
            profileRepository.saveProfileSnapshot(updatedProfile.snapshot())

            // Return new account
            newAccount
        }
    }
}
