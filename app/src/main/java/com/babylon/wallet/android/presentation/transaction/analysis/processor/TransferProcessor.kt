package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.ResourceIndicator
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import javax.inject.Inject

class TransferProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) : PreviewTypeProcessor<DetailedManifestClass.Transfer> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.Transfer): PreviewType {
        val resourceIndicators = summary.accountDeposits.values.flatten() + summary.accountWithdraws.values.flatten()

        val assets = resolveAssetsFromAddressUseCase(
            fungibleAddresses = resourceIndicators.mapNotNull { (it as? ResourceIndicator.Fungible)?.resourceAddress?.addressString() },
            nonFungibleIds = resourceIndicators.mapNotNull { it as? ResourceIndicator.NonFungible }.associate {
                it.resourceAddress.addressString() to it.localIds
            }
        ).getOrThrow()

        val involvedAccountAddresses = summary.accountDeposits.keys + summary.accountWithdraws.keys
        val allOwnedAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
            involvedAccountAddresses.contains(it.address)
        }.associateBy { it.address }


        return PreviewType.Transfer.GeneralTransfer(
            from = summary.accountWithdraws.map { entry ->
                entry.value.map {
                    Transferable.Withdrawing(it.toTransferableAsset(assets))
                }.toAccountWithTransferableResources(entry.key, allOwnedAccounts)
            },
            to = summary.accountDeposits.map { entry ->
                entry.value.map {
                    Transferable.Depositing(it.toTransferableAsset(assets))
                }.toAccountWithTransferableResources(entry.key, allOwnedAccounts)
            }
        )
    }

    private fun List<Transferable>.toAccountWithTransferableResources(
        accountAddress: String,
        ownedAccounts: Map<String, Network.Account>
    ): AccountWithTransferableResources {
        val ownedAccount = ownedAccounts[accountAddress]
        return if (ownedAccount != null) {
            AccountWithTransferableResources.Owned(ownedAccount, this)
        } else {
            AccountWithTransferableResources.Other(accountAddress, this)
        }
    }
}
