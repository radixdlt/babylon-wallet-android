package com.babylon.wallet.android.presentation.transfer.prepare

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.radixdlt.ret.Address
import com.radixdlt.ret.ManifestBuilderBucket
import com.radixdlt.ret.NonFungibleGlobalId
import kotlinx.coroutines.flow.update
import rdx.works.core.ret.BabylonManifestBuilder
import rdx.works.core.ret.buildSafely
import rdx.works.core.toRETDecimal
import rdx.works.profile.data.model.extensions.hasAnyDenyDepositRule
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.MnemonicRepository
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class PrepareManifestDelegate @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository,
    private val mnemonicRepository: MnemonicRepository
) : ViewModelDelegate<TransferViewModel.State>() {

    suspend fun onSubmit() {
        val fromAccount = _state.value.fromAccount ?: return
        prepareRequest(fromAccount, _state.value).onSuccess { request ->
            _state.update { it.copy(transferRequestId = request.requestId) }
            Timber.d("Manifest for ${request.requestId} prepared:")
            Timber.d(request.transactionManifestData.instructions)
            incomingRequestRepository.add(request)
        }.onFailure { error ->
            _state.update { it.copy(error = UiMessage.ErrorMessage.from(error)) }
        }
    }

    private suspend fun prepareRequest(
        fromAccount: Network.Account,
        currentState: TransferViewModel.State
    ): Result<MessageFromDataChannel.IncomingRequest.TransactionRequest> =
        BabylonManifestBuilder()
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
    private suspend fun BabylonManifestBuilder.attachInstructionsForFungibles(
        fromAccount: Network.Account,
        targetAccounts: List<TargetAccount>
    ) = apply {
        _state.value.withdrawingFungibles().forEach { (resource, amount) ->
            // Withdraw the total amount for each fungible
            withdrawFromAccount(
                fromAddress = Address(fromAccount.address),
                fungible = Address(resource.resourceAddress),
                amount = amount.toRETDecimal(roundingMode = RoundingMode.HALF_UP)
            )

            // Deposit to each target account
            targetAccounts.filter { targetAccount ->
                targetAccount.assets.any { it.address == resource.resourceAddress }
            }.forEach { targetAccount ->
                val spendingFungibleAsset = targetAccount.assets.find {
                    it.address == resource.resourceAddress
                } as? SpendingAsset.Fungible
                if (spendingFungibleAsset != null) {
                    val bucket = newBucket()

                    // First take the correct amount from worktop and pour it into bucket
                    takeFromWorktop(
                        fungible = Address(resource.resourceAddress),
                        amount = spendingFungibleAsset.amountDecimal.toRETDecimal(roundingMode = RoundingMode.HALF_UP),
                        intoBucket = bucket
                    )

                    deposit(
                        targetAccount = targetAccount,
                        bucket = bucket,
                        spendingAsset = spendingFungibleAsset
                    )
                }
            }
        }
    }

    @Suppress("NestedBlockDepth")
    private suspend fun BabylonManifestBuilder.attachInstructionsForNFTs(
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
                withdrawNonFungiblesFromAccount(
                    fromAddress = Address(fromAccount.address),
                    nonFungible = globalId
                )
                takeNonFungiblesFromWorktop(
                    nonFungible = globalId,
                    intoBucket = bucket
                )

                deposit(
                    targetAccount = targetAccount,
                    bucket = bucket,
                    spendingAsset = nft
                )
            }
        }
    }

    private suspend fun BabylonManifestBuilder.deposit(
        targetAccount: TargetAccount,
        bucket: ManifestBuilderBucket,
        spendingAsset: SpendingAsset
    ) = apply {
        val isAccountAbleToSign = targetAccount.factorSourceId?.let {
            it.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET ||
                (it.kind == FactorSourceKind.DEVICE && mnemonicRepository.mnemonicExist(it))
        } ?: false

        if (targetAccount is TargetAccount.Owned && isAccountAbleToSign) {
            if (targetAccount.account.hasAnyDenyDepositRule(forSpecificAssetAddress = spendingAsset.address)) {
                // if for example account has deny all we don't want to prevent transfer between our OWN accounts
                // therefore ask user to sign
                accountDeposit(
                    toAddress = Address(targetAccount.address),
                    fromBucket = bucket
                )
            } else {
                accountTryDepositOrAbort(
                    toAddress = Address(targetAccount.address),
                    fromBucket = bucket
                )
            }
        } else { // try_deposit_or_abort for account that we are not controlling and are not able to sign tx
            accountTryDepositOrAbort(
                toAddress = Address(targetAccount.address),
                fromBucket = bucket
            )
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
