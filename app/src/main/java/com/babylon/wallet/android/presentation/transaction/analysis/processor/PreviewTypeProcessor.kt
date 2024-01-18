package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import javax.inject.Inject

@Suppress("LongParameterList")
class PreviewTypeAnalyzer @Inject constructor(
    private val generalTransferProcessor: GeneralTransferProcessor,
    private val transferProcessor: TransferProcessor,
    private val poolContributionProcessor: PoolContributionProcessor,
    private val accountDepositSettingsProcessor: AccountDepositSettingsProcessor,
    private val poolRedemptionProcessor: PoolRedemptionProcessor,
    private val validatorStakeProcessor: ValidatorStakeProcessor,
    private val validatorClaimProcessor: ValidatorClaimProcessor,
    private val validatorUnstakeProcessor: ValidatorUnstakeProcessor
) {

    suspend fun analyze(summary: ExecutionSummary): PreviewType {
        val manifestClass = summary.detailedClassification.firstOrNull() ?: return PreviewType.NonConforming

        return when (manifestClass) {
            is DetailedManifestClass.General -> generalTransferProcessor.process(summary, manifestClass)
            is DetailedManifestClass.Transfer -> transferProcessor.process(summary, manifestClass)
            is DetailedManifestClass.PoolContribution -> poolContributionProcessor.process(summary, manifestClass)
            is DetailedManifestClass.AccountDepositSettingsUpdate -> accountDepositSettingsProcessor.process(summary, manifestClass)
            is DetailedManifestClass.PoolRedemption -> poolRedemptionProcessor.process(summary, manifestClass)
            is DetailedManifestClass.ValidatorStake -> validatorStakeProcessor.process(summary, manifestClass)
            is DetailedManifestClass.ValidatorClaim -> validatorClaimProcessor.process(summary, manifestClass)
            is DetailedManifestClass.ValidatorUnstake -> validatorUnstakeProcessor.process(summary, manifestClass)
        }
    }
}

interface PreviewTypeProcessor<C : DetailedManifestClass> {

    suspend fun process(summary: ExecutionSummary, classification: C): PreviewType
}
