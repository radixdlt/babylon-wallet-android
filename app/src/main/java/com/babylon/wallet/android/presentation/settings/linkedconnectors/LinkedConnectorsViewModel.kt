package com.babylon.wallet.android.presentation.settings.linkedconnectors

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.PublicKeyHash
import com.radixdlt.sargon.extensions.id
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
                            activeConnectors = p2pLinks.asList().map { link ->
                                LinkedConnectorsUiState.ConnectorUiItem(
                                    id = link.id,
                                    name = link.displayName
                                )
                            }.toPersistentList(),
                            renameLinkConnectorItem = null
                        )
                    }
                }
        }
    }

    fun onDeleteConnectorClick(id: PublicKeyHash) {
        viewModelScope.launch {
            p2pLinksRepository.removeP2PLink(id)
        }
    }

    fun onLinkNewConnectorClick() {
        _state.update {
            it.copy(showAddLinkConnectorScreen = true)
        }
    }

    fun onNewConnectorCloseClick() {
        _state.update {
            it.copy(showAddLinkConnectorScreen = false)
        }
    }

    fun setRenameConnectorSheetVisible(
        isVisible: Boolean,
        connectorUiItem: LinkedConnectorsUiState.ConnectorUiItem? = null
    ) {
        _state.update {
            it.setRenameConnectorSheetVisible(isVisible, connectorUiItem)
        }
    }

    fun onNewConnectorNameChanged(newName: String) {
        _state.update { state ->
            state.copy(
                renameLinkConnectorItem = state.renameLinkConnectorItem?.copy(
                    name = newName,
                    isNameEmpty = newName.isEmpty()
                )
            )
        }
    }

    fun onUpdateConnectorNameClick() {
        viewModelScope.launch {
            val p2pLinkToRename = p2pLinksRepository.getP2PLinks().asList().find { p2pLink ->
                p2pLink.id == state.value.renameLinkConnectorItem?.id
            }
            p2pLinkToRename?.let {
                val newConnectorName = state.value.renameLinkConnectorItem?.name
                newConnectorName?.let {
                    p2pLinkToRename.displayName = newConnectorName
                }
                p2pLinksRepository.addOrUpdateP2PLink(p2pLinkToRename)
            }
            setRenameConnectorSheetVisible(isVisible = false, connectorUiItem = null)
        }
    }
}

internal sealed interface Event : OneOffEvent {
    data object Close : Event
}

data class LinkedConnectorsUiState(
    val activeConnectors: ImmutableList<ConnectorUiItem> = persistentListOf(),
    val showAddLinkConnectorScreen: Boolean = false,
    val triggerCameraPermissionPrompt: Boolean = false,
    val renameLinkConnectorItem: RenameConnectorInput? = null
) : UiState {

    fun setRenameConnectorSheetVisible(
        isVisible: Boolean,
        connectorUiItem: ConnectorUiItem? = null
    ) = copy(
        renameLinkConnectorItem = RenameConnectorInput(
            id = connectorUiItem?.id,
            name = connectorUiItem?.name.orEmpty()
        ).takeIf { isVisible }
    )

    data class ConnectorUiItem(
        val id: PublicKeyHash,
        val name: String
    )

    data class RenameConnectorInput(
        val id: PublicKeyHash? = null,
        val name: String = "",
        val isNameEmpty: Boolean = false
    )
}
