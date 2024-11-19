package com.babylon.wallet.android.presentation.transaction.analysis.summary.execution

import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.extensions.asIdentifiable
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class AccountDeletionProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) : PreviewTypeProcessor<DetailedManifestClass.DeleteAccounts> {

    override suspend fun process(
        summary: ExecutionSummary,
        classification: DetailedManifestClass.DeleteAccounts
    ): PreviewType {
        val profile = getProfileUseCase()
        val assets = resolveAssetsFromAddressUseCase(addresses = summary.involvedAddresses()).getOrThrow()
        val deletingAccount = requireNotNull(
            profile.activeAccountsOnCurrentNetwork.asIdentifiable()
                .getBy(classification.accountAddresses.first())
        )
        val deposit = summary.resolveAccountDeletion(
            deletingAccountAddress = deletingAccount.address,
            onLedgerAssets = assets,
            profile = profile
        )

        return PreviewType.DeleteAccount(
            deletingAccount = deletingAccount,
            to = deposit
        )
    }
}
