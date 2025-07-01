package com.babylon.wallet.android.presentation.dappdir.connected

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.dappdir.common.delegates.DAppListDelegate
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListItem.DAppWithDetails
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListState
import com.radixdlt.sargon.AuthorizedDapp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import rdx.works.profile.data.repository.DAppConnectionRepository
import javax.inject.Inject

@HiltViewModel
class ConnectedDAppsViewModel @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val dAppListDelegate: DAppListDelegate
) : StateViewModel<DAppListState>(), DAppListDelegate.ViewActions by dAppListDelegate {

    private val connectedDAppsState: MutableStateFlow<List<AuthorizedDapp>> = MutableStateFlow(emptyList())
    private val dAppsWithDetails: Flow<List<DAppWithDetails>> = combine(
        connectedDAppsState,
        dAppListDelegate.dAppDataState
    ) { connectedDApps, dAppData ->
        connectedDApps.map { dApp ->
            DAppWithDetails(
                dAppDefinitionAddress = dApp.dappDefinitionAddress,
                hasDeposits = false,
                details = dAppListDelegate.dAppDataState.value.getOrDefault(dApp.dappDefinitionAddress, DAppWithDetails.Details.Fetching)
            )
        }
    }
    private var loadDAppsJob: Job? = null

    init {
        dAppListDelegate.initialize(
            scope = viewModelScope,
            state = _state,
            dAppsWithDetailsState = dAppsWithDetails,
            observeAccountLockerDeposits = true
        )
        loadConnectedDApps()
    }

    override fun initialState(): DAppListState = DAppListState(isLoading = true)

    fun onRefresh() {
        _state.update { it.copy(isRefreshing = true) }
        loadConnectedDApps()
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    private fun loadConnectedDApps() {
        loadDAppsJob?.cancel()
        loadDAppsJob = dAppConnectionRepository.getAuthorizedDApps()
            .onEach { connectedDApps ->
                connectedDAppsState.update { connectedDApps }
                dAppListDelegate.onDAppsLoaded(connectedDApps.map { it.dappDefinitionAddress })
                _state.update { state -> state.copy(canRefresh = connectedDApps.isNotEmpty()) }
            }
            .catch { error ->
                dAppListDelegate.onDAppsLoadingError(error)
            }
            .launchIn(viewModelScope)
    }
}
