package com.babylon.wallet.android.presentation.transaction.analysis.summary.manifest

import com.babylon.wallet.android.domain.usecases.ResolveComponentAddressesUseCase
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.analysis.summary.Summary
import com.babylon.wallet.android.presentation.transaction.analysis.summary.SummaryToPreviewTypeAnalyzer
import com.radixdlt.sargon.ManifestClass
import com.radixdlt.sargon.ManifestSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class ManifestSummaryToPreviewTypeAnalyser @Inject constructor(
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveComponentAddressesUseCase: ResolveComponentAddressesUseCase
) : SummaryToPreviewTypeAnalyzer<Summary.FromStaticAnalysis> {

    override suspend fun analyze(summary: Summary.FromStaticAnalysis): PreviewType {
        val isConforming = summary.summary.classification.firstOrNull()?.isConforming ?: false
        if (!isConforming) return PreviewType.NonConforming

        val assets = resolveAssetsFromAddressUseCase(addresses = summary.summary.involvedResourceAddresses()).getOrThrow()
        val profile = getProfileUseCase()

        val (withdraws, deposits) = summary.summary.resolveWithdrawsAndDeposits(
            onLedgerAssets = assets,
            profile = profile
        )

        return PreviewType.Transaction(
            from = withdraws,
            to = deposits,
            involvedComponents = PreviewType.Transaction.InvolvedComponents.DApps(
                components = summary.summary.resolveDApps(),
                morePossibleDAppsPresent = true
            ),
            badges = summary.summary.resolveBadges(onLedgerAssets = assets),
        )
    }

    private suspend fun ManifestSummary.resolveDApps() = coroutineScope {
        encounteredEntities
            .map { address ->
                async { resolveComponentAddressesUseCase.invoke(address) }
            }
            .awaitAll()
            .mapNotNull { it.getOrNull() }
            .distinctBy { it.first }
    }

    private val ManifestClass.isConforming: Boolean
        get() = this == ManifestClass.GENERAL_SUBINTENT
}
