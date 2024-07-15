package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.AccountWithDepositSettingsChanges
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.ResourcePreference
import com.radixdlt.sargon.ResourcePreferenceUpdate
import rdx.works.core.domain.assets.Asset
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class AccountDepositSettingsProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) : PreviewTypeProcessor<DetailedManifestClass.AccountDepositSettingsUpdate> {
    override suspend fun process(
        summary: ExecutionSummary,
        classification: DetailedManifestClass.AccountDepositSettingsUpdate
    ): PreviewType {
        val assets = resolveAssetsFromAddressUseCase(
            addresses = classification.involvedResourceAddresses + summary.proofAddresses.toSet()
        ).getOrThrow()
        val badges = summary.resolveBadges(assets)
        val involvedAccountAddresses = classification.depositModeUpdates.keys +
            classification.resourcePreferencesUpdates.keys +
            classification.authorizedDepositorsAdded.keys +
            classification.authorizedDepositorsRemoved.keys
        val involvedAccounts = getProfileUseCase().activeAccountsOnCurrentNetwork.filter {
            it.address in involvedAccountAddresses
        }
        val result = involvedAccounts.map { involvedAccount ->
            val defaultDepositRule = classification.depositModeUpdates[involvedAccount.address]
            val assetChanges = classification.resolveAssetChanges(involvedAccount, assets)
            val depositorChanges = classification.resolveDepositorChanges(involvedAccount, assets)
            AccountWithDepositSettingsChanges(
                account = involvedAccount,
                defaultDepositRule = when (defaultDepositRule) {
                    DepositRule.ACCEPT_ALL -> DepositRule.ACCEPT_ALL
                    DepositRule.DENY_ALL -> DepositRule.DENY_ALL
                    DepositRule.ACCEPT_KNOWN -> DepositRule.ACCEPT_KNOWN
                    null -> null
                },
                assetChanges = assetChanges,
                depositorChanges = depositorChanges
            )
        }
        return PreviewType.AccountsDepositSettings(result, badges)
    }

    private val DetailedManifestClass.AccountDepositSettingsUpdate.involvedResourceAddresses
        get() = resourcePreferencesUpdates.values.map {
            it.keys
        }.flatten().map {
            ResourceOrNonFungible.Resource(it)
        }.toSet() + (authorizedDepositorsRemoved.values.flatten() + authorizedDepositorsAdded.values.flatten()).toSet()

    private fun DetailedManifestClass.AccountDepositSettingsUpdate.resolveDepositorChanges(
        involvedAccount: Account,
        assets: List<Asset>
    ) = authorizedDepositorsAdded[involvedAccount.address]?.let { authorizedDepositorsChangeForAccount ->
        val added = authorizedDepositorsChangeForAccount.map { added ->
            when (added) {
                is ResourceOrNonFungible.NonFungible -> {
                    val resource = assets.find { it.resource.address == added.value.resourceAddress }?.resource
                    AccountWithDepositSettingsChanges.DepositorPreferenceChange(
                        change = AccountWithDepositSettingsChanges.DepositorPreferenceChange.ChangeType.Add,
                        resource = resource
                    )
                }

                is ResourceOrNonFungible.Resource -> {
                    val resource = assets.find { it.resource.address == added.value }?.resource
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
                    val resource = assets.find { it.resource.address == removed.value }?.resource
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
        involvedAccount: Account,
        allResources: List<Asset>
    ) = resourcePreferencesUpdates[involvedAccount.address]?.let { resourcePreferenceChangeForAccount ->
        resourcePreferenceChangeForAccount.map { resourcePreferenceChange ->
            val resource = allResources.find { it.resource.address == resourcePreferenceChange.key }?.resource
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
}
