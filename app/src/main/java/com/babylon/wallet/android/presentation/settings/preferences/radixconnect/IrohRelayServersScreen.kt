package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.repository.radixconnect.IrohRelayServer
import com.babylon.wallet.android.data.repository.radixconnect.IrohRelayServers
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.domain.RadixConnectTransport

/**
 * The iroh relay servers, as the WebRTC signaling servers are shown: the current one, the others, the diagnostics
 * of the connectors over iroh, and adding one.
 */
@Composable
fun IrohRelayServersScreen(
    viewModel: RadixConnectSettingsViewModel,
    onBackClick: () -> Unit,
    onRelayServerClick: (IrohRelayServer) -> Unit,
    onAddRelayServerClick: () -> Unit,
    onDiagnosticsClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val connectorDiagnostics by viewModel.connectorDiagnostics.collectAsStateWithLifecycle()

    IrohRelayServersContent(
        state = state,
        isDiagnosticsAvailable = connectorDiagnostics != null,
        onBackClick = onBackClick,
        onRelayServerClick = onRelayServerClick,
        onAddRelayServerClick = onAddRelayServerClick,
        onDiagnosticsClick = onDiagnosticsClick
    )
}

@Composable
private fun IrohRelayServersContent(
    state: RadixConnectSettingsViewModel.State,
    isDiagnosticsAvailable: Boolean,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onRelayServerClick: (IrohRelayServer) -> Unit,
    onAddRelayServerClick: () -> Unit,
    onDiagnosticsClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixConnectTopBar(
                title = stringResource(id = R.string.preferences_radixConnect_servers_iroh),
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
                ScreenLead(text = stringResource(id = R.string.preferences_radixConnect_info))
            }
            state.relayServers?.let { relayServers ->
                relayServerItems(
                    relayServers = relayServers,
                    publicRelayUrls = state.publicRelayUrls,
                    onRelayServerClick = onRelayServerClick
                )
            }
            if (isDiagnosticsAvailable) {
                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    DiagnosticsRow(onClick = onDiagnosticsClick)
                }
            }
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.preferences_radixConnect_relays_add),
                    onClick = onAddRelayServerClick
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            }
        }
    }
}

/**
 * The diagnostics of a transport's connectors, under their own label as «Current» and «Others» are: the same on
 * the iroh and the WebRTC screens.
 */
@Composable
fun DiagnosticsRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        ListLabel(text = stringResource(id = R.string.preferences_radixConnect_diagnostics_title))
        DefaultSettingsItem(
            title = stringResource(id = R.string.preferences_radixConnect_diagnostics_connectors),
            subtitle = stringResource(id = R.string.preferences_radixConnect_diagnostics_subtitle),
            onClick = onClick
        )
    }
}

private fun LazyListScope.relayServerItems(
    relayServers: IrohRelayServers,
    publicRelayUrls: ImmutableList<String>?,
    onRelayServerClick: (IrohRelayServer) -> Unit
) {
    item { ListLabel(text = stringResource(id = R.string.preferences_radixConnect_relays_current)) }
    item {
        RelayServerView(
            relayServer = relayServers.current,
            publicRelayUrls = publicRelayUrls,
            onClick = { onRelayServerClick(relayServers.current) }
        )
    }
    if (relayServers.others.isNotEmpty()) {
        item { ListLabel(text = stringResource(id = R.string.preferences_radixConnect_relays_others)) }
    }
    relayServers.others.forEachIndexed { index, relayServer ->
        item {
            RelayServerView(
                relayServer = relayServer,
                publicRelayUrls = publicRelayUrls,
                onClick = { onRelayServerClick(relayServer) }
            )
            if (index < relayServers.others.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier
                        .background(RadixTheme.colors.card)
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    color = RadixTheme.colors.divider
                )
            }
        }
    }
}

@Composable
private fun RelayServerView(
    relayServer: IrohRelayServer,
    publicRelayUrls: ImmutableList<String>?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = RadixTheme.colors.card)
            .throttleClickable(onClick = onClick)
            .padding(
                start = RadixTheme.dimensions.paddingDefault,
                end = RadixTheme.dimensions.paddingSmall,
                top = RadixTheme.dimensions.paddingDefault,
                bottom = RadixTheme.dimensions.paddingDefault
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val (title, subtitle) = when (relayServer) {
                IrohRelayServer.Public -> stringResource(id = R.string.preferences_radixConnect_relays_public) to
                    (publicRelayUrls?.relayHosts() ?: stringResource(id = R.string.preferences_radixConnect_relay_unavailable))

                is IrohRelayServer.Custom -> relayServer.url to null
            }
            Text(
                text = title,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            painter = painterResource(id = DSR.ic_chevron_right),
            contentDescription = null,
            tint = RadixTheme.colors.iconTertiary
        )
    }
}

@Preview
@Composable
private fun IrohRelayServersPreview() {
    RadixWalletPreviewTheme {
        IrohRelayServersContent(
            state = RadixConnectSettingsViewModel.State(
                transport = RadixConnectTransport.AUTOMATIC,
                relayServers = IrohRelayServers(
                    current = IrohRelayServer.Custom("https://relay.example.com"),
                    others = listOf(IrohRelayServer.Public)
                ),
                publicRelayUrls = persistentListOf("https://euc1-1.relay.n0.iroh.link./", "https://use1-1.relay.n0.iroh.link./")
            ),
            isDiagnosticsAvailable = true,
            onBackClick = {},
            onRelayServerClick = {},
            onAddRelayServerClick = {},
            onDiagnosticsClick = {}
        )
    }
}
