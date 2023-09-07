package com.babylon.wallet.android.presentation.ui.composables

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors.AddLinkConnectorUiState
import com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors.qrcode.CameraPreview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AddLinkConnectorScreen(
    modifier: Modifier,
    showContent: AddLinkConnectorUiState.ShowContent,
    isLoading: Boolean,
    onQrCodeScanned: (String) -> Unit,
    onConnectorDisplayNameChanged: (String) -> Unit,
    connectorDisplayName: String,
    isNewConnectorContinueButtonEnabled: Boolean,
    onNewConnectorContinueClick: () -> Unit,
    onNewConnectorCloseClick: () -> Unit
) {
    BackHandler(onBack = onNewConnectorCloseClick)

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    AddLinkConnectorContent(
        modifier = modifier,
        showContent = showContent,
        isLoading = isLoading,
        isCameraPermissionGranted = cameraPermissionState.status.isGranted,
        onQrCodeScanned = onQrCodeScanned,
        onConnectorDisplayNameChanged = onConnectorDisplayNameChanged,
        connectorDisplayName = connectorDisplayName,
        isContinueButtonEnabled = isNewConnectorContinueButtonEnabled,
        onContinueClick = onNewConnectorContinueClick,
        onCloseClick = onNewConnectorCloseClick
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AddLinkConnectorContent(
    modifier: Modifier = Modifier,
    showContent: AddLinkConnectorUiState.ShowContent,
    isLoading: Boolean,
    isCameraPermissionGranted: Boolean,
    onQrCodeScanned: (String) -> Unit,
    connectorDisplayName: String,
    onConnectorDisplayNameChanged: (String) -> Unit,
    isContinueButtonEnabled: Boolean,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onCloseClick,
                backIconType = BackIconType.Close,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.defaultBackground,
    ) { padding ->
        val keyboardController = LocalSoftwareKeyboardController.current
        Column(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                FullscreenCircularProgressContent()
            }
            when (showContent) {
                AddLinkConnectorUiState.ShowContent.ScanQrCode -> {
                    if (isCameraPermissionGranted) {
                        ScanQrCode(onQrCodeScanned = onQrCodeScanned)
                    }
                }

                AddLinkConnectorUiState.ShowContent.NameLinkConnector -> {
                    NameNewConnector(
                        connectorDisplayName = connectorDisplayName,
                        onConnectorDisplayNameChanged = onConnectorDisplayNameChanged,
                        isContinueButtonEnabled = isContinueButtonEnabled,
                        onContinueClick = {
                            keyboardController?.hide()
                            onContinueClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanQrCode(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.linkedConnectors_linkNewConnector),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        Text(
            modifier = Modifier.padding(RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.linkedConnectors_newConnection_subtitle),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        CameraPreview(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingDefault
                )
                .imePadding()
                .clip(RadixTheme.shapes.roundedRectMedium),
            disableBackHandler = false,
            isVisible = true,
            onQrCodeDetected = {
                onQrCodeScanned(it)
            }
        )
    }
}

@Composable
private fun NameNewConnector(
    modifier: Modifier = Modifier,
    connectorDisplayName: String,
    onConnectorDisplayNameChanged: (String) -> Unit,
    isContinueButtonEnabled: Boolean,
    onContinueClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    bottom = RadixTheme.dimensions.paddingDefault
                ),
            text = stringResource(id = R.string.linkedConnectors_nameNewConnector_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = RadixTheme.dimensions.paddingLarge,
                    end = RadixTheme.dimensions.paddingLarge,
                    bottom = RadixTheme.dimensions.paddingLarge
                ),
            text = stringResource(id = R.string.linkedConnectors_nameNewConnector_subtitle),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            onValueChanged = onConnectorDisplayNameChanged,
            value = connectorDisplayName,
            hint = stringResource(id = R.string.empty),
            optionalHint = stringResource(id = R.string.linkedConnectors_nameNewConnector_textFieldHint),
            singleLine = true
        )
        Spacer(modifier = Modifier.weight(1f))
        RadixPrimaryButton(
            text = stringResource(id = R.string.linkedConnectors_nameNewConnector_saveLinkButtonTitle),
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(RadixTheme.dimensions.paddingMedium),
            enabled = isContinueButtonEnabled
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NameNewConnectorPreview() {
    RadixWalletTheme {
        AddLinkConnectorContent(
            showContent = AddLinkConnectorUiState.ShowContent.NameLinkConnector,
            isLoading = false,
            isCameraPermissionGranted = true,
            onQrCodeScanned = {},
            onConnectorDisplayNameChanged = {},
            connectorDisplayName = "",
            isContinueButtonEnabled = false,
            onContinueClick = {},
            onCloseClick = {}
        )
    }
}
