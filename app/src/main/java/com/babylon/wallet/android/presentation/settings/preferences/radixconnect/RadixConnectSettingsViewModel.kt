package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.data.repository.radixconnect.IrohRelayServers
import com.babylon.wallet.android.data.repository.radixconnect.IrohRelaysRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.extensions.clientID
import com.radixdlt.sargon.extensions.hex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.core.preferences.RadixConnectPreferences
import rdx.works.peerdroid.data.iroh.IrohRelayConfig
import rdx.works.peerdroid.domain.RadixConnectDiagnostics
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
@HiltViewModel
class RadixConnectSettingsViewModel @Inject constructor(
    private val radixConnectPreferences: RadixConnectPreferences,
    irohRelaysRepository: IrohRelaysRepository,
    private val irohRelayConfig: IrohRelayConfig,
    private val radixConnectDiagnostics: RadixConnectDiagnostics,
    private val p2pLinksRepository: P2PLinksRepository,
    getProfileUseCase: GetProfileUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<RadixConnectSettingsViewModel.State>() {

    /**
     * The diagnostics of the connectors, refreshed while the screen shows them: debug builds and developer mode only
     * (null otherwise).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val connectorDiagnostics: StateFlow<ImmutableList<ConnectorDiagnostics>?> = getProfileUseCase.flow
        .map { profile -> BuildConfig.DEBUG_MODE || profile.appPreferences.security.isDeveloperModeEnabled }
        .distinctUntilChanged()
        .flatMapLatest { isVisible -> if (isVisible) refreshedConnectorDiagnostics() else flowOf(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.VM_STOP_TIMEOUT_MS),
            initialValue = null
        )

    override fun initialState(): State = State()

    init {
        combine(radixConnectPreferences.transport, irohRelaysRepository.relayServers) { transport, relayServers ->
            transport to relayServers
        }.onEach { (transport, relayServers) ->
            _state.update { state -> state.copy(transport = transport, relayServers = relayServers) }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            // The first call loads the native library: keep it off the main thread.
            val publicRelayUrls = withContext(defaultDispatcher) {
                runCatching { irohRelayConfig.publicRelayUrls() }
                    .onFailure { error -> Timber.w("iroh library unavailable: ${error.javaClass.simpleName}") }
                    .getOrNull()
            }
            _state.update { state -> state.copy(publicRelayUrls = publicRelayUrls?.toPersistentList()) }
        }
    }

    fun onTransportSelected(transport: RadixConnectTransport) {
        viewModelScope.launch { radixConnectPreferences.setTransport(transport) }
    }

    /**
     * The text copied by "Copy diagnostics": no secrets.
     */
    fun diagnosticsReport(): String = with(_state.value) {
        RadixConnectDiagnosticsReport.build(
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            transport = transport,
            relayServer = relayServers?.current,
            connectors = connectorDiagnostics.value.orEmpty()
        )
    }

    private fun refreshedConnectorDiagnostics(): Flow<ImmutableList<ConnectorDiagnostics>?> = flow {
        while (true) {
            val names = p2pLinksRepository.getP2PLinks().asList().associate { link -> link.clientID().hex to link.displayName }
            val diagnostics = radixConnectDiagnostics.linkDiagnostics().map { linkDiagnostics ->
                ConnectorDiagnostics(
                    name = names[linkDiagnostics.connectionId] ?: UNKNOWN_CONNECTOR_NAME,
                    diagnostics = linkDiagnostics
                )
            }
            emit(diagnostics.toPersistentList())
            delay(DIAGNOSTICS_REFRESH_INTERVAL)
        }
    }

    data class State(
        val transport: RadixConnectTransport = RadixConnectTransport.DEFAULT,
        val relayServers: IrohRelayServers? = null,
        // n0's public relays, for display. Null when the iroh library is not available on this device.
        val publicRelayUrls: ImmutableList<String>? = null
    ) : UiState

    companion object {
        private val DIAGNOSTICS_REFRESH_INTERVAL = 2.seconds
        private const val UNKNOWN_CONNECTOR_NAME = "?"
    }
}
