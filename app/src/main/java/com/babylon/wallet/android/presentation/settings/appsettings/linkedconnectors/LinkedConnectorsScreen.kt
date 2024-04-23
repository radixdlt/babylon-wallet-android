package com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.babylon.wallet.android.presentation.ui.composables.AddLinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.samples.sample
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun LinkedConnectorsScreen(
    viewModel: LinkedConnectorsViewModel,
    addLinkConnectorViewModel: AddLinkConnectorViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addLinkConnectorState by addLinkConnectorViewModel.state.collectAsStateWithLifecycle()
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
            onQrCodeScanned = addLinkConnectorViewModel::onQrCodeScanned,
            onConnectorDisplayNameChanged = addLinkConnectorViewModel::onConnectorDisplayNameChanged,
            connectorDisplayName = addLinkConnectorState.connectorDisplayName,
            isNewConnectorContinueButtonEnabled = addLinkConnectorState.isContinueButtonEnabled,
            onNewConnectorContinueClick = {
                addLinkConnectorViewModel.onContinueClick()
                viewModel.onNewConnectorCloseClick()
            },
            onNewConnectorCloseClick = {
                addLinkConnectorViewModel.onCloseClick()
                viewModel.onNewConnectorCloseClick()
            },
            invalidConnectionPassword = addLinkConnectorState.invalidConnectionPassword,
            onInvalidConnectionPasswordDismissed = addLinkConnectorViewModel::onInvalidConnectionPasswordShown
        )
    } else {
        LinkedConnectorsContent(
            modifier = modifier,
            isAddingNewLinkConnectorInProgress = addLinkConnectorState.isAddingNewLinkConnectorInProgress,
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
    isAddingNewLinkConnectorInProgress: Boolean,
    activeLinkedConnectorsList: ImmutableList<P2pLink>,
    onLinkNewConnectorClick: () -> Unit,
    onDeleteConnectorClick: (P2pLink) -> Unit,
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
        var onLinkToDelete by remember { mutableStateOf<P2pLink?>(null) }

        Column(modifier = Modifier.padding(padding)) {
            HorizontalDivider(color = RadixTheme.colors.gray5)

            Box(modifier = Modifier.fillMaxSize()) {
                ActiveLinkedConnectorDetails(
                    activeLinkedConnectorsList = activeLinkedConnectorsList,
                    onLinkNewConnectorClick = onLinkNewConnectorClick,
                    onDeleteConnectorClick = { onLinkToDelete = it },
                    isAddingNewLinkConnectorInProgress = isAddingNewLinkConnectorInProgress,
                    modifier = Modifier.fillMaxWidth()
                )

                if (onLinkToDelete != null) {
                    @Suppress("UnsafeCallOnNullableType")
                    BasicPromptAlertDialog(
                        finish = {
                            if (it) {
                                onDeleteConnectorClick(onLinkToDelete!!)
                            }
                            onLinkToDelete = null
                        },
                        title = {
                            Text(
                                text = stringResource(id = R.string.linkedConnectors_removeConnectionAlert_title),
                                style = RadixTheme.typography.body2Header,
                                color = RadixTheme.colors.gray1
                            )
                        },
                        message = {
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
    activeLinkedConnectorsList: ImmutableList<P2pLink>,
    onLinkNewConnectorClick: () -> Unit,
    onDeleteConnectorClick: (P2pLink) -> Unit,
    isAddingNewLinkConnectorInProgress: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            modifier = Modifier.padding(RadixTheme.dimensions.paddingMedium),
            text = stringResource(R.string.linkedConnectors_subtitle),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray2
        )
        HorizontalDivider(color = RadixTheme.colors.gray5)
        ActiveLinkedConnectorsListContent(
            activeLinkedConnectorsList = activeLinkedConnectorsList,
            onDeleteConnectorClick = onDeleteConnectorClick,
            isAddingNewLinkConnectorInProgress = isAddingNewLinkConnectorInProgress,
            onLinkNewConnectorClick = onLinkNewConnectorClick
        )
    }
}

@Composable
private fun ActiveLinkedConnectorsListContent(
    modifier: Modifier = Modifier,
    activeLinkedConnectorsList: ImmutableList<P2pLink>,
    onDeleteConnectorClick: (P2pLink) -> Unit,
    isAddingNewLinkConnectorInProgress: Boolean,
    onLinkNewConnectorClick: () -> Unit
) {
    LazyColumn(modifier) {
        items(
            items = activeLinkedConnectorsList,
            key = { activeLinkedConnector ->
                activeLinkedConnector.id.hex
            },
            itemContent = { p2pLink ->
                ActiveLinkedConnectorContent(
                    activeLinkedConnector = p2pLink,
                    onDeleteConnectorClick = onDeleteConnectorClick
                )
            }
        )
        item {
            Column {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingMedium),
                    text = stringResource(id = R.string.linkedConnectors_linkNewConnector),
                    onClick = onLinkNewConnectorClick,
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = DSR.ic_qr_code_scanner),
                            contentDescription = null
                        )
                    },
                    isLoading = isAddingNewLinkConnectorInProgress,
                    enabled = isAddingNewLinkConnectorInProgress.not()
                )
            }
        }
    }
}

@Composable
private fun ActiveLinkedConnectorContent(
    activeLinkedConnector: P2pLink,
    modifier: Modifier = Modifier,
    onDeleteConnectorClick: (P2pLink) -> Unit,
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
                onDeleteConnectorClick(activeLinkedConnector)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete_24),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray1
                )
            }
        }
        HorizontalDivider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
    }
}

@Preview(showBackground = true)
@Composable
fun LinkedConnectorsContentWithoutActiveLinkedConnectorsPreview() {
    RadixWalletTheme {
        LinkedConnectorsContent(
            activeLinkedConnectorsList = persistentListOf(),
            onLinkNewConnectorClick = {},
            isAddingNewLinkConnectorInProgress = false,
            onBackClick = {},
            onDeleteConnectorClick = {}
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun LinkedConnectorsContentWithActiveLinkedConnectorsPreview() {
    RadixWalletTheme {
        LinkedConnectorsContent(
            activeLinkedConnectorsList= persistentListOf(P2pLink.sample()),
            onLinkNewConnectorClick = {},
            isAddingNewLinkConnectorInProgress = false,
            onBackClick = {},
            onDeleteConnectorClick = {}
        )
    }
}
