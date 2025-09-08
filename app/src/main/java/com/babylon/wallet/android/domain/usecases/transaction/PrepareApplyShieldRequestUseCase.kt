package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.model.transaction.prepareInternalTransactionRequest
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.toList
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import javax.inject.Inject

class PrepareApplyShieldRequestUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        securityStructureId: SecurityStructureId,
        entityAddress: AddressOfAccountOrPersona
    ): Result<TransactionRequest> = sargonOsManager.callSafely(dispatcher) {
        makeInteractionForApplyingSecurityShield(securityStructureId, listOf(entityAddress))
    }.map { interaction ->
        Timber.d("Interaction: $interaction")

        val transaction = interaction.transactions.first()
        UnvalidatedManifestData(
            instructions = transaction.transactionManifestString,
            plainMessage = null,
            networkId = sargonOsManager.sargonOs.currentNetworkId(),
            blobs = transaction.blobs.toList().map { it.bytes },
        ).prepareInternalTransactionRequest(
            transactionType = TransactionType.SecurifyEntity(
                entityAddress = entityAddress
            )
        )
    }
}
