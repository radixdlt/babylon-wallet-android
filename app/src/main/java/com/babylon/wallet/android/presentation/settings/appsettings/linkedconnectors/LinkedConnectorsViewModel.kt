package com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.p2pLinks
import rdx.works.profile.domain.p2plink.DeleteP2PLinkUseCase
import javax.inject.Inject

@HiltViewModel
class LinkedConnectorsViewModel @Inject constructor(
    private val peerdroidClient: PeerdroidClient,
    getProfileUseCase: GetProfileUseCase,
    private val deleteP2PLinkUseCase: DeleteP2PLinkUseCase,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<LinkedConnectorsUiState>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = LinkedConnectorsScreenArgs(savedStateHandle)

    override fun initialState(): LinkedConnectorsUiState = LinkedConnectorsUiState(
        showAddLinkConnectorScreen = args.shouldShowAddLinkConnectorScreen,
    )

    init {
        viewModelScope.launch {
            getProfileUseCase.p2pLinks
                .collect { p2pLinks ->
                    _state.update {
                        it.copy(activeConnectors = p2pLinks.toPersistentList())
                    }
                }
        }
    }

    fun onDeleteConnectorClick(connectionPassword: String) {
        viewModelScope.launch {
            deleteP2PLinkUseCase(connectionPassword)
            peerdroidClient.deleteLink(connectionPassword)
        }
    }

    fun onLinkNewConnectorClick() {
        _state.update {
            it.copy(
                showAddLinkConnectorScreen = true
            )
        }
    }

    fun onNewConnectorCloseClick() {
        _state.update {
            it.copy(showAddLinkConnectorScreen = false)
        }
    }
}

internal sealed interface Event : OneOffEvent {
    data object Close : Event
}

data class LinkedConnectorsUiState(
    val activeConnectors: ImmutableList<P2PLink> = persistentListOf(),
    val showAddLinkConnectorScreen: Boolean = false,
    val triggerCameraPermissionPrompt: Boolean = false
) : UiState
