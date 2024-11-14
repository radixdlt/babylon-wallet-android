package com.babylon.wallet.android.presentation.transaction.analysis.summary.execution

import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.ExecutionSummary
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class AccountDeletionProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) {

    suspend fun process(
        summary: ExecutionSummary,
        deletingAccount: Account
    ): PreviewType {
        val assets = resolveAssetsFromAddressUseCase(addresses = summary.involvedAddresses()).getOrThrow()
        val deposit = summary.resolveAccountDeletion(
            onLedgerAssets = assets,
            profile = getProfileUseCase()
        )

        return PreviewType.DeleteAccount(
            deletingAccount = deletingAccount,
            to = deposit
        )
    }
}
