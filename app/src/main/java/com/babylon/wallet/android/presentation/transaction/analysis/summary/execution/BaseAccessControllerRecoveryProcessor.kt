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

        val structure = sargonOsManager.sargonOs
            .provisionalSecurityStructureOfFactorSourcesFromAddressOfAccountOrPersona(entity.address)

        return PreviewType.UpdateSecurityStructure(
            entity = entity,
            provisionalConfig = structure,
            operation = operation
        )
    }
}
