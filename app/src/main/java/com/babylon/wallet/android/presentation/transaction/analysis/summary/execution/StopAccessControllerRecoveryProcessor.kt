package com.babylon.wallet.android.presentation.transaction.analysis.summary.execution

import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.os.SargonOsManager
import javax.inject.Inject

class StopAccessControllerRecoveryProcessor @Inject constructor(
    sargonOsManager: SargonOsManager
) : BaseAccessControllerRecoveryProcessor(
    sargonOsManager = sargonOsManager
),
    PreviewTypeProcessor<DetailedManifestClass.AccessControllerStopTimedRecovery> {

    override suspend fun process(
        summary: ExecutionSummary,
        classification: DetailedManifestClass.AccessControllerStopTimedRecovery
    ): PreviewType = process(
        summary = summary,
        acAddresses = classification.acAddresses,
        operation = PreviewType.UpdateSecurityStructure.Operation.StopRecovery
    )
}
