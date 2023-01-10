@file:OptIn(ExperimentalPermissionsApi::class)

package com.babylon.wallet.android.presentation.settings.addconnection

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun SettingsConnectionScreen(
    viewModel: SettingsConnectionViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var connectionDisplayName by rememberSaveable { mutableStateOf("") }
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    val state by viewModel.state.collectAsStateWithLifecycle()
    PermissionRequired(
        permissionState = cameraPermissionState,
        permissionNotGrantedContent = {},
        permissionNotAvailableContent = {}
    ) {
        CameraPreview(
            modifier = Modifier.fillMaxWidth()
        ) {
        }
        BackHandler(enabled = true) { }
    }

//    SettingsAddConnectionContent(
//        modifier = modifier
////                .systemBarsPadding()
//            .navigationBarsPadding()
//            .fillMaxSize()
//            .background(RadixTheme.colors.defaultBackground),
//        connectionName = state.connectionName,
//        onConnectionClick = {
//            viewModel.onConnectionClick(
//                connectionDisplayName = connectionDisplayName
//            )
//        },
//        isLoading = state.isLoading,
//        onBackClick = onBackClick,
//        connectionDisplayName = connectionDisplayName,
//        onConnectionDisplayNameChanged = { connectionDisplayName = it },
//        onDeleteConnectionClick = viewModel::onDeleteConnectionClick,
//        onConnectionPasswordDecoded = viewModel::onConnectionPasswordDecoded,
//        settingsMode = state.mode,
//        onAddConnection = viewModel::addConnection,
//        cancelQrScan = viewModel::cancelQrScan
//    )
}

@Composable
private fun SettingsAddConnectionContent(
    connectionName: String?,
    onConnectionClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    connectionDisplayName: String,
    onConnectionDisplayNameChanged: (String) -> Unit,
    onConnectionPasswordDecoded: (String) -> Unit,
    onDeleteConnectionClick: () -> Unit,
    settingsMode: SettingsConnectionMode,
    onAddConnection: () -> Unit,
    cancelQrScan: () -> Unit,
) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    Column(modifier = modifier) {
        RadixCenteredTopAppBar(
            title = if (settingsMode == SettingsConnectionMode.AddConnection) stringResource(R.string.link_to_connector) else stringResource(
                R.string.linked_connector),
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
        when (settingsMode) {
            SettingsConnectionMode.AddConnection -> {
                EnterConnection(
                    onConnectionClick = onConnectionClick,
                    connectionDisplayName = connectionDisplayName,
                    onConnectionDisplayNameChanged = onConnectionDisplayNameChanged,
                )
            }
            SettingsConnectionMode.ShowDetails -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
                            text = stringResource(R.string.your_radix_wallet_is_linked),
                            style = RadixTheme.typography.body2Regular,
                            color = RadixTheme.colors.gray2
                        )
                        Divider(color = RadixTheme.colors.gray5)
                        AnimatedVisibility(visible = connectionName == null) {
                            RadixSecondaryButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RadixTheme.dimensions.paddingMedium),
                                text = stringResource(id = R.string.link_to_connector),
                                onClick = {
                                    onAddConnection()
                                },
                                icon = {
                                    Icon(painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_qr_code_scanner),
                                        contentDescription = null)
                                }
                            )
                        }
                        AnimatedVisibility(visible = connectionName?.isNotEmpty() == true) {
                            ShowConnectionContent(
                                connectionName = connectionName!!,
                                onDeleteConnectionClick = onDeleteConnectionClick
                            )
                        }
                    }
                    if (isLoading) {
                        FullscreenCircularProgressContent()
                    }
                }
            }
            SettingsConnectionMode.ScanQr -> {
                PermissionRequired(
                    permissionState = cameraPermissionState,
                    permissionNotGrantedContent = {},
                    permissionNotAvailableContent = {}
                ) {
                    CameraPreview(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        onConnectionPasswordDecoded(it)
                    }
                    BackHandler(enabled = true) { }
                }
            }
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
        Spacer(modifier = Modifier.size(RadixTheme.dimensions.paddingSmall))
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
private fun EnterConnection(
    modifier: Modifier = Modifier,
    onConnectionClick: () -> Unit,
    connectionDisplayName: String,
    onConnectionDisplayNameChanged: (String) -> Unit,
) {
    Column(modifier = modifier) {
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
                .imePadding()
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
    RadixWalletTheme {
        SettingsAddConnectionContent(
            connectionName = "",
            onConnectionClick = {},
            isLoading = false,
            onBackClick = {},
            connectionDisplayName = "",
            onConnectionDisplayNameChanged = {},
            onConnectionPasswordDecoded = {},
            onDeleteConnectionClick = {},
            settingsMode = SettingsConnectionMode.ShowDetails,
            onAddConnection = {},
            cancelQrScan = {}
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
            onConnectionDisplayNameChanged = {},
            onConnectionPasswordDecoded = {},
            onDeleteConnectionClick = {},
            cancelQrScan = {},
            settingsMode = SettingsConnectionMode.ShowDetails,
            onAddConnection = {},
        )
    }
}
