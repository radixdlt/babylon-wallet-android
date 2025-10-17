package com.babylon.wallet.android.domain.utils

import com.babylon.wallet.android.data.repository.accesscontroller.AccessControllersRepository
import com.babylon.wallet.android.data.repository.accesscontroller.model.AccessControllerRecoveryState
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.radixdlt.sargon.AccountOrPersona
import com.radixdlt.sargon.AddressOfAccountOrPersona
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.core.sargon.address
import rdx.works.core.sargon.securityStateAccessControllerAddress
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
class AccessControllerTimedRecoveryStateObserver @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val accessControllersRepository: AccessControllersRepository,
    @ApplicationScope private val appScope: CoroutineScope,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    private var monitoringJob: Job? = null
    private val _recoveryStateByAddress =
        MutableSharedFlow<Map<AddressOfAccountOrPersona, AccessControllerRecoveryState>>(1)
    val recoveryStateByAddress: Flow<Map<AddressOfAccountOrPersona, AccessControllerRecoveryState>> =
        _recoveryStateByAddress.asSharedFlow()

    fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = combine(
            getProfileUseCase.flow.map { it.activeAccountsOnCurrentNetwork }.distinctUntilChanged(),
            getProfileUseCase.flow.map { it.activePersonasOnCurrentNetwork }.distinctUntilChanged(),
            ticker(5.minutes)
        ) { accounts, personas, _ ->
            val entities = accounts.map {
                AccountOrPersona.AccountEntity(it)
            } + personas.map {
                AccountOrPersona.PersonaEntity(it)
            }
            updateAccessControllerRecoveryStates(entities)
        }
            .catch { Timber.w(it) }
            .flowOn(defaultDispatcher)
            .launchIn(appScope)
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
    }

    private suspend fun updateAccessControllerRecoveryStates(entities: List<AccountOrPersona>) {
        val accessControllerAddresses = entities.mapNotNull { entity ->
            entity.securityStateAccessControllerAddress ?: return@mapNotNull null
        }.toSet()

        accessControllersRepository.getAccessControllerRecoveryStates(accessControllerAddresses, false)
            .onSuccess { states ->
                _recoveryStateByAddress.emit(
                    entities.mapNotNull { entity ->
                        val accessControllerAddress = entity.securityStateAccessControllerAddress
                            ?: return@mapNotNull null
                        states.firstOrNull { it.address == accessControllerAddress }
                            ?.let { state -> entity.address to state }
                    }.toMap()
                )
            }.onFailure {
                Timber.w(it)
            }
    }
}
