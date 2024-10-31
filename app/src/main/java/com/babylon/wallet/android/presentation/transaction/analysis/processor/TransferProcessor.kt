package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class TransferProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) : PreviewTypeProcessor<DetailedManifestClass.Transfer> {

    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.Transfer): PreviewType {
        val assets = resolveAssetsFromAddressUseCase(addresses = summary.involvedAddresses()).getOrThrow()
        val badges = summary.resolveBadges(onLedgerAssets = assets)
        val (withdraws, deposits) = summary.resolveWithdrawsAndDeposits(
            onLedgerAssets = assets,
            profile = getProfileUseCase()
        )

        return PreviewType.Transfer.GeneralTransfer(
            from = withdraws,
            to = deposits,
            badges = badges,
            newlyCreatedNFTItems = summary.resolveNewlyCreatedNFTs()
        )
    }
}
