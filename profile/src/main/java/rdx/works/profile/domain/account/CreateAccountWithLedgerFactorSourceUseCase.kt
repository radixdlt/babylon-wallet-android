package rdx.works.profile.domain.account

import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Account.Companion.initAccountWithLedgerFactorSource
import rdx.works.profile.data.model.pernetwork.addAccounts
import rdx.works.profile.data.model.pernetwork.nextAccountIndex
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
        ledgerFactorSourceID: FactorSource.FactorSourceID.FromHash,
        derivationPath: DerivationPath,
        networkID: NetworkId? = null
    ): Network.Account {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()

            val ledgerHardwareWalletFactorSource = profile.factorSources
                .first {
                    it.id == ledgerFactorSourceID
                } as LedgerHardwareWalletFactorSource
            // Construct new account
            val networkId = networkID ?: profile.currentNetwork.knownNetworkId ?: Radix.Gateway.default.network.networkId()
            val totalAccountsOnNetwork = profile.currentNetwork.accounts.size
            val newAccount = initAccountWithLedgerFactorSource(
                entityIndex = profile.nextAccountIndex(networkId, ledgerFactorSourceID),
                displayName = displayName,
                derivedPublicKeyHex = derivedPublicKeyHex,
                ledgerFactorSource = ledgerHardwareWalletFactorSource,
                networkId = networkId,
                derivationPath = derivationPath,
                appearanceID = totalAccountsOnNetwork % AccountGradientList.count()
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
