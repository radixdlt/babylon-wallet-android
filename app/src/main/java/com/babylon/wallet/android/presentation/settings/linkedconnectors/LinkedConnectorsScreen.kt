package com.babylon.wallet.android.presentation.settings.linkedconnectors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.linkedconnectors.LinkedConnectorsUiState.ConnectorUiItem
import com.babylon.wallet.android.presentation.ui.composables.AddLinkConnectorScreen
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.RenameBottomSheet
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.SyncSheetState
import com.radixdlt.sargon.PublicKeyHash
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedConnectorsScreen(
    viewModel: LinkedConnectorsViewModel,
    addLinkConnectorViewModel: AddLinkConnectorViewModel,
    onInfoClick: (GlossaryItem) -> Unit,
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

    LaunchedEffect(Unit) {
        addLinkConnectorViewModel.oneOffEvent.collect { event ->
            when (event) {
                is AddLinkConnectorViewModel.Event.Close -> viewModel.onNewConnectorCloseClick()
            }
        }
    }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SyncSheetState(
        sheetState = bottomSheetState,
        isSheetVisible = state.renameLinkConnectorItem != null,
        onSheetClosed = {
            viewModel.setRenameConnectorSheetVisible(false)
        }
    )

    if (state.showAddLinkConnectorScreen) {
        AddLinkConnectorScreen(
            modifier = modifier,
            state = addLinkConnectorState,
            onQrCodeScanned = addLinkConnectorViewModel::onQrCodeScanned,
            onQrCodeScanFailure = addLinkConnectorViewModel::onQrCodeScanFailure,
            onConnectorDisplayNameChanged = addLinkConnectorViewModel::onConnectorDisplayNameChanged,
            onInfoClick = onInfoClick,
            onContinueClick = addLinkConnectorViewModel::onContinueClick,
            onCloseClick = addLinkConnectorViewModel::onCloseClick,
            onErrorDismiss = addLinkConnectorViewModel::onErrorDismiss
        )
    } else {
        LinkedConnectorsContent(
            modifier = modifier,
            isAddingNewLinkConnectorInProgress = addLinkConnectorState.isAddingNewLinkConnectorInProgress,
            activeLinkedConnectorsList = state.activeConnectors,
            isLinkConnectorNameUpdated = state.isLinkConnectorNameUpdated,
            onSnackbarMessageShown = viewModel::onSnackbarMessageShown,
            onLinkNewConnectorClick = viewModel::onLinkNewConnectorClick,
            onRenameConnectorClick = { viewModel.setRenameConnectorSheetVisible(true, it) },
            onDeleteConnectorClick = viewModel::onDeleteConnectorClick,
            onBackClick = onBackClick
        )

        state.renameLinkConnectorItem?.let {
            RenameBottomSheet(
                sheetState = bottomSheetState,
                renameInput = it,
                titleRes = R.string.linkedConnectors_renameConnector_title,
                subtitleRes = R.string.linkedConnectors_renameConnector_subtitle,
                errorValidationMessageRes = R.string.linkedConnectors_renameConnector_errorEmpty,
                onNameChange = viewModel::onNewConnectorNameChanged,
                onUpdateNameClick = viewModel::onUpdateConnectorNameClick,
                onDismiss = { viewModel.setRenameConnectorSheetVisible(false) },
            )
        }
    }
}

@Composable
private fun LinkedConnectorsContent(
    modifier: Modifier = Modifier,
    isAddingNewLinkConnectorInProgress: Boolean,
    activeLinkedConnectorsList: ImmutableList<ConnectorUiItem>,
    isLinkConnectorNameUpdated: Boolean,
    onSnackbarMessageShown: () -> Unit,
    onLinkNewConnectorClick: () -> Unit,
    onRenameConnectorClick: (connectorUiItem: ConnectorUiItem) -> Unit,
    onDeleteConnectorClick: (id: PublicKeyHash) -> Unit,
    onBackClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = stringResource(R.string.linkedConnectors_renameConnector_successHud)
    LaunchedEffect(isLinkConnectorNameUpdated) {
        if (isLinkConnectorNameUpdated) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
            onSnackbarMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.linkedConnectors_title),
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )

                HorizontalDivider(color = RadixTheme.colors.divider)
            }

        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackbarHostState
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        var connectionLinkToDelete by remember { mutableStateOf<PublicKeyHash?>(null) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ActiveLinkedConnectorsListContent(
                modifier = Modifier.fillMaxWidth(),
                activeLinkedConnectorsList = activeLinkedConnectorsList,
                onLinkNewConnectorClick = onLinkNewConnectorClick,
                onRenameConnectorClick = onRenameConnectorClick,
                onDeleteConnectorClick = { connectionLinkToDelete = it },
                isAddingNewLinkConnectorInProgress = isAddingNewLinkConnectorInProgress
            )

            if (connectionLinkToDelete != null) {
                @Suppress("UnsafeCallOnNullableType")
                BasicPromptAlertDialog(
                    finish = {
                        if (it) {
                            onDeleteConnectorClick(connectionLinkToDelete!!)
                        }
                        connectionLinkToDelete = null
                    },
                    title = {
                        Text(
                            text = stringResource(id = R.string.linkedConnectors_removeConnectionAlert_title),
                            style = RadixTheme.typography.body2Header,
                            color = RadixTheme.colors.text
                        )
                    },
                    message = {
                        Text(
                            text = stringResource(id = R.string.linkedConnectors_removeConnectionAlert_message),
                            style = RadixTheme.typography.body2Regular,
                            color = RadixTheme.colors.text
                        )
                    },
                    confirmText = stringResource(id = R.string.common_remove),
                    confirmTextColor = RadixTheme.colors.error
                )
            }
        }
    }
}

