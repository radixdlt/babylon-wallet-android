package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithDepositSettingsChanges
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.ResourcePreference
import com.radixdlt.sargon.ResourcePreferenceUpdate
import com.radixdlt.sargon.extensions.init
import rdx.works.core.domain.assets.Asset
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.Account.OnLedgerSettings.ThirdPartyDeposits
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
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
            fungibleAddresses = summary.involvedFungibleAddresses(),
            nonFungibleIds = summary.involvedNonFungibleIds()
        ).getOrThrow()
        val involvedAccountAddresses = classification.depositModeUpdates.keys +
            classification.resourcePreferencesUpdates.keys +
            classification.authorizedDepositorsAdded.keys +
            classification.authorizedDepositorsRemoved.keys
        val involvedAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
            involvedAccountAddresses.contains(AccountAddress.init(it.address))
        }
        val result = involvedAccounts.map { involvedAccount ->
            val defaultDepositRule = classification.depositModeUpdates[AccountAddress.init(involvedAccount.address)]
            val assetChanges = classification.resolveAssetChanges(involvedAccount, assets)
            val depositorChanges = classification.resolveDepositorChanges(involvedAccount, assets)
            AccountWithDepositSettingsChanges(
                account = involvedAccount,
                defaultDepositRule = when (defaultDepositRule) {
                    DepositRule.ACCEPT_ALL -> ThirdPartyDeposits.DepositRule.AcceptAll
                    DepositRule.DENY_ALL -> ThirdPartyDeposits.DepositRule.DenyAll
                    DepositRule.ACCEPT_KNOWN -> ThirdPartyDeposits.DepositRule.AcceptKnown
                    null -> null
                },
                assetChanges = assetChanges,
                depositorChanges = depositorChanges
            )
        }
        return PreviewType.AccountsDepositSettings(result)
    }

    private fun DetailedManifestClass.AccountDepositSettingsUpdate.resolveDepositorChanges(
        involvedAccount: Network.Account,
        assets: List<Asset>
    ) = authorizedDepositorsAdded[AccountAddress.init(involvedAccount.address)]?.let { authorizedDepositorsChangeForAccount ->
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
        val removed = authorizedDepositorsRemoved[AccountAddress.init(involvedAccount.address)]?.map { removed ->
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
        involvedAccount: Network.Account,
        allResources: List<Asset>
    ) = resourcePreferencesUpdates[AccountAddress.init(involvedAccount.address)]?.let { resourcePreferenceChangeForAccount ->
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
