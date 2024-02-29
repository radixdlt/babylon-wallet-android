package com.babylon.wallet.android.presentation.transfer.prepare

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import kotlinx.coroutines.flow.update
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.ret.ManifestPoet
import rdx.works.profile.ret.transaction.TransactionManifestData
import timber.log.Timber
import javax.inject.Inject

class PrepareManifestDelegate @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository,
    private val mnemonicRepository: MnemonicRepository
) : ViewModelDelegate<TransferViewModel.State>() {

    suspend fun onSubmit() {
        val fromAccount = _state.value.fromAccount ?: return
        val accountsAbleToSign = _state.value.targetAccounts.filterAccountsAbleToSign()

        ManifestPoet
            .buildTransfer(
                fromAccount = fromAccount,
                depositFungibles = _state.value.toFungibleTransfers(accountsAbleToSign),
                depositNFTs = _state.value.toNonFungibleTransfers(accountsAbleToSign),
            )
            .map { manifest ->
                val message = when (val messageState = _state.value.messageState) {
                    is TransferViewModel.State.Message.Added -> TransactionManifestData.TransactionMessage.Public(
                        message = messageState.message
                    )
                    is TransferViewModel.State.Message.None -> TransactionManifestData.TransactionMessage.None
                }
                manifest.copy(message = message).prepareInternalTransactionRequest()
            }
            .onSuccess { request ->
                _state.update { it.copy(transferRequestId = request.requestId) }
                Timber.d("Manifest for ${request.requestId} prepared:\n${request.transactionManifestData.instructions}")
                incomingRequestRepository.add(request)
            }.onFailure { error ->
                _state.update { it.copy(error = UiMessage.ErrorMessage(error)) }
            }
    }

    private fun TransferViewModel.State.toFungibleTransfers(
        accountsAbleToSign: List<TargetAccount.Owned>
    ): List<ManifestPoet.FungibleTransfer> = targetAccounts.map { targetAccount ->
        targetAccount.spendingAssets.filterIsInstance<SpendingAsset.Fungible>().map { spendingFungible ->
            ManifestPoet.FungibleTransfer(
                toAccountAddress = targetAccount.address,
                resourceAddress = spendingFungible.address,
                amount = spendingFungible.amountDecimal,
                signatureRequired = targetAccount in accountsAbleToSign &&
                        targetAccount.isSignatureRequiredForTransfer(spendingFungible)
            )
        }
    }.flatten()

    private fun TransferViewModel.State.toNonFungibleTransfers(
        accountsAbleToSign: List<TargetAccount.Owned>
    ): List<ManifestPoet.NonFungibleTransfer> = targetAccounts.map { targetAccount ->
        targetAccount.spendingAssets.filterIsInstance<SpendingAsset.NFT>().map { spendingNonFungible ->
            ManifestPoet.NonFungibleTransfer(
                toAccountAddress = targetAccount.address,
                globalId = spendingNonFungible.address,
                signatureRequired = targetAccount in accountsAbleToSign &&
                        targetAccount.isSignatureRequiredForTransfer(spendingNonFungible)
            )
        }
    }.flatten()

    private suspend fun List<TargetAccount>.filterAccountsAbleToSign(): List<TargetAccount.Owned> =
        filterIsInstance<TargetAccount.Owned>().filter {
            val factorSourceId = it.factorSourceId ?: return@filter false

            factorSourceId.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET || (
                    factorSourceId.kind == FactorSourceKind.DEVICE && mnemonicRepository.mnemonicExist(factorSourceId)
                    )
        }
}