@Composable
private fun ActiveLinkedConnectorsListContent(
    modifier: Modifier = Modifier,
    activeLinkedConnectorsList: ImmutableList<ConnectorUiItem>,
    onRenameConnectorClick: (connectorUiItem: ConnectorUiItem) -> Unit,
    onDeleteConnectorClick: (id: PublicKeyHash) -> Unit,
    isAddingNewLinkConnectorInProgress: Boolean,
    onLinkNewConnectorClick: () -> Unit
) {
    LazyColumn(modifier) {
        item {
            Text(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                text = stringResource(R.string.linkedConnectors_subtitle),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.textSecondary
            )
        }
        itemsIndexed(activeLinkedConnectorsList) { index, activeLinkedConnector ->
            ActiveLinkedConnectorContent(
                activeLinkedConnector = activeLinkedConnector,
                onRenameConnectorClick = onRenameConnectorClick,
                onDeleteConnectorClick = onDeleteConnectorClick
            )
            if (remember(activeLinkedConnectorsList.size) { index < activeLinkedConnectorsList.size - 1 }) {
                HorizontalDivider(
                    color = RadixTheme.colors.divider,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                )
            }
        }
        item {
            HorizontalDivider(
                color = RadixTheme.colors.divider,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Column {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingMedium)
                        .padding(bottom = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.linkedConnectors_linkNewConnector),
                    onClick = onLinkNewConnectorClick,
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = DSR.ic_qr_code_scanner),
                            contentDescription = null,
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
    activeLinkedConnector: ConnectorUiItem,
    modifier: Modifier = Modifier,
    onRenameConnectorClick: (connectorUiItem: ConnectorUiItem) -> Unit,
    onDeleteConnectorClick: (id: PublicKeyHash) -> Unit,
) {
    Column(modifier = modifier.background(color = RadixTheme.colors.background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = activeLinkedConnector.name,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = { onRenameConnectorClick(activeLinkedConnector) }
            ) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_account_label),
                    contentDescription = null,
                    tint = RadixTheme.colors.icon
                )
            }
            IconButton(onClick = {
                onDeleteConnectorClick(activeLinkedConnector.id)
            }) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_delete_outline),
                    contentDescription = null,
                    tint = RadixTheme.colors.icon
                )
            }
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun LinkedConnectorsContentWithActiveLinkedConnectorsPreview() {
    RadixWalletTheme {
        LinkedConnectorsContent(
            activeLinkedConnectorsList = listOf(
                ConnectorUiItem(
                    id = PublicKeyHash.sample.invoke(),
                    name = "chrome connection"
                ),
                ConnectorUiItem(
                    id = PublicKeyHash.sample.other(),
                    name = "firefox connection"
                )
            ).toPersistentList(),
            isAddingNewLinkConnectorInProgress = false,
            isLinkConnectorNameUpdated = false,
            onSnackbarMessageShown = {},
            onLinkNewConnectorClick = {},
            onBackClick = {},
            onRenameConnectorClick = {},
            onDeleteConnectorClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LinkedConnectorsContentWithoutActiveLinkedConnectorsPreview() {
    RadixWalletTheme {
        LinkedConnectorsContent(
            activeLinkedConnectorsList = persistentListOf(),
            isAddingNewLinkConnectorInProgress = false,
            isLinkConnectorNameUpdated = false,
            onLinkNewConnectorClick = {},
            onSnackbarMessageShown = {},
            onBackClick = {},
            onRenameConnectorClick = {},
            onDeleteConnectorClick = {}
        )
    }
}
