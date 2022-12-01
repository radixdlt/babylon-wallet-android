package rdx.works.profile.domain

import com.radixdlt.bip39.model.MnemonicWords
import rdx.works.profile.data.repository.AccountDerivationPath
import rdx.works.profile.data.repository.CompressedPublicKey
import rdx.works.profile.data.extensions.addAccountOnNetwork
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.createNewVirtualAccount
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.utils.accountsPerNetworkCount
import rdx.works.profile.derivation.model.NetworkId
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val generateMnemonicUseCase: GetMnemonicUseCase,
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(
        displayName: String
    ): Account {

        // Read profile first as its needed to create account
        profileRepository.readProfile()?.let { profile ->

            val networkID = profile.appPreferences.networkAndGateway.network.networkId()
            // Construct new account
            val newAccount = createNewVirtualAccount(
                displayName = displayName,
                entityDerivationPath = AccountDerivationPath(
                    perNetwork = profile.perNetwork,
                    networkId = networkID
                ),
                entityIndex = profile.perNetwork.accountsPerNetworkCount(networkID),
                derivePublicKey = CompressedPublicKey(
                    mnemonic = MnemonicWords(
                        phrase = generateMnemonicUseCase(
                            mnemonicKey = profile.factorSources
                                .curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources
                                .first().factorSourceID
                        )
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
            profileRepository.saveProfile(updatedProfile)

            // Return new account
            return newAccount
        }

        // We should never reach here
        throw IllegalStateException("Profile does not exist")
    }
}
