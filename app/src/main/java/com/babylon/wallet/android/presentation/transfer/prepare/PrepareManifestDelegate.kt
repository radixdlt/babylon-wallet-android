package com.babylon.wallet.android.presentation.transfer.prepare

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.toTransactionRequest
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.Instruction
import com.radixdlt.ret.ManifestValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.ret.ManifestBuilder
import rdx.works.core.ret.ManifestMethod
import rdx.works.profile.data.model.pernetwork.Network
import timber.log.Timber
import java.math.BigDecimal

class PrepareManifestDelegate(
    private val state: MutableStateFlow<TransferViewModel.State>,
    private val incomingRequestRepository: IncomingRequestRepository
) {

    suspend fun onSubmit() {
        val fromAccount = state.value.fromAccount ?: return
        val request = prepareRequest(fromAccount, state.value)
        state.update { it.copy(transferRequestId = request.requestId) }
        Timber.d("Manifest for ${request.requestId} prepared:")
        Timber.d(request.transactionManifestData.instructions)
        incomingRequestRepository.add(request)
    }

    private fun prepareRequest(
        fromAccount: Network.Account,
        currentState: TransferViewModel.State
    ): MessageFromDataChannel.IncomingRequest.TransactionRequest {
        val manifest = ManifestBuilder()
            .attachInstructionsForFungibles(
                fromAccount = fromAccount,
                targetAccounts = currentState.targetAccounts
            )
            .attachInstructionsForNFTs(
                fromAccount = fromAccount,
                targetAccounts = currentState.targetAccounts
            )
            .build(fromAccount.networkID)

        return manifest.toTransactionRequest(
            networkId = fromAccount.networkID,
            message = currentState.submittedMessage,
        )
    }

    @Suppress("NestedBlockDepth")
    private fun ManifestBuilder.attachInstructionsForFungibles(
        fromAccount: Network.Account,
        targetAccounts: List<TargetAccount>
    ): ManifestBuilder = apply {
        state.value.withdrawingFungibles().forEach { (resource, amount) ->
            // Withdraw the total amount for each fungible
            addInstruction(withdraw(fromAccount = fromAccount, fungible = resource, amount = amount))

            // Deposit to each target account
            targetAccounts.filter { targetAccount ->
                targetAccount.assets.any { it.address == resource.resourceAddress }
            }.forEach { targetAccount ->
                val spendingFungibleAsset = targetAccount.assets.find { it.address == resource.resourceAddress } as? SpendingAsset.Fungible
                if (spendingFungibleAsset != null) {
                    // First take the correct amount from worktop
                    addInstruction(takeFromWorktop(fungible = resource, amount = spendingFungibleAsset.amountDecimal))

                    // Then deposit into the target account
                    addInstruction(deposit(into = targetAccount))
                }
            }
        }
    }

    private fun ManifestBuilder.attachInstructionsForNFTs(
        fromAccount: Network.Account,
        targetAccounts: List<TargetAccount>
    ): ManifestBuilder = apply {
        targetAccounts.forEach { targetAccount ->
            val nonFungibleSpendingAssets = targetAccount.assets.filterIsInstance<SpendingAsset.NFT>()
            nonFungibleSpendingAssets.forEach { nft ->
                addInstruction(withdraw(fromAccount = fromAccount, nonFungible = nft.item))
                addInstruction(takeNonFungiblesFromWorktop(nonFungible = nft.item))
                addInstruction(deposit(into = targetAccount))
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

    private fun withdraw(
        fromAccount: Network.Account,
        fungible: Resource.FungibleResource,
        amount: BigDecimal
    ) = Instruction.CallMethod(
        address = Address(fromAccount.address),
        methodName = ManifestMethod.Withdraw.value,
        args = ManifestValue.TupleValue(
            fields = listOf(
                ManifestValue.AddressValue(value = Address(fungible.resourceAddress)),
                ManifestValue.DecimalValue(value = Decimal(amount.toPlainString()))
            )
        )
    )

    private fun takeFromWorktop(
        fungible: Resource.FungibleResource,
        amount: BigDecimal
    ) = Instruction.TakeFromWorktop(
        resourceAddress = Address(fungible.resourceAddress),
        amount = Decimal(amount.toPlainString())
    )

    private fun withdraw(
        fromAccount: Network.Account,
        nonFungible: Resource.NonFungibleResource.Item,
    ) = Instruction.CallMethod(
        address = Address(fromAccount.address),
        methodName = ManifestMethod.WithdrawNonFungibles.value,
        args = ManifestValue.TupleValue(
            fields = listOf(
                ManifestValue.AddressValue(Address(nonFungible.collectionAddress)),
                ManifestValue.NonFungibleLocalIdValue(nonFungible.localId.toRetId()),
            )
        )
    )

    private fun takeNonFungiblesFromWorktop(
        nonFungible: Resource.NonFungibleResource.Item,
    ) = Instruction.TakeNonFungiblesFromWorktop(
        resourceAddress = Address(nonFungible.collectionAddress),
        ids = listOf(nonFungible.localId.toRetId())
    )

    private fun deposit(
        into: TargetAccount
    ) = Instruction.CallMethod(
        address = Address(into.address),
        methodName = ManifestMethod.TryDepositOrAbort.value,
        args = ManifestValue.TupleValue(fields = listOf())
    )
}
