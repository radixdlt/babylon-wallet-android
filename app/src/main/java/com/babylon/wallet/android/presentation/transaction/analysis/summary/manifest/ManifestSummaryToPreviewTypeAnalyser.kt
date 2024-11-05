package com.babylon.wallet.android.presentation.transaction.analysis.summary.manifest

import com.babylon.wallet.android.domain.usecases.ResolveComponentAddressesUseCase
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.analysis.summary.Summary
import com.babylon.wallet.android.presentation.transaction.analysis.summary.SummaryToPreviewTypeAnalyzer
import com.radixdlt.sargon.DappToWalletInteractionSubintentExpiration
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.ManifestSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class ManifestSummaryToPreviewTypeAnalyser @Inject constructor(
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveComponentAddressesUseCase: ResolveComponentAddressesUseCase
) : SummaryToPreviewTypeAnalyzer<Summary.FromStaticAnalysis> {

    private fun DappToWalletInteractionSubintentExpiration?.toExpiration() = when (this) {
        is DappToWalletInteractionSubintentExpiration.AtTime -> PreviewType.PreAuthTransaction.Expiration.AtTime(
            timestamp = v1.unixTimestampSeconds
        )
        is DappToWalletInteractionSubintentExpiration.AfterDelay -> PreviewType.PreAuthTransaction.Expiration.DelayAfterSign(
            delay = v1.expireAfterSeconds.toLong().seconds
        )
        null -> PreviewType.PreAuthTransaction.Expiration.None
    }

    override suspend fun analyze(summary: Summary.FromStaticAnalysis): PreviewType {
        val assets = resolveAssetsFromAddressUseCase(addresses = summary.summary.involvedResourceAddresses()).getOrThrow()
        val profile = getProfileUseCase()

        val (withdraws, deposits) = summary.summary.resolveWithdrawsAndDeposits(
            onLedgerAssets = assets,
            profile = profile
        )

        return PreviewType.PreAuthTransaction(
            from = withdraws,
            to = deposits,
            dApps = summary.summary.resolveDApps(),
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

}
