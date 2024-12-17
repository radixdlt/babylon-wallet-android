package com.babylon.wallet.android.presentation.transaction.guarantees

import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State.Sheet
import com.babylon.wallet.android.presentation.transaction.model.GuaranteeItem
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
        val transaction = (_state.value.previewType as? PreviewType.Transaction) ?: return

        val accountsWithPredictedGuarantee = mutableListOf<GuaranteeItem>()
        transaction.to.forEach { accountWithTransferables ->
            accountWithTransferables.transferables.forEach tr@{ transferable ->
                val fungibleTransferable = (transferable as? Transferable.FungibleType) ?: return@tr

                GuaranteeItem.from(
                    involvedAccount = accountWithTransferables.account,
                    transferable = fungibleTransferable
                )?.also {
                    accountsWithPredictedGuarantee.add(it)
                }
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
        val preview = (_state.value.previewType as? PreviewType.Transaction) ?: return

        val depositsWithUpdatedGuarantees = preview.to.mapWhen(
            predicate = { depositing ->
                sheet.guarantees.any { it.accountAddress == depositing.account.address }
            },
            mutation = { depositing ->
                depositing.updateFromGuarantees(sheet.guarantees)
            }
        )

        _state.update {
            it.copy(
                previewType = preview.copy(to = depositsWithUpdatedGuarantees),
                sheetState = Sheet.None
            )
        }
    }
}
