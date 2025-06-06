package com.babylon.wallet.android.presentation.transaction.analysis.summary.execution

import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.analysis.summary.Summary
import com.babylon.wallet.android.presentation.transaction.analysis.summary.SummaryToPreviewTypeAnalyzer
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import javax.inject.Inject

@Suppress("LongParameterList")
class ExecutionSummaryToPreviewTypeAnalyser @Inject constructor(
    private val generalTransferProcessor: GeneralTransferProcessor,
    private val transferProcessor: TransferProcessor,
    private val poolContributionProcessor: PoolContributionProcessor,
    private val accountDepositSettingsProcessor: AccountDepositSettingsProcessor,
    private val poolRedemptionProcessor: PoolRedemptionProcessor,
    private val validatorStakeProcessor: ValidatorStakeProcessor,
    private val validatorClaimProcessor: ValidatorClaimProcessor,
    private val validatorUnstakeProcessor: ValidatorUnstakeProcessor,
    private val accountDeletionProcessor: AccountDeletionProcessor,
    private val securifyEntityProcessor: SecurifyEntityProcessor
) : SummaryToPreviewTypeAnalyzer<Summary.FromExecution> {

    override suspend fun analyze(summary: Summary.FromExecution): PreviewType {
        val executionSummary = summary.summary
        val manifestClass = executionSummary.detailedClassification ?: return PreviewType.NonConforming

        return when (manifestClass) {
            is DetailedManifestClass.General -> generalTransferProcessor.process(executionSummary, manifestClass)
            is DetailedManifestClass.Transfer -> transferProcessor.process(executionSummary, manifestClass)
            is DetailedManifestClass.PoolContribution -> poolContributionProcessor.process(executionSummary, manifestClass)
            is DetailedManifestClass.PoolRedemption -> poolRedemptionProcessor.process(executionSummary, manifestClass)
            is DetailedManifestClass.ValidatorStake -> validatorStakeProcessor.process(executionSummary, manifestClass)
            is DetailedManifestClass.ValidatorClaim -> validatorClaimProcessor.process(executionSummary, manifestClass)
            is DetailedManifestClass.ValidatorUnstake -> validatorUnstakeProcessor.process(executionSummary, manifestClass)
            is DetailedManifestClass.AccountDepositSettingsUpdate -> accountDepositSettingsProcessor.process(
                executionSummary,
                manifestClass
            )
            is DetailedManifestClass.DeleteAccounts -> accountDeletionProcessor.process(executionSummary, manifestClass)
            is DetailedManifestClass.SecurifyEntity -> securifyEntityProcessor.process(executionSummary, manifestClass)
        }
    }
}

interface PreviewTypeProcessor<C : DetailedManifestClass> {

    suspend fun process(summary: ExecutionSummary, classification: C): PreviewType
}
