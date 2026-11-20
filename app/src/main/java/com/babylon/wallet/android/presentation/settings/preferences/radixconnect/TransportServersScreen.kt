package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import rdx.works.core.domain.RadixConnectTransport

/**
 * The servers of each Radix Connect transport: iroh's relay servers, then WebRTC's signaling servers — only the
 * transports the connection method chosen in Radix Connect uses.
 */
@Composable
fun TransportServersScreen(
    viewModel: RadixConnectSettingsViewModel,
    onBackClick: () -> Unit,
    onIrohClick: () -> Unit,
    onWebRtcClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TransportServersContent(
        transport = state.transport,
        onBackClick = onBackClick,
        onIrohClick = onIrohClick,
        onWebRtcClick = onWebRtcClick
    )
}

@Composable
private fun TransportServersContent(
    transport: RadixConnectTransport,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onIrohClick: () -> Unit,
    onWebRtcClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixConnectTopBar(
                title = stringResource(id = R.string.preferences_radixConnect_servers_title),
                onBackClick = onBackClick
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                ScreenLead(text = stringResource(id = R.string.preferences_radixConnect_servers_lead))
            }
            if (transport.usesIroh()) {
                item {
                    DefaultSettingsItem(
                        title = stringResource(id = R.string.preferences_radixConnect_servers_iroh),
                        subtitle = stringResource(id = R.string.preferences_radixConnect_servers_iroh_subtitle),
                        onClick = onIrohClick
                    )
                    HorizontalDivider(color = RadixTheme.colors.divider)
                }
            }
            if (transport.usesWebRtc()) {
                item {
                    DefaultSettingsItem(
                        title = stringResource(id = R.string.preferences_radixConnect_servers_webRtc),
                        subtitle = stringResource(id = R.string.preferences_radixConnect_servers_webRtc_subtitle),
                        onClick = onWebRtcClick
                    )
                    HorizontalDivider(color = RadixTheme.colors.divider)
                }
            }
        }
    }
}

private fun RadixConnectTransport.usesIroh(): Boolean = this != RadixConnectTransport.WEBRTC

private fun RadixConnectTransport.usesWebRtc(): Boolean = this != RadixConnectTransport.IROH

@Preview
@Composable
private fun TransportServersPreview() {
    RadixWalletPreviewTheme {
        TransportServersContent(
            transport = RadixConnectTransport.AUTOMATIC,
            onBackClick = {},
            onIrohClick = {},
            onWebRtcClick = {}
        )
    }
}
