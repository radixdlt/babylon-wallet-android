package com.babylon.wallet.android.presentation.transaction.guarantees

import com.babylon.wallet.android.domain.model.GuaranteeType
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.presentation.transaction.AccountWithPredictedGuarantee
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.mapWhen

class TransactionGuaranteesDelegate(
    private val state: MutableStateFlow<State>
) {

    fun onEdit() {
        val transaction = (state.value.previewType as? PreviewType.Transfer) ?: return

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

        state.update {
            it.copy(
                sheetState = State.Sheet.CustomizeGuarantees(
                    accountsWithPredictedGuarantees = accountsWithPredictedGuarantee
                )
            )
        }
    }

    fun onValueChange(account: AccountWithPredictedGuarantee, value: String) {
        val sheet = (state.value.sheetState as? State.Sheet.CustomizeGuarantees) ?: return

        state.update { state ->
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
        val sheet = (state.value.sheetState as? State.Sheet.CustomizeGuarantees) ?: return

        state.update { state ->
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
        val sheet = (state.value.sheetState as? State.Sheet.CustomizeGuarantees) ?: return

        state.update { state ->
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
        state.update { it.copy(sheetState = State.Sheet.None) }
    }

    fun onApply() {
        val sheet = (state.value.sheetState as? State.Sheet.CustomizeGuarantees) ?: return
        val preview = (state.value.previewType as? PreviewType.Transfer) ?: return

        state.update {
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
                sheetState = State.Sheet.None
            )
        }
    }
}
