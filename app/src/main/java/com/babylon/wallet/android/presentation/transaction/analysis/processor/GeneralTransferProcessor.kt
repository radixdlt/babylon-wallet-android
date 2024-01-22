package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.usecases.ResolveDAppInTransactionUseCase
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.defaultDepositGuarantee
import javax.inject.Inject

class GeneralTransferProcessor @Inject constructor(
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
    private val getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveDAppInTransactionUseCase: ResolveDAppInTransactionUseCase
) : PreviewTypeProcessor<DetailedManifestClass.General> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.General): PreviewType {
        val badges = getTransactionBadgesUseCase(accountProofs = summary.presentedProofs)
        val dApps = summary.resolveDApps(resolveDAppInTransactionUseCase).distinctBy {
            it.first.definitionAddresses
        }
        val involvedAccountAddresses = summary.accountWithdraws.keys + summary.accountDeposits.keys
        val allOwnedAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
            involvedAccountAddresses.contains(it.address)
        }

        val assets = resolveAssetsFromAddressUseCase(
            fungibleAddresses = summary.involvedFungibleAddresses,
            nonFungibleIds = summary.involvedNonFungibleIds,
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

    private suspend fun ExecutionSummary.resolveDApps(
        resolveDAppInTransactionUseCase: ResolveDAppInTransactionUseCase
    ) = coroutineScope {
        encounteredEntities.filter { it.isGlobalComponent() }
            .map { address ->
                async {
                    resolveDAppInTransactionUseCase.invoke(address.addressString())
                }
            }
            .awaitAll()
            .mapNotNull { it.getOrNull() }
    }
}
