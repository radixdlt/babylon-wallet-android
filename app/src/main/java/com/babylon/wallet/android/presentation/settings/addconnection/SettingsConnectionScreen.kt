@file:OptIn(ExperimentalPermissionsApi::class)

package com.babylon.wallet.android.presentation.settings.addconnection

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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.qr.CameraPreview
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun SettingsConnectionScreen(
    viewModel: SettingsConnectionViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsAddConnectionContent(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        connectionName = state.connectionName,
        onConnectionClick = viewModel::onConnectionClick,
        isLoading = state.isLoading,
        onBackClick = onBackClick,
        connectionDisplayName = state.editedConnectionDisplayName,
        buttonEnabled = state.buttonEnabled,
        onConnectionDisplayNameChanged = viewModel::onConnectionDisplayNameChanged,
        onDeleteConnectionClick = viewModel::onDeleteConnectionClick,
        onConnectionPasswordDecoded = viewModel::onConnectionPasswordDecoded,
        settingsMode = state.mode,
        onAddConnection = viewModel::addConnection,
        cancelQrScan = viewModel::cancelQrScan,
        triggerCameraPermissionPrompt = state.triggerCameraPermissionPrompt
    )
}

@Composable
private fun SettingsAddConnectionContent(
    connectionName: String?,
    onConnectionClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    connectionDisplayName: String,
    buttonEnabled: Boolean,
    onConnectionDisplayNameChanged: (String) -> Unit,
    onConnectionPasswordDecoded: (String) -> Unit,
    onDeleteConnectionClick: () -> Unit,
    settingsMode: SettingsConnectionMode,
    onAddConnection: () -> Unit,
    cancelQrScan: () -> Unit,
    triggerCameraPermissionPrompt: Boolean,
) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    LaunchedEffect(Unit) {
        snapshotFlow { triggerCameraPermissionPrompt }.distinctUntilChanged().filter { it }.collect {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    Column(modifier = modifier) {
        RadixCenteredTopAppBar(
            title = if (settingsMode == SettingsConnectionMode.ShowDetails) {
                stringResource(R.string.linked_connector)
            } else {
                stringResource(R.string.link_to_connector)
            },
            onBackClick = {
                if (settingsMode == SettingsConnectionMode.ScanQr) {
                    cancelQrScan()
                } else {
                    onBackClick()
                }
            },
            contentColor = RadixTheme.colors.gray1
        )
        Divider(color = RadixTheme.colors.gray5)
        Box(modifier = Modifier.fillMaxSize()) {
            when (settingsMode) {
                SettingsConnectionMode.AddConnection -> {
                    ConnectionNameInput(
                        onConnectionClick = onConnectionClick,
                        connectionDisplayName = connectionDisplayName,
                        buttonEnabled = buttonEnabled,
                        onConnectionDisplayNameChanged = onConnectionDisplayNameChanged,
                    )
                }
                SettingsConnectionMode.ShowDetails -> {
                    ActiveConnectionDetails(
                        connectionName,
                        onAddConnection,
                        cameraPermissionState,
                        onDeleteConnectionClick,
                        Modifier.fillMaxWidth()
                    )
                }
                SettingsConnectionMode.ScanQr -> {
                    if (cameraPermissionState.status.isGranted) {
                        CameraPreview(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            onConnectionPasswordDecoded(it)
                        }
                        BackHandler(enabled = true) { }
                    }
                }
            }
            if (isLoading) {
                FullscreenCircularProgressContent()
            }
        }
    }
}

@Composable
private fun ActiveConnectionDetails(
    connectionName: String?,
    onAddConnection: () -> Unit,
    cameraPermissionState: PermissionState,
    onDeleteConnectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            modifier = Modifier.padding(RadixTheme.dimensions.paddingMedium),
            text = stringResource(R.string.your_radix_wallet_is_linked),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray2
        )
        Divider(color = RadixTheme.colors.gray5)
        AnimatedVisibility(visible = connectionName == null) {
            Column {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingMedium),
                    text = stringResource(id = R.string.link_new_connector),
                    onClick = {
                        onAddConnection()
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
        connectionName?.let { connectionName ->
            ShowConnectionContent(
                connectionName = connectionName,
                onDeleteConnectionClick = onDeleteConnectionClick
            )
        }
    }
}

@Composable
private fun ShowConnectionContent(
    connectionName: String,
    modifier: Modifier = Modifier,
    onDeleteConnectionClick: () -> Unit,
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
                text = connectionName,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
            IconButton(onClick = onDeleteConnectionClick) {
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
private fun ConnectionNameInput(
    modifier: Modifier = Modifier,
    onConnectionClick: () -> Unit,
    connectionDisplayName: String,
    buttonEnabled: Boolean,
    onConnectionDisplayNameChanged: (String) -> Unit,
) {
    Column(modifier = modifier) {
        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            value = connectionDisplayName,
            onValueChanged = onConnectionDisplayNameChanged,
            hint = stringResource(R.string.name_of_connector),
            singleLine = true,
            optionalHint = stringResource(id = R.string.hint_name_this_connector)
        )
        Spacer(modifier = Modifier.size(RadixTheme.dimensions.paddingMedium))
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            text = stringResource(id = R.string.save_link),
            onClick = onConnectionClick,
            enabled = buttonEnabled
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun SettingsScreenAddConnectionWithoutActiveConnectionPreview() {
    RadixWalletTheme {
        SettingsAddConnectionContent(
            connectionName = "",
            onConnectionClick = {},
            isLoading = false,
            onBackClick = {},
            connectionDisplayName = "",
            buttonEnabled = false,
            onConnectionDisplayNameChanged = {},
            onConnectionPasswordDecoded = {},
            onDeleteConnectionClick = {},
            settingsMode = SettingsConnectionMode.ShowDetails,
            onAddConnection = {},
            cancelQrScan = {},
            triggerCameraPermissionPrompt = false
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun SettingsScreenAddConnectionWithActiveConnectionPreview() {
    RadixWalletTheme {
        SettingsAddConnectionContent(
            connectionName = "my cool connection",
            onConnectionClick = {},
            isLoading = false,
            onBackClick = {},
            connectionDisplayName = "",
            buttonEnabled = true,
            onConnectionDisplayNameChanged = {},
            onConnectionPasswordDecoded = {},
            onDeleteConnectionClick = {},
            settingsMode = SettingsConnectionMode.ShowDetails,
            onAddConnection = {},
            cancelQrScan = {},
            triggerCameraPermissionPrompt = false
        )
    }
}
