package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.settings.preferences.radixconnect.RadixConnectDiagnosticsReport.describe
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.utils.copyToClipboard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.peerdroid.domain.LinkDiagnostics
import rdx.works.peerdroid.domain.PeerConnectionStatus
import rdx.works.peerdroid.domain.TransportDiagnostics

/** How each connector is reaching this wallet right now, over one transport; refreshed while shown. */
@Composable
fun RadixConnectDiagnosticsScreen(
    viewModel: RadixConnectSettingsViewModel,
    transport: DiagnosedTransport,
    onBackClick: () -> Unit
) {
    val connectorDiagnostics by viewModel.connectorDiagnostics.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val copiedMessage = stringResource(id = R.string.addressAction_copiedToClipboard)

    RadixConnectDiagnosticsContent(
        transport = transport,
        connectors = connectorDiagnostics,
        onBackClick = onBackClick,
        onCopyDiagnosticsClick = {
            context.copyToClipboard(
                label = "Radix Connect diagnostics",
                value = viewModel.diagnosticsReport(),
                successMessage = copiedMessage
            )
        }
    )
}

@Composable
private fun RadixConnectDiagnosticsContent(
    transport: DiagnosedTransport,
    connectors: ImmutableList<ConnectorDiagnostics>?,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onCopyDiagnosticsClick: () -> Unit
) {
    // Only the connectors this transport is (or was) trying, and only its lines for each.
    val shown = connectors.orEmpty().filter { connector -> connector.diagnostics.transports.any(transport::shows) }
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixConnectTopBar(
                title = stringResource(id = R.string.preferences_radixConnect_diagnostics_title),
                onBackClick = onBackClick
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            item { SectionTitle(text = stringResource(id = transport.titleRes())) }
            if (shown.isEmpty()) {
                item {
                    Text(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.preferences_radixConnect_diagnostics_empty),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.textSecondary
                    )
                }
            }
            shown.forEach { connector ->
                item {
                    ConnectorDiagnosticsView(connector = connector, transport = transport)
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                }
            }
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.preferences_radixConnect_diagnostics_copy),
                    onClick = onCopyDiagnosticsClick
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            }
        }
    }
}

private fun DiagnosedTransport.titleRes(): Int = when (this) {
    DiagnosedTransport.IROH -> R.string.preferences_radixConnect_servers_iroh
    DiagnosedTransport.WEBRTC -> R.string.preferences_radixConnect_servers_webRtc
}

@Composable
private fun ConnectorDiagnosticsView(connector: ConnectorDiagnostics, transport: DiagnosedTransport) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = RadixTheme.colors.card)
            .padding(all = RadixTheme.dimensions.paddingDefault),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall)
    ) {
        Text(
            text = "${connector.name} · ${connector.diagnostics.status?.name?.lowercase() ?: "not running"}",
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.text
        )
        connector.diagnostics.transports.filter(transport::shows).forEach { line ->
            Text(
                text = line.describe(),
                style = RadixTheme.typography.body2Regular,
                color = if (line.lastError == null) RadixTheme.colors.textSecondary else RadixTheme.colors.error
            )
        }
    }
}

@Preview
@Composable
private fun RadixConnectDiagnosticsPreview() {
    RadixWalletPreviewTheme {
        RadixConnectDiagnosticsContent(
            transport = DiagnosedTransport.WEBRTC,
            connectors = persistentListOf(
                ConnectorDiagnostics(
                    name = "Chrome",
                    diagnostics = LinkDiagnostics(
                        connectionId = "1a2b3c4d5e6f",
                        status = PeerConnectionStatus.OPEN,
                        transports = listOf(
                            TransportDiagnostics.WebRtc(
                                status = PeerConnectionStatus.OPEN,
                                lastError = null,
                                signalingServer = "wss://signaling.example.com"
                            )
                        )
                    )
                )
            ),
            onBackClick = {},
            onCopyDiagnosticsClick = {}
        )
    }
}
