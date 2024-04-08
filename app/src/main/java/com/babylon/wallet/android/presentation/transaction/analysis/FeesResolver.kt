package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.transaction.guaranteesCount
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.toDecimal192

object FeesResolver {
    fun resolve(
        summary: ExecutionSummary,
        notaryAndSigners: NotaryAndSigners,
        previewType: PreviewType
    ): TransactionFees {
        return TransactionFees(
            nonContingentFeeLock = summary.feeLocks.lock.asStr().toDecimal192(),
            networkExecution = summary.feeSummary.executionCost.asStr().toDecimal192(),
            networkFinalization = summary.feeSummary.finalizationCost.asStr().toDecimal192(),
            networkStorage = summary.feeSummary.storageExpansionCost.asStr().toDecimal192(),
            royalties = summary.feeSummary.royaltyCost.asStr().toDecimal192(),
            guaranteesCount = (previewType as? PreviewType.Transfer)?.to?.guaranteesCount() ?: 0,
            notaryIsSignatory = notaryAndSigners.notaryIsSignatory,
            includeLockFee = false, // First its false because we don't know if lock fee is applicable or not yet
            signersCount = notaryAndSigners.signers.count()
        ).let { fees ->
            if (fees.defaultTransactionFee > 0.toDecimal192()) {
                // There will be a lock fee so update lock fee cost
                fees.copy(includeLockFee = true)
            } else {
                fees
            }
        }
    }
}
