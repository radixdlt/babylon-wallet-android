package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.AccountWithDepositSettingsChanges
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ResourceOrNonFungible
import com.radixdlt.ret.ResourcePreference
import com.radixdlt.ret.ResourcePreferenceUpdate
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork

suspend fun DetailedManifestClass.AccountDepositSettingsUpdate.resolve(
    getProfileUseCase: GetProfileUseCase,
    allResources: List<Resource>
): PreviewType {
    val involvedAccountAddresses =
        depositModeUpdates.keys + resourcePreferencesUpdates.keys + authorizedDepositorsAdded.keys + authorizedDepositorsRemoved.keys
    val involvedAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter { involvedAccountAddresses.contains(it.address) }
    val result = involvedAccounts.map { involvedAccount ->
        val defaultDepositRule = depositModeUpdates[involvedAccount.address]
        val assetChanges = resolveAssetChanges(involvedAccount, allResources)
        val depositorChanges = resolveDepositorChanges(involvedAccount, allResources)
        AccountWithDepositSettingsChanges(
            account = involvedAccount,
            defaultDepositRule = defaultDepositRule,
            assetChanges = assetChanges,
            depositorChanges = depositorChanges
        )
    }
    return PreviewType.AccountsDepositSettings(result)
}

private fun DetailedManifestClass.AccountDepositSettingsUpdate.resolveDepositorChanges(
    involvedAccount: Network.Account,
    allResources: List<Resource>
) = authorizedDepositorsAdded[involvedAccount.address]?.let { authorizedDepositorsChangeForAccount ->
    val added = authorizedDepositorsChangeForAccount.map { added ->
        when (added) {
            is ResourceOrNonFungible.NonFungible -> {
                val resource = allResources.find { it.resourceAddress == added.value.resourceAddress().addressString() }
                AccountWithDepositSettingsChanges.DepositorPreferenceChange(
                    change = AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Add,
                    resource = resource
                )
            }

            is ResourceOrNonFungible.Resource -> {
                val resource = allResources.find { it.resourceAddress == added.value.addressString() }
                AccountWithDepositSettingsChanges.DepositorPreferenceChange(
                    change = AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Add,
                    resource = resource
                )
            }
        }
    }
    val removed = authorizedDepositorsRemoved[involvedAccount.address]?.map { removed ->
        when (removed) {
            is ResourceOrNonFungible.NonFungible -> {
                AccountWithDepositSettingsChanges.DepositorPreferenceChange(
                    change = AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Remove
                )
            }

            is ResourceOrNonFungible.Resource -> {
                val resource = allResources.find { it.resourceAddress == removed.value.addressString() }
                AccountWithDepositSettingsChanges.DepositorPreferenceChange(
                    change = AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Remove,
                    resource = resource
                )
            }
        }
    }.orEmpty()
    added + removed
}.orEmpty()

private fun DetailedManifestClass.AccountDepositSettingsUpdate.resolveAssetChanges(
    involvedAccount: Network.Account,
    allResources: List<Resource>
) = resourcePreferencesUpdates[involvedAccount.address]?.let { resourcePreferenceChangeForAccount ->
    resourcePreferenceChangeForAccount.map { resourcePreferenceChange ->
        val resource = allResources.find { it.resourceAddress == resourcePreferenceChange.key }
        val assetPreferenceChange = when (val action = resourcePreferenceChange.value) {
            ResourcePreferenceUpdate.Remove -> AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Clear
            is ResourcePreferenceUpdate.Set -> when (action.value) {
                ResourcePreference.ALLOWED -> AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Allow
                ResourcePreference.DISALLOWED -> AccountWithDepositSettingsChanges.AssetPreferenceChange.ChangeType.Disallow
            }
        }
        AccountWithDepositSettingsChanges.AssetPreferenceChange(
            resource = resource,
            change = assetPreferenceChange
        )
    }
}.orEmpty()
