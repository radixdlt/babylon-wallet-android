package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.model.transaction.prepareInternalTransactionRequest
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.SecurityStructureOfFactorSourceIDs
import com.radixdlt.sargon.SecurityStructureOfFactorSources
import com.radixdlt.sargon.extensions.blobs
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.manifestString
import com.radixdlt.sargon.extensions.then
import com.radixdlt.sargon.extensions.toList
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class PrepareApplyShieldRequestUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend fun applyShield(
        securityStructureId: SecurityStructureId,
        entityAddress: AddressOfAccountOrPersona
    ): Result<TransactionRequest> = runCatching {
        sargonOsManager.sargonOs.securityStructuresOfFactorSources()
            .first { it.metadata.id == securityStructureId }
    }.then { securityStructure ->
        prepareTransactionRequest(
            securityStructure = securityStructure,
            entityAddress = entityAddress,
            transactionType = TransactionType.SecurifyEntity(
                entityAddress = entityAddress
            )
        )
    }

    suspend fun updateShield(
        securityStructureIds: SecurityStructureOfFactorSourceIDs,
        entityAddress: AddressOfAccountOrPersona
    ): Result<TransactionRequest> = sargonOsManager.callSafely(dispatcher) {
        securityStructureOfFactorSourcesFromSecurityStructureOfFactorSourceIds(securityStructureIds)
    }.then { securityStructure ->
        prepareTransactionRequest(
            securityStructure = securityStructure,
            entityAddress = entityAddress,
            transactionType = TransactionType.InitiateSecurityStructureRecovery
        )
    }

    private suspend fun prepareTransactionRequest(
        securityStructure: SecurityStructureOfFactorSources,
        entityAddress: AddressOfAccountOrPersona,
        transactionType: TransactionType
    ): Result<TransactionRequest> = sargonOsManager.callSafely(dispatcher) {
        makeUpdateSecurityShieldManifest(securityStructure, entityAddress)
    }.mapCatching { manifest ->
        UnvalidatedManifestData(
            instructions = manifest.manifestString,
            plainMessage = null,
            networkId = sargonOsManager.sargonOs.currentNetworkId(),
            blobs = manifest.blobs.toList().map { it.bytes },
        ).prepareInternalTransactionRequest(
            transactionType = transactionType,
            blockUntilCompleted = true
        )
    }
}
