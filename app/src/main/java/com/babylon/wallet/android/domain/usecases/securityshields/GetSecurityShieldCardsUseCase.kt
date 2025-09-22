package com.babylon.wallet.android.domain.usecases.securityshields

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.ui.model.securityshields.SecurityShieldCard
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.MatrixOfFactorSources
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetSecurityShieldCardsUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(): Result<List<SecurityShieldCard>> = sargonOsManager.callSafely(dispatcher) {
        securityStructuresOfFactorSources().map { shield ->
            SecurityShieldCard(
                id = shield.metadata.id,
                name = shield.metadata.displayName,
                factorSources = shield.matrixOfFactors.factorSources.toSet().toPersistentList()
            )
        }
    }

    private val MatrixOfFactorSources.factorSources
        get() = primaryRole.thresholdFactors +
            primaryRole.overrideFactors +
            recoveryRole.overrideFactors +
            confirmationRole.overrideFactors
}
