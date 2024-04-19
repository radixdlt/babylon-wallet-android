package com.babylon.wallet.android.presentation.settings.linkedconnectors

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
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
import javax.inject.Inject

@HiltViewModel
class LinkedConnectorsViewModel @Inject constructor(
    private val p2pLinksRepository: P2PLinksRepository,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<LinkedConnectorsUiState>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = LinkedConnectorsScreenArgs(savedStateHandle)

    override fun initialState(): LinkedConnectorsUiState = LinkedConnectorsUiState(
        showAddLinkConnectorScreen = args.shouldShowAddLinkConnectorScreen,
    )

    init {
        viewModelScope.launch {
            p2pLinksRepository.observeP2PLinks()
                .collect { p2pLinks ->
                    _state.update {
                        it.copy(
                            activeConnectors = p2pLinks.map { link ->
                                LinkedConnectorsUiState.ConnectorUiItem(
                                    id = link.publicKey,
                                    name = link.displayName
                                )
                            }.toPersistentList()
                        )
                    }
                }
        }
    }

    fun onDeleteConnectorClick(id: String) {
        viewModelScope.launch {
            p2pLinksRepository.removeP2PLink(id)
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
    val activeConnectors: ImmutableList<ConnectorUiItem> = persistentListOf(),
    val showAddLinkConnectorScreen: Boolean = false,
    val triggerCameraPermissionPrompt: Boolean = false
) : UiState {

    data class ConnectorUiItem(
        val id: String,
        val name: String
    )
}
