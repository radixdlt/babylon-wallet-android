package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.usecases.ResolveComponentAddressesUseCase
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.core.ret.isGlobalComponent
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.defaultDepositGuarantee
import javax.inject.Inject

class GeneralTransferProcessor @Inject constructor(
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
    private val getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveComponentAddressesUseCase: ResolveComponentAddressesUseCase
) : PreviewTypeProcessor<DetailedManifestClass.General> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.General): PreviewType {
        val badges = getTransactionBadgesUseCase(accountProofs = summary.presentedProofs)
        val dApps = summary.resolveDApps()
        val allOwnedAccounts = summary.involvedOwnedAccounts(getProfileUseCase.accountsOnCurrentNetwork())
        val assets = resolveAssetsFromAddressUseCase(
            fungibleAddresses = summary.involvedFungibleAddresses(),
            nonFungibleIds = summary.involvedNonFungibleIds()
        ).getOrThrow()

        return PreviewType.Transfer.GeneralTransfer(
            from = summary.toWithdrawingAccountsWithTransferableAssets(
                involvedAssets = assets,
                allOwnedAccounts = allOwnedAccounts
            ),
            to = summary.toDepositingAccountsWithTransferableAssets(
                involvedAssets = assets,
                allOwnedAccounts = allOwnedAccounts,
                defaultGuarantee = getProfileUseCase.defaultDepositGuarantee()
            ),
            badges = badges,
            dApps = dApps
        )
    }

    private suspend fun ExecutionSummary.resolveDApps() = coroutineScope {
        encounteredEntities.filter { it.entityType().isGlobalComponent() }
            .map { address ->
                async {
                    resolveComponentAddressesUseCase.invoke(address.addressString())
                }
            }
            .awaitAll()
            .mapNotNull { it.getOrNull() }
            .distinctBy { it.first }
    }
}
