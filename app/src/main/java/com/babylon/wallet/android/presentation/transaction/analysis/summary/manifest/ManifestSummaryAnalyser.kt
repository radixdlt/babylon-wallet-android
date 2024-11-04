package com.babylon.wallet.android.presentation.transaction.analysis.summary.manifest

import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.analysis.processor.involvedResourceAddresses
import com.babylon.wallet.android.presentation.transaction.analysis.processor.resolveWithdrawsAndDeposits
import com.radixdlt.sargon.DappToWalletInteractionSubintentExpiration
import com.radixdlt.sargon.ManifestSummary
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class ManifestSummaryAnalyser @Inject constructor(
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
    private val getProfileUseCase: GetProfileUseCase,
) {

    suspend fun analyse(
        summary: ManifestSummary,
        expiration: DappToWalletInteractionSubintentExpiration?
    ): PreviewType {
        val assets = resolveAssetsFromAddressUseCase(addresses = summary.involvedResourceAddresses()).getOrThrow()
        val profile = getProfileUseCase()

        val (withdraws, deposits) = summary.resolveWithdrawsAndDeposits(
            onLedgerAssets = assets,
            profile = profile
        )

        return PreviewType.PreAuthTransaction(
            from = withdraws,
            to = deposits,
            dApps = emptyList(), // TODO
            badges = emptyList(), // TODO
            expiration = expiration.toExpiration()
        )
    }

    private fun DappToWalletInteractionSubintentExpiration?.toExpiration() = when (this) {
        is DappToWalletInteractionSubintentExpiration.AtTime -> PreviewType.PreAuthTransaction.Expiration.AtTime(
            timestamp = v1.unixTimestampSeconds
        )
        is DappToWalletInteractionSubintentExpiration.AfterDelay -> PreviewType.PreAuthTransaction.Expiration.DelayAfterSign(
            delay = v1.expireAfterSeconds.toLong().seconds
        )
        null -> PreviewType.PreAuthTransaction.Expiration.None
    }

}