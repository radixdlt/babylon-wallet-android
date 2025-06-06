package com.babylon.wallet.android.presentation.transfer.prepare

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.model.transaction.prepareInternalTransactionRequest
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.radixdlt.sargon.AccountForDisplay
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.PerAssetFungibleResource
import com.radixdlt.sargon.PerAssetFungibleTransfer
import com.radixdlt.sargon.PerAssetNonFungibleTransfer
import com.radixdlt.sargon.PerAssetTransfers
import com.radixdlt.sargon.PerAssetTransfersOfFungibleResource
import com.radixdlt.sargon.PerAssetTransfersOfNonFungibleResource
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.TransferRecipient
import com.radixdlt.sargon.extensions.from
import com.radixdlt.sargon.extensions.perAssetTransfers
import kotlinx.coroutines.flow.update
import rdx.works.core.domain.resources.Resource
import rdx.works.profile.data.repository.MnemonicRepository
import timber.log.Timber
import javax.inject.Inject

class PrepareManifestDelegate @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository,
    private val mnemonicRepository: MnemonicRepository
) : ViewModelDelegate<TransferViewModel.State>() {

    suspend fun onSubmit() {
        val fromAccount = _state.value.fromAccount ?: return
        val accountsAbleToSign = _state.value.targetAccounts.filterAccountsAbleToSign()

        runCatching {
            TransactionManifest.perAssetTransfers(
                transfers = PerAssetTransfers(
                    fromAccount = fromAccount.address,
                    fungibleResources = _state.value.toFungibleTransfers(accountsAbleToSign),
                    nonFungibleResources = _state.value.toNonFungibleTransfers(accountsAbleToSign)
                )
            )
        }.map { manifest ->
            UnvalidatedManifestData.from(
                manifest = manifest,
                message = (_state.value.messageState as? TransferViewModel.State.Message.Added)?.message
            ).prepareInternalTransactionRequest()
        }.onSuccess { request ->
            _state.update { it.copy(transferRequestId = request.interactionId) }
            Timber.d("Manifest for ${request.interactionId} prepared:\n${request.unvalidatedManifestData.instructions}")
            incomingRequestRepository.add(request)
        }.onFailure { error ->
            _state.update { it.copy(error = UiMessage.ErrorMessage(error)) }
        }
    }

    private fun TransferViewModel.State.toFungibleTransfers(
        accountsAbleToSign: List<TargetAccount.Owned>
    ): List<PerAssetTransfersOfFungibleResource> {
        val perFungibleAssetTransfers = mutableMapOf<Resource.FungibleResource, MutableList<PerAssetFungibleTransfer>>()
        targetAccounts.forEach { targetAccount ->
            targetAccount.spendingAssets.filterIsInstance<SpendingAsset.Fungible>().forEach { asset ->
                val listOfExistingTransfers = perFungibleAssetTransfers.getOrPut(asset.resource) { mutableListOf() }
                listOfExistingTransfers.add(
                    PerAssetFungibleTransfer(
                        useTryDepositOrAbort = targetAccount.useTryDepositOrAbort(asset.resourceAddress, accountsAbleToSign),
                        amount = asset.amountDecimal,
                        recipient = targetAccount.toAssetTransfersRecipient()
                    )
                )
            }
        }

        return perFungibleAssetTransfers.map { entry ->
            PerAssetTransfersOfFungibleResource(
                resource = PerAssetFungibleResource(
                    resourceAddress = entry.key.address,
                    divisibility = entry.key.divisibility?.value
                ),
                transfers = entry.value
            )
        }
    }

    private fun TransferViewModel.State.toNonFungibleTransfers(
        accountsAbleToSign: List<TargetAccount.Owned>
    ): List<PerAssetTransfersOfNonFungibleResource> {
        val perNFTAssetTransfers = mutableMapOf<Resource.NonFungibleResource, MutableList<PerAssetNonFungibleTransfer>>()
        targetAccounts.forEach { targetAccount ->
            val spendingNFTs = mutableMapOf<Resource.NonFungibleResource, MutableList<Resource.NonFungibleResource.Item>>()
            targetAccount.spendingAssets.filterIsInstance<SpendingAsset.NFT>().forEach { asset ->
                val items = spendingNFTs.getOrPut(asset.resource) { mutableListOf() }
                items.add(asset.item)
            }

            spendingNFTs.forEach { entry ->
                val perNFTAssetTransfer = perNFTAssetTransfers.getOrPut(entry.key) { mutableListOf() }
                perNFTAssetTransfer.add(
                    PerAssetNonFungibleTransfer(
                        useTryDepositOrAbort = targetAccount.useTryDepositOrAbort(entry.key.address, accountsAbleToSign),
                        nonFungibleLocalIds = entry.value.map { it.localId },
                        recipient = targetAccount.toAssetTransfersRecipient()
                    )
                )
            }
        }

        return perNFTAssetTransfers.map { entry ->
            PerAssetTransfersOfNonFungibleResource(
                resource = entry.key.address,
                transfers = entry.value
            )
        }
    }

    private fun TargetAccount.toAssetTransfersRecipient(): TransferRecipient = when (this) {
        is TargetAccount.Other -> {
            when (val input = requireNotNull(resolvedInput)) {
                is TargetAccount.Other.ResolvedInput.AccountInput -> {
                    TransferRecipient.AddressOfExternalAccount(value = input.accountAddress)
                }
                is TargetAccount.Other.ResolvedInput.DomainInput -> {
                    TransferRecipient.RnsDomain(value = input.receiver)
                }
            }
        }
        is TargetAccount.Owned -> TransferRecipient.ProfileAccount(
            value = AccountForDisplay.from(account)
        )
        is TargetAccount.Skeleton -> error("Not a valid recipient")
    }

    private suspend fun List<TargetAccount>.filterAccountsAbleToSign(): List<TargetAccount.Owned> =
        filterIsInstance<TargetAccount.Owned>().filter {
            val factorSourceId = it.factorSourceId ?: return@filter false

            factorSourceId.value.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET || (
                factorSourceId.value.kind == FactorSourceKind.DEVICE && mnemonicRepository.mnemonicExist(factorSourceId)
                )
        }

    private fun TargetAccount.useTryDepositOrAbort(
        resourceAddress: ResourceAddress,
        accountsAbleToSign: List<TargetAccount.Owned>
    ): Boolean = this !in accountsAbleToSign || !isSignatureRequiredForTransfer(resourceAddress)
}
