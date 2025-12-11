package com.babylon.wallet.android.presentation.transaction.analysis.summary.execution

import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.AccessControllerAddress
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.os.SargonOsManager
import rdx.works.core.sargon.asProfileEntity

open class BaseAccessControllerRecoveryProcessor(
    private val sargonOsManager: SargonOsManager
) {

    open suspend fun process(
        summary: ExecutionSummary,
        acAddresses: List<AccessControllerAddress>,
        operation: PreviewType.UpdateSecurityStructure.Operation
    ): PreviewType.UpdateSecurityStructure {
        val address = acAddresses.first()
        val entity = sargonOsManager.sargonOs.entityByAccessControllerAddress(address).asProfileEntity()

        // To stop recovery the on-ledger structure is used instead of provisional one
        val structure = if (operation != PreviewType.UpdateSecurityStructure.Operation.StopRecovery) {
            runCatching {
                sargonOsManager.sargonOs
                    .provisionalSecurityStructureOfFactorSourcesFromAddressOfAccountOrPersona(entity.address)
            }.getOrNull()
        } else {
            null
        }

        return PreviewType.UpdateSecurityStructure(
            entity = entity,
            provisionalConfig = structure,
            operation = operation
        )
    }
}
