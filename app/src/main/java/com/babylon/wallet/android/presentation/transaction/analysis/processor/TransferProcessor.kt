package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import rdx.works.core.domain.resources.Resource
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class TransferProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) : PreviewTypeProcessor<DetailedManifestClass.Transfer> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.Transfer): PreviewType {
        val assets = resolveAssetsFromAddressUseCase(addresses = summary.involvedAddresses()).getOrThrow()

        val involvedAccountAddresses = summary.deposits.keys + summary.withdrawals.keys
        val allOwnedAccounts = getProfileUseCase().activeAccountsOnCurrentNetwork.filter {
            it.address in involvedAccountAddresses
        }
        val badges = summary.resolveBadges(assets = assets)

        return PreviewType.Transfer.GeneralTransfer(
            from = summary.toWithdrawingAccountsWithTransferableAssets(
                involvedAssets = assets,
                allOwnedAccounts = allOwnedAccounts
            ),
            to = summary.toDepositingAccountsWithTransferableAssets(
                involvedAssets = assets,
                allOwnedAccounts = allOwnedAccounts,
                defaultGuarantee = getProfileUseCase().appPreferences.transaction.defaultDepositGuarantee
            ),
            badges = badges,
            newlyCreatedNFTItems = summary.newlyCreatedNonFungibles.map {
                Resource.NonFungibleResource.Item(
                    it.resourceAddress,
                    it.nonFungibleLocalId
                )
            }
        )
    }
}
