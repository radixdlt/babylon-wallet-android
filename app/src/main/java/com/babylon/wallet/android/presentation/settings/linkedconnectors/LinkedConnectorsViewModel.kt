package com.babylon.wallet.android.presentation.settings.linkedconnectors

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.composables.RenameInput
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.PublicKeyHash
import com.radixdlt.sargon.extensions.clientID
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.id
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.peerdroid.domain.LinkDiagnostics
import rdx.works.peerdroid.domain.PeerConnectionStatus
import rdx.works.peerdroid.domain.RadixConnectDiagnostics
import rdx.works.peerdroid.domain.TransportDiagnostics
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class LinkedConnectorsViewModel @Inject constructor(
    private val p2pLinksRepository: P2PLinksRepository,
    private val radixConnectDiagnostics: RadixConnectDiagnostics,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<LinkedConnectorsUiState>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = LinkedConnectorsScreenArgs(savedStateHandle)

    /**
     * How each linked connector is connected, by [PublicKeyHash] of its link; null until first known. Refreshed while
     * the screen shows it.
     */
    val connectorConnections: StateFlow<ImmutableMap<PublicKeyHash, ConnectorConnection>?> = flow {
        while (true) {
            emit(currentConnectorConnections())
            delay(CONNECTIONS_REFRESH_INTERVAL)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(Constants.VM_STOP_TIMEOUT_MS),
        initialValue = null
    )

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
                renameLinkConnectorItem = state.renameLinkConnectorItem?.copy(name = newName)
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
                _state.update { state -> state.copy(isLinkConnectorNameUpdated = true) }
            }
            setRenameConnectorSheetVisible(isVisible = false, connectorUiItem = null)
        }
    }

    fun onSnackbarMessageShown() {
        _state.update { state -> state.copy(isLinkConnectorNameUpdated = false) }
    }

    private suspend fun currentConnectorConnections(): ImmutableMap<PublicKeyHash, ConnectorConnection> {
        val diagnosticsByConnectionId = radixConnectDiagnostics.linkDiagnostics().associateBy { it.connectionId }
        return p2pLinksRepository.getP2PLinks().asList().associate { link ->
            link.id to diagnosticsByConnectionId[link.clientID().hex].connection()
        }.toImmutableMap()
    }

    companion object {
        private val CONNECTIONS_REFRESH_INTERVAL = 2.seconds
    }
}

/**
 * Which transport carries a linked connector.
 */
enum class ConnectorConnection {
    IROH,
    WEBRTC,
    NOT_CONNECTED
}

/**
 * iroh when it is connected, since messages then go over it, else WebRTC when connected.
 */
internal fun LinkDiagnostics?.connection(): ConnectorConnection {
    val connectedTransports = this?.transports.orEmpty().filter { it.status == PeerConnectionStatus.OPEN }
    return when {
        connectedTransports.any { it is TransportDiagnostics.Iroh } -> ConnectorConnection.IROH
        connectedTransports.any { it is TransportDiagnostics.WebRtc } -> ConnectorConnection.WEBRTC
        else -> ConnectorConnection.NOT_CONNECTED
    }
}

internal sealed interface Event : OneOffEvent {
    data object Close : Event
}

data class LinkedConnectorsUiState(
    val activeConnectors: ImmutableList<ConnectorUiItem> = persistentListOf(),
    val showAddLinkConnectorScreen: Boolean = false,
    val triggerCameraPermissionPrompt: Boolean = false,
    val isLinkConnectorNameUpdated: Boolean = false,
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
        override val name: String = "",
    ) : RenameInput()
}
