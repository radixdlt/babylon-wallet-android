package com.babylon.wallet.android.presentation.dappdir.all

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.dappdir.common.delegates.DAppListDelegate
import com.babylon.wallet.android.presentation.dappdir.common.delegates.DAppListViewActions
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListItem.DAppWithDetails
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.collections.map
import kotlin.collections.orEmpty

@HiltViewModel
class AllDAppsViewModel @Inject constructor(
    private val dAppListDelegate: DAppListDelegate
) : StateViewModel<DAppListState>(), DAppListViewActions by dAppListDelegate {

    private val dAppsWithDetails: Flow<List<DAppWithDetails>> = combine(
        dAppListDelegate.directoryState.map { directory ->
            directory?.copy(
                // Shuffle order of highlighted dApps
                highlighted = directory.highlighted?.shuffled(),
                // Shuffle order of other dApps
                others = directory.others?.shuffled()
            )
        }.onEach {
            it?.let { directory ->
                dAppListDelegate.onDAppsLoaded(it.allDefinitionAddresses())
            }
        },
        dAppListDelegate.dAppDataState
    ) { directory, dAppData ->
        directory?.highlighted.orEmpty().map {
            DAppWithDetails(
                dAppDefinitionAddress = it.dAppDefinitionAddress,
                hasDeposits = false,
                details = dAppData.getOrDefault(it.dAppDefinitionAddress, DAppWithDetails.Details.Fetching)
            )
        } + directory?.others.orEmpty().map {
            DAppWithDetails(
                dAppDefinitionAddress = it.dAppDefinitionAddress,
                hasDeposits = false,
                details = dAppData.getOrDefault(it.dAppDefinitionAddress, DAppWithDetails.Details.Fetching)
            )
        }
    }

    init {
        dAppListDelegate.initialize(
            scope = viewModelScope,
            state = _state,
            dAppsWithDetailsState = dAppsWithDetails,
            observeAccountLockerDeposits = false
        )
    }

    override fun initialState(): DAppListState = DAppListState(isLoading = true)

    fun onRefresh() {
        _state.update { it.copy(isRefreshing = true) }
        dAppListDelegate.loadDAppDirectory(
            onSuccess = { directory ->
                dAppListDelegate.onDAppsLoaded(directory.allDefinitionAddresses())
            }
        )
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }
}
