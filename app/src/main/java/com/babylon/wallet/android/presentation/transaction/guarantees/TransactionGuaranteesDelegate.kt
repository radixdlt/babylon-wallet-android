package com.babylon.wallet.android.presentation.transaction.guarantees

import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State.Sheet
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.GuaranteeItem
import com.babylon.wallet.android.presentation.transaction.model.InvolvedAccount
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import kotlinx.coroutines.flow.update
import rdx.works.core.mapWhen
import javax.inject.Inject

interface TransactionGuaranteesDelegate {

    fun onEditGuaranteesClick()

    fun onGuaranteeValueChange(account: GuaranteeItem, value: String)

    fun onGuaranteeValueIncreased(account: GuaranteeItem)

    fun onGuaranteeValueDecreased(account: GuaranteeItem)

    fun onGuaranteesApplyClick()
}

class TransactionGuaranteesDelegateImpl @Inject constructor() :
    ViewModelDelegate<TransactionReviewViewModel.State>(),
    TransactionGuaranteesDelegate {

    override fun onEditGuaranteesClick() {
        val transaction = (_state.value.previewType as? PreviewType.Transfer) ?: return

        val accountsWithPredictedGuarantee = mutableListOf<GuaranteeItem>()
        transaction.to.forEach { accountWithTransferableResources ->
            accountWithTransferableResources.transferables.forEach tr@{ transferable ->
                val fungibleTransferable = (transferable as? Transferable.FungibleType) ?: return@tr

                val involvedAccount = when (accountWithTransferableResources) {
                    is AccountWithTransferables.Other -> InvolvedAccount.Other(accountWithTransferableResources.address)
                    is AccountWithTransferables.Owned -> InvolvedAccount.Owned(accountWithTransferableResources.account)
                }

                GuaranteeItem.from(
                    involvedAccount = involvedAccount,
                    transferable = fungibleTransferable
                )
            }
        }

        _state.update {
            it.copy(
                sheetState = Sheet.CustomizeGuarantees(
                    guarantees = accountsWithPredictedGuarantee
                )
            )
        }
    }

    override fun onGuaranteeValueChange(account: GuaranteeItem, value: String) {
        val sheet = (_state.value.sheetState as? Sheet.CustomizeGuarantees) ?: return

        _state.update { state ->
            state.copy(
                sheetState = sheet.copy(
                    guarantees = sheet.guarantees.mapWhen(
                        predicate = { it.isTheSameGuaranteeItem(with = account) },
                        mutation = { it.change(value) }
                    )
                )
            )
        }
    }

    override fun onGuaranteeValueIncreased(account: GuaranteeItem) {
        val sheet = (_state.value.sheetState as? Sheet.CustomizeGuarantees) ?: return

        _state.update { state ->
            state.copy(
                sheetState = sheet.copy(
                    guarantees = sheet.guarantees.mapWhen(
                        predicate = { it.isTheSameGuaranteeItem(with = account) },
                        mutation = { it.increase() }
                    )
                )
            )
        }
    }

    override fun onGuaranteeValueDecreased(account: GuaranteeItem) {
        val sheet = (_state.value.sheetState as? Sheet.CustomizeGuarantees) ?: return

        _state.update { state ->
            state.copy(
                sheetState = sheet.copy(
                    guarantees = sheet.guarantees.mapWhen(
                        predicate = { it.isTheSameGuaranteeItem(with = account) },
                        mutation = { it.decrease() }
                    )
                )
            )
        }
    }

    override fun onGuaranteesApplyClick() {
        val sheet = (_state.value.sheetState as? Sheet.CustomizeGuarantees) ?: return
        val preview = (_state.value.previewType as? PreviewType.Transfer) ?: return

        val depositsWithUpdatedGuarantees = preview.to.mapWhen(
            predicate = { depositing ->
                sheet.guarantees.any { it.accountAddress == depositing.address }
            },
            mutation = { depositing ->
                depositing.updateFromGuarantees(sheet.guarantees)
            }
        )

        val updatedPreview = when (preview) {
            is PreviewType.Transfer.GeneralTransfer -> preview.copy(to = depositsWithUpdatedGuarantees)
            is PreviewType.Transfer.Pool -> preview.copy(to = depositsWithUpdatedGuarantees)
            is PreviewType.Transfer.Staking -> preview.copy(to = depositsWithUpdatedGuarantees)
        }

        _state.update {
            it.copy(
                previewType = updatedPreview,
                sheetState = Sheet.None
            )
        }
    }
}
