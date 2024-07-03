package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.domain.model.GuaranteeAssertion
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferableResources
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.toDecimal192

object FeesResolver {
    fun resolve(
        summary: ExecutionSummary,
        notaryAndSigners: NotaryAndSigners,
        previewType: PreviewType
    ): TransactionFees {
        return TransactionFees(
            nonContingentFeeLock = summary.feeLocks.lock,
            networkExecution = summary.feeSummary.executionCost,
            networkFinalization = summary.feeSummary.finalizationCost,
            networkStorage = summary.feeSummary.storageExpansionCost,
            royalties = summary.feeSummary.royaltyCost,
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

private fun List<AccountWithTransferableResources>.guaranteesCount(): Int = map { accountWithTransferableResources ->
    accountWithTransferableResources.resources.filter { transferable ->
        transferable.guaranteeAssertion is GuaranteeAssertion.ForAmount
    }
}.flatten().count()
