package com.babylon.wallet.android.presentation.transfer.prepare

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.NonFungibleGlobalId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.ret.ManifestBuilder
import rdx.works.core.ret.buildSafely
import rdx.works.profile.data.model.pernetwork.Network
import timber.log.Timber
import java.math.BigDecimal

class PrepareManifestDelegate(
    private val state: MutableStateFlow<TransferViewModel.State>,
    private val incomingRequestRepository: IncomingRequestRepository
) {

    suspend fun onSubmit() {
        val fromAccount = state.value.fromAccount ?: return
        prepareRequest(fromAccount, state.value).onSuccess { request ->
            state.update { it.copy(transferRequestId = request.requestId) }
            Timber.d("Manifest for ${request.requestId} prepared:")
            Timber.d(request.transactionManifestData.instructions)
            incomingRequestRepository.add(request)
        }.onFailure { error ->
            state.update { it.copy(error = UiMessage.ErrorMessage.from(error)) }
        }
    }

    private fun prepareRequest(
        fromAccount: Network.Account,
        currentState: TransferViewModel.State
    ): Result<MessageFromDataChannel.IncomingRequest.TransactionRequest> = ManifestBuilder()
        .attachInstructionsForFungibles(
            fromAccount = fromAccount,
            targetAccounts = currentState.targetAccounts
        )
        .attachInstructionsForNFTs(
            fromAccount = fromAccount,
            targetAccounts = currentState.targetAccounts
        )
        .buildSafely(fromAccount.networkID)
        .map { manifest ->
            manifest.prepareInternalTransactionRequest(
                networkId = fromAccount.networkID,
                message = currentState.submittedMessage,
            )
        }

    @Suppress("NestedBlockDepth")
    private fun ManifestBuilder.attachInstructionsForFungibles(
        fromAccount: Network.Account,
        targetAccounts: List<TargetAccount>
    ) = apply {
        state.value.withdrawingFungibles().forEach { (resource, amount) ->
            // Withdraw the total amount for each fungible
            withdraw(
                fromAddress = Address(fromAccount.address),
                fungible = Address(resource.resourceAddress),
                amount = Decimal(amount.toPlainString())
            )

            // Deposit to each target account
            targetAccounts.filter { targetAccount ->
                targetAccount.assets.any { it.address == resource.resourceAddress }
            }.forEach { targetAccount ->
                val spendingFungibleAsset = targetAccount.assets.find { it.address == resource.resourceAddress } as? SpendingAsset.Fungible
                if (spendingFungibleAsset != null) {
                    val bucket = newBucket()

                    // First take the correct amount from worktop and pour it into bucket
                    takeFromWorktop(
                        fungible = Address(resource.resourceAddress),
                        amount = Decimal(spendingFungibleAsset.amountDecimal.toPlainString()),
                        intoBucket = bucket
                    )

                    // Then deposit the bucket into the target account
                    deposit(
                        toAddress = Address(targetAccount.address),
                        fromBucket = bucket
                    )
                }
            }
        }
    }

    private fun ManifestBuilder.attachInstructionsForNFTs(
        fromAccount: Network.Account,
        targetAccounts: List<TargetAccount>
    ) = apply {
        targetAccounts.forEach { targetAccount ->
            val nonFungibleSpendingAssets = targetAccount.assets.filterIsInstance<SpendingAsset.NFT>()
            nonFungibleSpendingAssets.forEach { nft ->
                val bucket = newBucket()

                val globalId = NonFungibleGlobalId.fromParts(
                    resourceAddress = Address(nft.item.collectionAddress),
                    nonFungibleLocalId = nft.item.localId.toRetId()
                )
                withdraw(
                    fromAddress = Address(fromAccount.address),
                    nonFungible = globalId
                )
                takeFromWorktop(
                    nonFungible = globalId,
                    intoBucket = bucket
                )
                deposit(
                    toAddress = Address(targetAccount.address),
                    fromBucket = bucket
                )
            }
        }
    }

    /**
     * Sums all the amount needed to be withdrawn for each fungible
     */
    private fun TransferViewModel.State.withdrawingFungibles(): Map<Resource.FungibleResource, BigDecimal> {
        val allFungibles: List<SpendingAsset.Fungible> =
            targetAccounts.map { it.assets.filterIsInstance<SpendingAsset.Fungible>() }.flatten()

        val fungibleAmounts = mutableMapOf<Resource.FungibleResource, BigDecimal>()
        allFungibles.forEach { fungible ->
            val alreadySpentAmount = fungibleAmounts[fungible.resource] ?: BigDecimal.ZERO

            fungibleAmounts[fungible.resource] = alreadySpentAmount + fungible.amountDecimal
        }

        return fungibleAmounts
    }
}
