package com.babylon.wallet.android.presentation.transfer.prepare

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.toTransactionRequest
import com.babylon.wallet.android.data.transaction.MethodName
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.ManifestAstValue
import com.radixdlt.toolkit.models.NonFungibleLocalIdInternal
import com.radixdlt.toolkit.models.ValueKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.profile.data.model.pernetwork.Network
import timber.log.Timber
import java.math.BigDecimal

class PrepareManifestDelegate(
    private val state: MutableStateFlow<TransferViewModel.State>,
    private val incomingRequestRepository: IncomingRequestRepository
) {

    suspend fun onSubmit() {
        val fromAccount = state.value.fromAccount ?: return
        prepareRequest(fromAccount, state.value).onValue {
            Timber.tag("PrepareManifestDelegate").d(it.transactionManifestData.instructions)
            incomingRequestRepository.add(it)
        }.onError { error ->
            Timber.e(error)
            state.update { it.copy(error = UiMessage.ErrorMessage(error)) }
        }
    }

    private fun prepareRequest(
        fromAccount: Network.Account,
        currentState: TransferViewModel.State
    ): Result<MessageFromDataChannel.IncomingRequest.TransactionRequest> {
        val manifest = ManifestBuilder()
            .attachInstructionsForFungibles(
                fromAccount = fromAccount,
                targetAccounts = currentState.targetAccounts
            )
            .attachInstructionsForNFTs(
                fromAccount = fromAccount,
                targetAccounts = currentState.targetAccounts
            )
            .build()

        return manifest.toTransactionRequest(
            networkId = fromAccount.networkID,
            message = currentState.submittedMessage,
        )
    }

    private fun ManifestBuilder.attachInstructionsForFungibles(
        fromAccount: Network.Account,
        targetAccounts: List<TargetAccount>
    ): ManifestBuilder  = apply {
        state.value.withdrawingFungibles().forEach { (resource, amount) ->
            // Withdraw the total amount for each fungible
            addInstruction(
                Instruction.CallMethod(
                    componentAddress = ManifestAstValue.Address(address = fromAccount.address),
                    methodName = ManifestAstValue.String(MethodName.Withdraw.stringValue),
                    arguments = arrayOf(
                        ManifestAstValue.Address(resource.resourceAddress),
                        ManifestAstValue.Decimal(amount)
                    )
                )
            )

            // Deposit to each target account
            targetAccounts.filter { targetAccount ->
                targetAccount.assets.any { it.address == resource.resourceAddress }
            }.forEach { targetAccount ->
                val spendingFungibleAsset = targetAccount.assets.find { it.address == resource.resourceAddress } as? SpendingAsset.Fungible
                if (spendingFungibleAsset != null) {
                    val bucket = ManifestAstValue.Bucket(identifier = "${targetAccount.address}_${resource.resourceAddress}")
                    // First fill in a bucket from worktop with the correct amount
                    addInstruction(
                        Instruction.TakeFromWorktopByAmount(
                            resourceAddress = ManifestAstValue.Address(address = resource.resourceAddress),
                            amount = ManifestAstValue.Decimal(spendingFungibleAsset.amountDecimal),
                            intoBucket = bucket
                        )
                    )

                    // Then deposit that bucket
                    addInstruction(
                        Instruction.CallMethod(
                            componentAddress = ManifestAstValue.Address(address = targetAccount.address),
                            methodName = ManifestAstValue.String(MethodName.Deposit.stringValue),
                            arguments = arrayOf(bucket)
                        )
                    )
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
                val bucket = ManifestAstValue.Bucket(identifier = "${targetAccount.address}_${nft.address}")
                addInstruction(
                    Instruction.CallMethod(
                        componentAddress = ManifestAstValue.Address(address = fromAccount.address),
                        methodName = ManifestAstValue.String(MethodName.WithdrawNonFungibles.stringValue),
                        arguments = arrayOf(
                            ManifestAstValue.Address(nft.item.collectionAddress),
                            ManifestAstValue.Array(
                                elementKind = ValueKind.NonFungibleLocalId,
                                elements = arrayOf(ManifestAstValue.NonFungibleLocalId(NonFungibleLocalIdInternal.String(nft.item.localId)))
                            )
                        )
                    )
                )

                addInstruction(
                    Instruction.TakeFromWorktopByIds(
                        resourceAddress = ManifestAstValue.Address(nft.item.collectionAddress),
                        ids = setOf(ManifestAstValue.NonFungibleLocalId(NonFungibleLocalIdInternal.String(nft.item.localId))),
                        intoBucket = bucket
                    )
                )

                addInstruction(
                    Instruction.CallMethod(
                        componentAddress = ManifestAstValue.Address(address = targetAccount.address),
                        methodName = ManifestAstValue.String(MethodName.Deposit.stringValue),
                        arguments = arrayOf(bucket)
                    )
                )
            }
        }
    }

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
