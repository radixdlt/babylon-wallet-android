package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.ResolveAccountsLedgerStateRepository
import com.babylon.wallet.android.presentation.accessfactorsource.AccessFactorSourceOutput
import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.extensions.createAccount
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.addAccounts
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.derivation.model.NetworkId
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val resolveAccountsLedgerStateRepository: ResolveAccountsLedgerStateRepository
) {

    suspend operator fun invoke(
        displayName: String,
        factorSource: FactorSource.CreatingEntity,
        publicKeyAndDerivationPath: AccessFactorSourceOutput.PublicKeyAndDerivationPath,
        onNetworkId: NetworkId?
    ): Network.Account {
        val currentProfile = profileRepository.profile.first()
        val networkId = onNetworkId ?: currentProfile.currentNetwork?.knownNetworkId ?: Radix.Gateway.default.network.networkId()

        val newAccount = currentProfile.createAccount(
            displayName = displayName,
            onNetworkId = networkId,
            factorSource = factorSource,
            derivationPath = publicKeyAndDerivationPath.derivationPath,
            compressedPublicKey = publicKeyAndDerivationPath.compressedPublicKey,
            onLedgerSettings = Network.Account.OnLedgerSettings.init()
        )

        val accountWithOnLedgerStatusResult = resolveAccountsLedgerStateRepository(listOf(newAccount))

        val accountToAdd = if (accountWithOnLedgerStatusResult.isSuccess) {
            accountWithOnLedgerStatusResult.getOrThrow().first().account
        } else {
            newAccount
        }

        val updatedProfile = currentProfile.addAccounts(
            accounts = listOf(accountToAdd),
            onNetwork = networkId
        )
        // Save updated profile
        profileRepository.saveProfile(updatedProfile)
        // Return new account
        return accountToAdd
    }
}
