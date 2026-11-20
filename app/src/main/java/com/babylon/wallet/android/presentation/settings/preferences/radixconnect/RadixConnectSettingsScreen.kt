package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import rdx.works.core.domain.RadixConnectTransport

@Composable
fun RadixConnectSettingsScreen(
    viewModel: RadixConnectSettingsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RadixConnectSettingsContent(
        state = state,
        onBackClick = onBackClick,
        onTransportSelected = viewModel::onTransportSelected
    )
}

/** How the wallet reaches its connectors: the three transports, one chosen. Their servers are under Signaling Servers. */
@Composable
private fun RadixConnectSettingsContent(
    state: RadixConnectSettingsViewModel.State,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onTransportSelected: (RadixConnectTransport) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixConnectTopBar(
                title = stringResource(id = R.string.preferences_radixConnect_title),
                onBackClick = onBackClick
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            item { SectionTitle(text = stringResource(id = R.string.preferences_radixConnect_connectionMethod)) }
            RadixConnectTransport.entries.forEach { transport ->
                item {
                    TransportItem(
                        transport = transport,
                        isSelected = transport == state.transport,
                        onClick = { onTransportSelected(transport) }
                    )
                    HorizontalDivider(color = RadixTheme.colors.divider)
                }
            }
        }
    }
}

@Composable
private fun TransportItem(
    transport: RadixConnectTransport,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DefaultSettingsItem(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(id = transport.titleRes()),
        subtitle = stringResource(id = transport.subtitleRes()),
        onClick = onClick,
        trailingIcon = {
            Box(modifier = Modifier.size(24.dp)) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(id = DSR.ic_check),
                        contentDescription = null,
                        tint = RadixTheme.colors.icon
                    )
                }
            }
        }
    )
}

@Preview
@Composable
private fun RadixConnectSettingsPreview() {
    RadixWalletPreviewTheme {
        RadixConnectSettingsContent(
            state = RadixConnectSettingsViewModel.State(transport = RadixConnectTransport.AUTOMATIC),
            onBackClick = {},
            onTransportSelected = {}
        )
    }
}
