package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.ResolveAccountsLedgerStateRepository
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.NetworkId
import kotlinx.coroutines.flow.first
import rdx.works.core.sargon.addAccounts
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.initBabylon
import rdx.works.core.sargon.nextAppearanceId
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val resolveAccountsLedgerStateRepository: ResolveAccountsLedgerStateRepository
) {

    suspend operator fun invoke(
        displayName: DisplayName,
        factorSourceId: FactorSourceId.Hash,
        publicKeyAndDerivationPath: AccessFactorSourcesOutput.PublicKeyAndDerivationPath,
        onNetworkId: NetworkId? = null
    ): Account {
        val currentProfile = profileRepository.profile.first()
        val networkId = onNetworkId ?: currentProfile.currentGateway.network.id

        val newAccount = Account.initBabylon(
            networkId = networkId,
            displayName = displayName,
            publicKey = publicKeyAndDerivationPath.publicKey,
            derivationPath = publicKeyAndDerivationPath.derivationPath,
            factorSourceId = factorSourceId,
            customAppearanceId = currentProfile.nextAppearanceId(forNetworkId = networkId)
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
