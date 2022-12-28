package com.babylon.wallet.android.presentation.settings.addconnection

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.qr.CameraPreview
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalLifecycleComposeApi::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsAddConnectionScreen(
    viewModel: SettingsAddConnectionViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var showQrScanner by rememberSaveable { mutableStateOf(false) }

    var connectionPasswordText by rememberSaveable { mutableStateOf("") }
    var connectionDisplayName by rememberSaveable { mutableStateOf("") }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (showQrScanner) {
        PermissionRequired(
            permissionState = cameraPermissionState,
            permissionNotGrantedContent = {},
            permissionNotAvailableContent = {}
        ) {
            CameraPreview(
                modifier = Modifier,
                onQrCodeDetected = {
                    connectionPasswordText = it
                    showQrScanner = false
                },
                onBackClick = { showQrScanner = false }
            )
        }
    } else {
        SettingsAddConnectionContent(
            modifier = modifier
                .systemBarsPadding()
                .fillMaxSize()
                .background(RadixTheme.colors.defaultBackground),
            connectionName = state.connectionName,
            onConnectionClick = {
                viewModel.onConnectionClick(
                    connectionPassword = connectionPasswordText,
                    connectionDisplayName = connectionDisplayName
                )
            },
            isLoading = state.isLoading,
            onBackClick = onBackClick,
            onQrCodeScannerClick = {
                cameraPermissionState.launchPermissionRequest()
                showQrScanner = true
            },
            connectionPassword = connectionPasswordText,
            connectionDisplayName = connectionDisplayName,
            onConnectionPasswordChanged = { connectionPasswordText = it },
            onConnectionDisplayNameChanged = { connectionDisplayName = it },
            onDeleteConnectionClick = viewModel::onDeleteConnectionClick
        )
    }
}

@Composable
private fun SettingsAddConnectionContent(
    connectionName: String?,
    onConnectionClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onQrCodeScannerClick: () -> Unit,
    connectionPassword: String,
    connectionDisplayName: String,
    onConnectionPasswordChanged: (String) -> Unit,
    onConnectionDisplayNameChanged: (String) -> Unit,
    onDeleteConnectionClick: () -> Unit
) {
    Column(modifier = modifier) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.new_connection),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1
        )
        if (connectionName?.isNotEmpty() == true) {
            ShowConnectionContent(
                connectionName = connectionName,
                onDeleteConnectionClick = onDeleteConnectionClick
            )
        } else {
            if (isLoading) {
                FullscreenCircularProgressContent()
            } else {
                EnterConnection(
                    onConnectionClick = onConnectionClick,
                    connectionPassword = connectionPassword,
                    connectionDisplayName = connectionDisplayName,
                    onConnectionPasswordChanged = onConnectionPasswordChanged,
                    onConnectionDisplayNameChanged = onConnectionDisplayNameChanged,
                    onQrCodeScannerClick = onQrCodeScannerClick
                )
            }
        }
    }
}

@Composable
private fun ShowConnectionContent(
    connectionName: String,
    modifier: Modifier = Modifier,
    onDeleteConnectionClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = "You have an active connection with name: $connectionName",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.size(RadixTheme.dimensions.paddingMedium))
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            text = stringResource(id = R.string.delete_connection),
            onClick = onDeleteConnectionClick
        )
    }
}

@Composable
private fun EnterConnection(
    modifier: Modifier = Modifier,
    onConnectionClick: () -> Unit,
    connectionPassword: String,
    connectionDisplayName: String,
    onConnectionPasswordChanged: (String) -> Unit,
    onConnectionDisplayNameChanged: (String) -> Unit,
    onQrCodeScannerClick: () -> Unit,
) {
    Column(modifier = modifier) {
        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            value = connectionPassword,
            onValueChanged = onConnectionPasswordChanged,
            hint = stringResource(R.string.enter_the_connection_id),
            trailingIcon = {
                IconButton(onClick = onQrCodeScannerClick) {
                    Icon(
                        painterResource(
                            id = com.babylon.wallet.android.designsystem.R.drawable.ic_qr_code_scanner
                        ),
                        contentDescription = "scan qr code icon"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.size(RadixTheme.dimensions.paddingMedium))

        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            value = connectionDisplayName,
            onValueChanged = onConnectionDisplayNameChanged,
            hint = stringResource(R.string.enter_the_display_name)
        )

        Spacer(modifier = Modifier.size(RadixTheme.dimensions.paddingMedium))

        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            text = stringResource(id = R.string.add_connection),
            onClick = onConnectionClick
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun SettingsScreenAddConnectionWithoutActiveConnectionPreview() {
    BabylonWalletTheme {
        SettingsAddConnectionContent(
            connectionName = "",
            onConnectionClick = {},
            isLoading = false,
            onBackClick = {},
            onQrCodeScannerClick = {},
            connectionPassword = "",
            connectionDisplayName = "",
            onConnectionPasswordChanged = {},
            onConnectionDisplayNameChanged = {},
            onDeleteConnectionClick = {}
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun SettingsScreenAddConnectionWithActiveConnectionPreview() {
    BabylonWalletTheme {
        SettingsAddConnectionContent(
            connectionName = "my cool connection",
            onConnectionClick = {},
            isLoading = false,
            onBackClick = {},
            onQrCodeScannerClick = {},
            connectionPassword = "",
            connectionDisplayName = "",
            onConnectionPasswordChanged = {},
            onConnectionDisplayNameChanged = {},
            onDeleteConnectionClick = {}
        )
    }
}
