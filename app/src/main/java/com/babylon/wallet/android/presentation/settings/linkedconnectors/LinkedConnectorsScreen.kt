package com.babylon.wallet.android.presentation.settings.linkedconnectors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.composables.AddLinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.apppreferences.P2PLink

@Composable
fun LinkedConnectorsScreen(
    viewModel: LinkedConnectorsViewModel,
    addLinkConnectorViewModel: AddLinkConnectorViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addLinkConnectorState by addLinkConnectorViewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                Event.Close -> onBackClick()
            }
        }
    }

    if (state.showAddLinkConnectorScreen) {
        AddLinkConnectorScreen(
            modifier = modifier,
            showContent = addLinkConnectorState.showContent,
            isLoading = addLinkConnectorState.isLoading,
            onQrCodeScanned = addLinkConnectorViewModel::onQrCodeScanned,
            onConnectorDisplayNameChanged = addLinkConnectorViewModel::onConnectorDisplayNameChanged,
            connectorDisplayName = addLinkConnectorState.connectorDisplayName,
            isNewConnectorContinueButtonEnabled = addLinkConnectorState.isContinueButtonEnabled,
            onNewConnectorContinueClick = {
                coroutineScope.launch {
                    addLinkConnectorViewModel.onContinueClick()
                    viewModel.onNewConnectorCloseClick()
                }
            },
            onNewConnectorCloseClick = {
                addLinkConnectorViewModel.onCloseClick()
                viewModel.onNewConnectorCloseClick()
            }
        )
    } else {
        LinkedConnectorsContent(
            modifier = modifier,
            isLoading = state.isLoading,
            activeLinkedConnectorsList = state.activeConnectors,
            onLinkNewConnectorClick = viewModel::onLinkNewConnectorClick,
            onDeleteConnectorClick = viewModel::onDeleteConnectorClick,
            onBackClick = onBackClick
        )
    }
}

@Composable
private fun LinkedConnectorsContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    activeLinkedConnectorsList: ImmutableList<P2PLink>,
    onLinkNewConnectorClick: () -> Unit,
    onDeleteConnectorClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.linkedConnectors_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { padding ->
        var connectionPasswordToDelete by remember { mutableStateOf<String?>(null) }

        Column(modifier = Modifier.padding(padding)) {
            Divider(color = RadixTheme.colors.gray5)

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    FullscreenCircularProgressContent()
                }

                ActiveLinkedConnectorDetails(
                    activeLinkedConnectorsList = activeLinkedConnectorsList,
                    onLinkNewConnectorClick = onLinkNewConnectorClick,
                    onDeleteConnectorClick = { connectionPasswordToDelete = it },
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                if (connectionPasswordToDelete != null) {
                    @Suppress("UnsafeCallOnNullableType")
                    BasicPromptAlertDialog(
                        finish = {
                            if (it) {
                                onDeleteConnectorClick(connectionPasswordToDelete!!)
                            }
                            connectionPasswordToDelete = null
                        },
                        title = {
                            Text(
                                text = stringResource(id = R.string.linkedConnectors_removeConnectionAlert_title),
                                style = RadixTheme.typography.body2Header,
                                color = RadixTheme.colors.gray1
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(id = R.string.linkedConnectors_removeConnectionAlert_message),
                                style = RadixTheme.typography.body2Regular,
                                color = RadixTheme.colors.gray1
                            )
                        },
                        confirmText = stringResource(id = R.string.common_remove)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveLinkedConnectorDetails(
    activeLinkedConnectorsList: ImmutableList<P2PLink>,
    onLinkNewConnectorClick: () -> Unit,
    onDeleteConnectorClick: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            modifier = Modifier.padding(RadixTheme.dimensions.paddingMedium),
            text = stringResource(R.string.linkedConnectors_subtitle),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray2
        )
        Divider(color = RadixTheme.colors.gray5)
        ActiveLinkedConnectorsListContent(
            activeLinkedConnectorsList = activeLinkedConnectorsList,
            onDeleteConnectorClick = onDeleteConnectorClick
        )
        AnimatedVisibility(!isLoading) {
            Column {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingMedium),
                    text = stringResource(id = R.string.linkedConnectors_linkNewConnector),
                    onClick = {
                        onLinkNewConnectorClick()
                    },
                    icon = {
                        Icon(
                            painter = painterResource(
                                id = com.babylon.wallet.android.designsystem.R.drawable.ic_qr_code_scanner
                            ),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ActiveLinkedConnectorsListContent(
    modifier: Modifier = Modifier,
    activeLinkedConnectorsList: ImmutableList<P2PLink>,
    onDeleteConnectorClick: (String) -> Unit
) {
    LazyColumn(modifier) {
        items(
            items = activeLinkedConnectorsList,
            key = { activeLinkedConnector: P2PLink ->
                activeLinkedConnector.id
            },
            itemContent = { p2pLink ->
                ActiveLinkedConnectorContent(
                    activeLinkedConnector = p2pLink,
                    onDeleteConnectorClick = onDeleteConnectorClick
                )
            }
        )
    }
}

@Composable
private fun ActiveLinkedConnectorContent(
    activeLinkedConnector: P2PLink,
    modifier: Modifier = Modifier,
    onDeleteConnectorClick: (String) -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = activeLinkedConnector.displayName,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
            IconButton(onClick = {
                onDeleteConnectorClick(activeLinkedConnector.connectionPassword)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete_24),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray1
                )
            }
        }
        Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
    }
}

@Preview(showBackground = true)
@Composable
fun LinkedConnectorsContentWithoutActiveLinkedConnectorsPreview() {
    RadixWalletTheme {
        LinkedConnectorsContent(
            activeLinkedConnectorsList = persistentListOf(),
            onLinkNewConnectorClick = {},
            isLoading = false,
            onBackClick = {},
            onDeleteConnectorClick = {}
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun LinkedConnectorsContentWithActiveLinkedConnectorsPreview() {
    RadixWalletTheme {
        LinkedConnectorsContent(
            activeLinkedConnectorsList = SampleDataProvider().p2pLinksSample.toPersistentList(),
            onLinkNewConnectorClick = {},
            isLoading = false,
            onBackClick = {},
            onDeleteConnectorClick = {}
        )
    }
}
