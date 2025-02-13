package com.babylon.wallet.android.presentation.transaction.analysis.summary.execution

import com.babylon.wallet.android.domain.usecases.ResolveComponentAddressesUseCase
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.ExecutionSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class GeneralTransferProcessor @Inject constructor(
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveComponentAddressesUseCase: ResolveComponentAddressesUseCase
) {

    suspend fun process(summary: ExecutionSummary): PreviewType {
        val dApps = summary.resolveDApps()
        val assets = resolveAssetsFromAddressUseCase(addresses = summary.involvedAddresses()).getOrThrow()
        val badges = summary.resolveBadges(onLedgerAssets = assets)

        val (withdraws, deposits) = summary.resolveWithdrawsAndDeposits(
            onLedgerAssets = assets,
            profile = getProfileUseCase()
        )

        return PreviewType.Transaction(
            from = withdraws,
            to = deposits,
            involvedComponents = PreviewType.Transaction.InvolvedComponents.DApps(components = dApps),
            badges = badges,
            newlyCreatedGlobalIds = summary.newlyCreatedNonFungibles
        )
    }

    private suspend fun ExecutionSummary.resolveDApps() = coroutineScope {
        encounteredAddresses
            .map { address ->
                async { resolveComponentAddressesUseCase.invoke(address) }
            }
            .awaitAll()
            .mapNotNull { it.getOrNull() }
            .distinctBy { it.first }
    }
}
