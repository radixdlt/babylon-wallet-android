package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.usecases.ResolveComponentAddressesUseCase
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class GeneralTransferProcessor @Inject constructor(
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
    private val getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveComponentAddressesUseCase: ResolveComponentAddressesUseCase
) : PreviewTypeProcessor<DetailedManifestClass.General> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.General): PreviewType {
        val badges = getTransactionBadgesUseCase(addresses = summary.presentedProofs.toSet()).getOrThrow()
        val dApps = summary.resolveDApps()
        val allOwnedAccounts = summary.involvedOwnedAccounts(getProfileUseCase().activeAccountsOnCurrentNetwork)
        val assets = resolveAssetsFromAddressUseCase(addresses = summary.involvedAddresses()).getOrThrow()

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
            dApps = dApps
        )
    }

    private suspend fun ExecutionSummary.resolveDApps() = coroutineScope {
        encounteredComponentAddresses
            .map { address ->
                async { resolveComponentAddressesUseCase.invoke(address) }
            }
            .awaitAll()
            .mapNotNull { it.getOrNull() }
            .distinctBy { it.first }
    }
}
