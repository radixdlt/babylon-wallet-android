package com.babylon.wallet.android.presentation.transaction.guarantees

import com.babylon.wallet.android.domain.model.GuaranteeType
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.AccountWithPredictedGuarantee
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State.Sheet
import kotlinx.coroutines.flow.update
import rdx.works.core.mapWhen
import javax.inject.Inject

class TransactionGuaranteesDelegate @Inject constructor() : ViewModelDelegate<TransactionReviewViewModel.State>() {

    fun onEdit() {
        val transaction = (_state.value.previewType as? PreviewType.Transfer) ?: return

        val accountsWithPredictedGuarantee = mutableListOf<AccountWithPredictedGuarantee>()
        transaction.to.forEach { depositing ->
            val resourcesWithGuarantees = depositing.resources.filterIsInstance<Transferable.Depositing>().filter {
                it.guaranteeType is GuaranteeType.Predicted && it.transferable is TransferableResource.Amount
            }

            val predictedAmounts = resourcesWithGuarantees.associate {
                it.transferable as TransferableResource.Amount to it.guaranteeType as GuaranteeType.Predicted
            }
            when (depositing) {
                is AccountWithTransferableResources.Other -> {
                    predictedAmounts.forEach { amount ->
                        accountsWithPredictedGuarantee.add(
                            AccountWithPredictedGuarantee.Other(
                                address = depositing.address,
                                transferableAmount = amount.key,
                                instructionIndex = amount.value.instructionIndex,
                                guaranteeAmountString = amount.value.guaranteePercent.toString()
                            )
                        )
                    }
                }

                is AccountWithTransferableResources.Owned -> {
                    predictedAmounts.forEach { amount ->
                        accountsWithPredictedGuarantee.add(
                            AccountWithPredictedGuarantee.Owned(
                                account = depositing.account,
                                transferableAmount = amount.key,
                                instructionIndex = amount.value.instructionIndex,
                                guaranteeAmountString = amount.value.guaranteePercent.toString()
                            )
                        )
                    }
                }
            }
        }

        _state.update {
            it.copy(
                sheetState = Sheet.CustomizeGuarantees(
                    accountsWithPredictedGuarantees = accountsWithPredictedGuarantee
                )
            )
        }
    }

    fun onValueChange(account: AccountWithPredictedGuarantee, value: String) {
        val sheet = (_state.value.sheetState as? Sheet.CustomizeGuarantees) ?: return

        _state.update { state ->
            state.copy(
                sheetState = sheet.copy(
                    accountsWithPredictedGuarantees = sheet.accountsWithPredictedGuarantees.mapWhen(
                        predicate = { it.isTheSameGuaranteeItem(with = account) },
                        mutation = { it.change(value) }
                    )
                )
            )
        }
    }

    fun onValueIncreased(account: AccountWithPredictedGuarantee) {
        val sheet = (_state.value.sheetState as? Sheet.CustomizeGuarantees) ?: return

        _state.update { state ->
            state.copy(
                sheetState = sheet.copy(
                    accountsWithPredictedGuarantees = sheet.accountsWithPredictedGuarantees.mapWhen(
                        predicate = { it.isTheSameGuaranteeItem(with = account) },
                        mutation = { it.increase() }
                    )
                )
            )
        }
    }

    fun onValueDecreased(account: AccountWithPredictedGuarantee) {
        val sheet = (_state.value.sheetState as? Sheet.CustomizeGuarantees) ?: return

        _state.update { state ->
            state.copy(
                sheetState = sheet.copy(
                    accountsWithPredictedGuarantees = sheet.accountsWithPredictedGuarantees.mapWhen(
                        predicate = { it.isTheSameGuaranteeItem(with = account) },
                        mutation = { it.decrease() }
                    )
                )
            )
        }
    }

    fun onClose() {
        _state.update { it.copy(sheetState = Sheet.None) }
    }

    fun onApply() {
        val sheet = (_state.value.sheetState as? Sheet.CustomizeGuarantees) ?: return
        val preview = (_state.value.previewType as? PreviewType.Transfer) ?: return

        _state.update {
            it.copy(
                previewType = preview.copy(
                    to = preview.to.mapWhen(
                        predicate = { depositing ->
                            sheet.accountsWithPredictedGuarantees.any { it.address == depositing.address }
                        },
                        mutation = { depositing ->
                            depositing.updateFromGuarantees(sheet.accountsWithPredictedGuarantees)
                        }
                    )
                ),
                sheetState = Sheet.None
            )
        }
    }
}
