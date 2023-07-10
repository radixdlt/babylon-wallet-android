package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.model.AccountWithTransferableResources
import com.babylon.wallet.android.domain.model.DepositingTransferableResource
import com.babylon.wallet.android.domain.model.WithdrawingTransferableResource
import com.babylon.wallet.android.presentation.transaction.analysis.resolveTo
import com.radixdlt.ret.ExecutionAnalysis
import com.radixdlt.ret.TransactionType
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

/**
 * This usecase gathers all accounts with its resources involved in transaction withdraws and deposits
 */
class GetTransactionResourcesFromAnalysis @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val entityRepository: EntityRepository
) {
    suspend operator fun invoke(
        executionAnalysis: ExecutionAnalysis,
    ): Result<ExecutionAnalysisWithAccounts> {
        val from = mutableMapOf<String, List<WithdrawingTransferableResource>>()
        val to = mutableMapOf<String, List<DepositingTransferableResource>>()

        when (val type = executionAnalysis.transactionType) {
            is TransactionType.NonConforming -> return Result.failure(IllegalStateException("Cannot process a non-conforming transaction"))
            is TransactionType.GeneralTransaction -> type.resolveTo(from = from, to = to)
            is TransactionType.SimpleTransfer -> type.resolveTo(from = from, to = to)
            is TransactionType.Transfer -> type.resolveTo(from = from, to = to)
        }

        return Result.success(
            ExecutionAnalysisWithAccounts(
                from = from.map {
                    AccountWithTransferableResources.Other(address = it.key, resources = it.value)
                },
                to = to.map {
                    AccountWithTransferableResources.Other(address = it.key, resources = it.value)
                }
            )
        )
    }
}

data class ExecutionAnalysisWithAccounts(
    val from: List<AccountWithTransferableResources>,
    val to: List<AccountWithTransferableResources>,
)
