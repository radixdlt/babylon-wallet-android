package com.babylon.wallet.android.domain.usecases

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
import rdx.works.profile.data.model.pernetwork.addAccounts
import rdx.works.profile.data.model.pernetwork.nextAccountIndex
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class CreateAccountWithLedgerFactorSourceUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val resolveAccountsLedgerStateUseCase: ResolveAccountsLedgerStateUseCase,
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
            val networkId = networkID ?: profile.currentNetwork?.knownNetworkId ?: Radix.Gateway.default.network.networkId()
            val totalAccountsOnNetwork = profile.networks.find { it.networkID == networkId.value }?.accounts?.size ?: 0
            val newAccount = Network.Account.initAccountWithLedgerFactorSource(
                entityIndex = profile.nextAccountIndex(derivationPath.scheme, networkId, ledgerFactorSourceID),
                displayName = displayName,
                derivedPublicKeyHex = derivedPublicKeyHex,
                ledgerFactorSource = ledgerHardwareWalletFactorSource,
                networkId = networkId,
                derivationPath = derivationPath,
                appearanceID = totalAccountsOnNetwork % AccountGradientList.count()
            )
            val resolveResult = resolveAccountsLedgerStateUseCase.invoke(listOf(newAccount))
            // Add account to the profile
            val updatedProfile = if (resolveResult.isSuccess) {
                profile.addAccounts(
                    accounts = listOf(resolveResult.getOrThrow().first().account),
                    onNetwork = networkId
                )
            } else {
                profile.addAccounts(
                    accounts = listOf(newAccount),
                    onNetwork = networkId
                )
            }
            // Save updated profile
            profileRepository.saveProfile(updatedProfile)
            // Return new account
            newAccount
        }
    }
}
