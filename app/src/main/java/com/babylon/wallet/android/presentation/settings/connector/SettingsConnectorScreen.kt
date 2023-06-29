@file:OptIn(ExperimentalPermissionsApi::class)

package com.babylon.wallet.android.presentation.settings.connector

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.settings.connector.qrcode.CameraPreview
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun SettingsConnectorScreen(
    viewModel: SettingsConnectorViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                Event.Close -> onBackClick()
            }
        }
    }
    SettingsLinkConnectorContent(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        activeConnectors = state.activeConnectors,
        onLinkNewConnectorClick = viewModel::onLinkNewConnectorClick,
        isLoading = state.isLoading,
        onBackClick = onBackClick,
        connectorDisplayName = state.editedConnectorDisplayName,
        buttonEnabled = state.buttonEnabled,
        onConnectorDisplayNameChanged = viewModel::onConnectorDisplayNameChanged,
        onDeleteConnectorClick = viewModel::onDeleteConnectorClick,
        onConnectionPasswordDecoded = viewModel::onConnectionPasswordDecoded,
        settingsMode = state.mode,
        onLinkConnector = viewModel::linkConnector,
        cancelQrScan = viewModel::cancelQrScan,
        triggerCameraPermissionPrompt = state.triggerCameraPermissionPrompt
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun SettingsLinkConnectorContent(
    activeConnectors: ImmutableList<ActiveConnectorUiModel>,
    onLinkNewConnectorClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    connectorDisplayName: String,
    buttonEnabled: Boolean,
    onConnectorDisplayNameChanged: (String) -> Unit,
    onConnectionPasswordDecoded: (String) -> Unit,
    onDeleteConnectorClick: (String) -> Unit,
    settingsMode: SettingsConnectorMode,
    onLinkConnector: () -> Unit,
    cancelQrScan: () -> Unit,
    triggerCameraPermissionPrompt: Boolean,
) {
    val backHandler = {
        if (settingsMode == SettingsConnectorMode.ScanQr) {
            cancelQrScan()
        } else {
            onBackClick()
        }
    }
    BackHandler(onBack = backHandler)
    var connectionPasswordToDelete by remember { mutableStateOf<String?>(null) }
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    LaunchedEffect(Unit) {
        snapshotFlow { triggerCameraPermissionPrompt }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                cameraPermissionState.launchPermissionRequest()
            }
    }

    Column(modifier = modifier) {
        RadixCenteredTopAppBar(
            title = if (settingsMode == SettingsConnectorMode.ShowDetails) {
                stringResource(R.string.linkedConnectors_title)
            } else {
                stringResource(R.string.linkedConnectors_newConnection_title)
            },
            onBackClick = backHandler,
            contentColor = RadixTheme.colors.gray1
        )
        Divider(color = RadixTheme.colors.gray5)
        Box(modifier = Modifier.fillMaxSize()) {
            when (settingsMode) {
                SettingsConnectorMode.LinkConnector -> {
                    ConnectorNameInput(
                        onLinkNewConnectorClick = onLinkNewConnectorClick,
                        connectorDisplayName = connectorDisplayName,
                        buttonEnabled = buttonEnabled,
                        onConnectorDisplayNameChanged = onConnectorDisplayNameChanged,
                    )
                }
                SettingsConnectorMode.ShowDetails -> {
                    ActiveConnectorDetails(
                        activeConnectors = activeConnectors,
                        onLinkConnector = onLinkConnector,
                        cameraPermissionState = cameraPermissionState,
                        onDeleteConnectorClick = { connectionPasswordToDelete = it },
                        isLoading = isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SettingsConnectorMode.ScanQr -> {
                    if (cameraPermissionState.status.isGranted) {
                        CameraPreview(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            onConnectionPasswordDecoded(it)
                        }
                    }
                }
            }
            if (isLoading) {
                FullscreenCircularProgressContent()
            }
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ActiveConnectorDetails(
    activeConnectors: ImmutableList<ActiveConnectorUiModel>,
    onLinkConnector: () -> Unit,
    cameraPermissionState: PermissionState,
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
        ActiveConnectorsListContent(
            activeConnectors = activeConnectors,
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
                        onLinkConnector()
                        cameraPermissionState.launchPermissionRequest()
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
private fun ActiveConnectorsListContent(
    modifier: Modifier = Modifier,
    activeConnectors: ImmutableList<ActiveConnectorUiModel>,
    onDeleteConnectorClick: (String) -> Unit
) {
    LazyColumn(modifier) {
        items(
            items = activeConnectors,
            key = { activeConnectorUiModel: ActiveConnectorUiModel ->
                activeConnectorUiModel.id
            },
            itemContent = { activeConnectorUiModel ->
                ShowConnectorContent(
                    activeConnectorUiModel = activeConnectorUiModel,
                    onDeleteConnectorClick = onDeleteConnectorClick
                )
            }
        )
    }
}

@Composable
private fun ShowConnectorContent(
    activeConnectorUiModel: ActiveConnectorUiModel,
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
                text = activeConnectorUiModel.name,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
            IconButton(onClick = {
                onDeleteConnectorClick(activeConnectorUiModel.connectionPassword)
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

@Composable
private fun ConnectorNameInput(
    modifier: Modifier = Modifier,
    onLinkNewConnectorClick: () -> Unit,
    connectorDisplayName: String,
    buttonEnabled: Boolean,
    onConnectorDisplayNameChanged: (String) -> Unit,
) {
    Column(modifier = modifier) {
        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            onValueChanged = onConnectorDisplayNameChanged,
            value = connectorDisplayName,
            hint = stringResource(R.string.linkedConnectors_nameNewConnector_textFieldPlaceholder),
            optionalHint = stringResource(id = R.string.linkedConnectors_nameNewConnector_textFieldHint),
            singleLine = true
        )
        Spacer(modifier = Modifier.size(RadixTheme.dimensions.paddingMedium))
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            text = stringResource(id = R.string.linkedConnectors_nameNewConnector_saveLinkButtonTitle),
            onClick = onLinkNewConnectorClick,
            enabled = buttonEnabled
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun SettingsScreenLinkConnectorWithoutActiveConnectorPreview() {
    RadixWalletTheme {
        SettingsLinkConnectorContent(
            activeConnectors = persistentListOf(),
            onLinkNewConnectorClick = {},
            isLoading = false,
            onBackClick = {},
            connectorDisplayName = "",
            buttonEnabled = false,
            onConnectorDisplayNameChanged = {},
            onConnectionPasswordDecoded = {},
            onDeleteConnectorClick = {},
            settingsMode = SettingsConnectorMode.ShowDetails,
            onLinkConnector = {},
            cancelQrScan = {},
            triggerCameraPermissionPrompt = false
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun SettingsScreenLinkConnectorWithActiveConnectorPreview() {
    RadixWalletTheme {
        SettingsLinkConnectorContent(
            activeConnectors = persistentListOf(
                ActiveConnectorUiModel(
                    id = "id",
                    name = "my cool connection",
                    connectionPassword = "conn pass"
                )
            ),
            onLinkNewConnectorClick = {},
            isLoading = false,
            onBackClick = {},
            connectorDisplayName = "",
            buttonEnabled = true,
            onConnectorDisplayNameChanged = {},
            onConnectionPasswordDecoded = {},
            onDeleteConnectorClick = {},
            settingsMode = SettingsConnectorMode.ShowDetails,
            onLinkConnector = {},
            cancelQrScan = {},
            triggerCameraPermissionPrompt = false
        )
    }
}
