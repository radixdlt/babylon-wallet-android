package com.babylon.wallet.android.domain.usecases.factorsources

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.EntitiesLinkedToFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.ProfileToCheck
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import javax.inject.Inject

class GetEntitiesLinkedToFactorSourceUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        factorSource: FactorSource
    ): EntitiesLinkedToFactorSource? = sargonOsManager.callSafely(dispatcher = defaultDispatcher) {
        entitiesLinkedToFactorSource(
            factorSource = factorSource,
            profileToCheck = ProfileToCheck.Current
        )
    }.onFailure { error ->
        Timber.e("Failed to find linked entities: $error")
    }.getOrNull()
}

val EntitiesLinkedToFactorSource?.hasHiddenEntities
    get() = this?.let { !it.hiddenAccounts.isNotEmpty() || hiddenPersonas.isNotEmpty() } ?: false
