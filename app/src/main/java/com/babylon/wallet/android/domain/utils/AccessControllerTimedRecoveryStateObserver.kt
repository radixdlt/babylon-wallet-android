package com.babylon.wallet.android.domain.utils

import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.AccessControllerStateDetails
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import rdx.works.core.sargon.address
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
class AccessControllerTimedRecoveryStateObserver @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @ApplicationScope private val appScope: CoroutineScope,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    private var monitoringJob: Job? = null
    private val _recoveryStateByAddress =
        MutableSharedFlow<Map<AddressOfAccountOrPersona, AccessControllerStateDetails>>(1)
    val recoveryStateByAddress: Flow<Map<AddressOfAccountOrPersona, AccessControllerStateDetails>> =
        _recoveryStateByAddress.asSharedFlow()

    fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = ticker(5.minutes).map {
            sargonOsManager.callSafely(defaultDispatcher) {
                fetchAllAccessControllersDetails().map { details ->
                    entityByAccessControllerAddress(details.address).address to details
                }
            }.onFailure {
                Timber.w(it)
            }.getOrNull().orEmpty().toMap()
        }.onEach { detailsByEntityAddress ->
            _recoveryStateByAddress.emit(detailsByEntityAddress)
        }.catch { Timber.w(it) }
            .flowOn(defaultDispatcher)
            .launchIn(appScope)
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
    }

    fun cachedStateByAddress(address: AddressOfAccountOrPersona): AccessControllerStateDetails? {
        return _recoveryStateByAddress.replayCache
            .firstOrNull()
            ?.let { states -> states[address] }
    }
}
