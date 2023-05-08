package rdx.works.profile.domain.account

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Account.Companion.initAccountWithLedgerFactorSource
import rdx.works.profile.data.model.pernetwork.addAccount
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class CreateAccountWithLedgerFactorSourceUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        displayName: String,
        derivedPublicKeyHex: String,
        factorSourceID: FactorSource.ID,
        derivationPath: DerivationPath,
        networkID: NetworkId? = null
    ): Network.Account {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()

            val factorSource = profile.factorSources.first { it.id == factorSourceID }
            // Construct new account
            val networkId = networkID ?: profile.currentNetwork.knownNetworkId ?: Radix.Gateway.default.network.networkId()
            val newAccount = initAccountWithLedgerFactorSource(
                displayName = displayName,
                derivedPublicKeyHex = derivedPublicKeyHex,
                ledgerFactorSource = factorSource,
                networkId = networkId,
                derivationPath = derivationPath
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
