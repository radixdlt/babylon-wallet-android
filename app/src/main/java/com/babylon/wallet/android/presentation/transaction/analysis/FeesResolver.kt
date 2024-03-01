package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.transaction.guaranteesCount
import com.radixdlt.ret.ExecutionSummary
import java.math.BigDecimal

object FeesResolver {
    fun resolve(
        summary: ExecutionSummary,
        notaryAndSigners: NotaryAndSigners,
        previewType: PreviewType
    ): TransactionFees {
        return TransactionFees(
            nonContingentFeeLock = summary.feeLocks.lock.asStr().toBigDecimal(),
            networkExecution = summary.feeSummary.executionCost.asStr().toBigDecimal(),
            networkFinalization = summary.feeSummary.finalizationCost.asStr().toBigDecimal(),
            networkStorage = summary.feeSummary.storageExpansionCost.asStr().toBigDecimal(),
            royalties = summary.feeSummary.royaltyCost.asStr().toBigDecimal(),
            guaranteesCount = (previewType as? PreviewType.Transfer)?.to?.guaranteesCount() ?: 0,
            notaryIsSignatory = notaryAndSigners.notaryIsSignatory,
            includeLockFee = false, // First its false because we don't know if lock fee is applicable or not yet
            signersCount = notaryAndSigners.signers.count()
        ).let { fees ->
            if (fees.defaultTransactionFee > BigDecimal.ZERO) {
                // There will be a lock fee so update lock fee cost
                fees.copy(includeLockFee = true)
            } else {
                fees
            }
        }
    }

}