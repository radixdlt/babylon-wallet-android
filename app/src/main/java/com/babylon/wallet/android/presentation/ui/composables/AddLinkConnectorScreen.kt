package com.babylon.wallet.android.presentation.ui.composables

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.linkedconnectors.AddLinkConnectorUiState
import com.babylon.wallet.android.presentation.settings.linkedconnectors.qrcode.CameraPreview
import com.babylon.wallet.android.presentation.ui.composables.linkedconnector.LinkedConnectorMessageScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AddLinkConnectorScreen(
    modifier: Modifier,
    state: AddLinkConnectorUiState,
    onQrCodeScanned: (String) -> Unit,
    onQrCodeScanFailure: (Throwable) -> Unit,
    onConnectorDisplayNameChanged: (String) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit,
    onErrorDismiss: () -> Unit
) {
    BackHandler(onBack = onCloseClick)

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    AddLinkConnectorContent(
        modifier = modifier,
        state = state,
        isCameraPermissionGranted = cameraPermissionState.status.isGranted,
        onQrCodeScanned = onQrCodeScanned,
        onQrCodeScanFailure = onQrCodeScanFailure,
        onConnectorDisplayNameChanged = onConnectorDisplayNameChanged,
        onInfoClick = onInfoClick,
        onContinueClick = onContinueClick,
        onCloseClick = onCloseClick,
        onErrorDismiss = onErrorDismiss
    )
}

@Composable
private fun AddLinkConnectorContent(
    modifier: Modifier = Modifier,
    state: AddLinkConnectorUiState,
    isCameraPermissionGranted: Boolean,
    onQrCodeScanned: (String) -> Unit,
    onQrCodeScanFailure: (Throwable) -> Unit,
    onConnectorDisplayNameChanged: (String) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit,
    onErrorDismiss: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onCloseClick,
                backIconType = BackIconType.Close,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.background,
    ) { padding ->
        val keyboardController = LocalSoftwareKeyboardController.current
        Column(modifier = Modifier.padding(padding)) {
            when (state.content) {
                is AddLinkConnectorUiState.Content.ScanQrCode -> {
                    if (isCameraPermissionGranted) {
                        ScanQrCode(
                            isCameraOn = state.content.isCameraOn,
                            onQrCodeScanned = onQrCodeScanned,
                            onQrCodeScanFailure = onQrCodeScanFailure,
                            onInfoClick = onInfoClick
                        )
                    }
                }

                is AddLinkConnectorUiState.Content.ApproveNewLinkConnector -> {
                    LinkedConnectorMessageScreen(
                        title = stringResource(id = R.string.linkedConnectors_approveNewConnector_title),
                        message = stringResource(id = R.string.linkedConnectors_approveNewConnector_message),
                        isInProgress = state.isAddingNewLinkConnectorInProgress,
                        onPositiveClick = onContinueClick,
                        onNegativeClick = onCloseClick
                    )
                }

                is AddLinkConnectorUiState.Content.UpdateLinkConnector -> {
                    LinkedConnectorMessageScreen(
                        title = stringResource(id = R.string.linkedConnectors_approveExistingConnector_title),
                        message = stringResource(id = R.string.linkedConnectors_approveExistingConnector_message),
                        isInProgress = state.isAddingNewLinkConnectorInProgress,
                        onPositiveClick = onContinueClick,
                        onNegativeClick = onCloseClick
                    )
                }

                is AddLinkConnectorUiState.Content.NameLinkConnector -> {
                    NameNewConnector(
                        content = state.content,
                        isInProgress = state.isAddingNewLinkConnectorInProgress,
                        onConnectorDisplayNameChanged = onConnectorDisplayNameChanged,
                        onContinueClick = {
                            keyboardController?.hide()
                            onContinueClick()
                        }
                    )
                }
            }
        }
    }

    state.error?.let {
        when (it) {
            is AddLinkConnectorUiState.Error.InvalidQR -> ConnectionErrorDialog(
                title = stringResource(id = R.string.linkedConnectors_incorrectQrTitle),
                message = stringResource(id = R.string.linkedConnectors_incorrectQrMessage),
                onDismiss = onErrorDismiss
            )

            is AddLinkConnectorUiState.Error.Other -> ErrorAlertDialog(
                title = stringResource(id = R.string.linkedConnectors_linkFailedErrorTitle),
                errorMessage = it.message,
                cancel = onErrorDismiss,
                cancelMessage = stringResource(R.string.common_dismiss)
            )
        }
    }
}

@Composable
private fun ScanQrCode(
    modifier: Modifier = Modifier,
    isCameraOn: Boolean,
    onQrCodeScanned: (String) -> Unit,
    onQrCodeScanFailure: (Throwable) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    Column(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                .padding(bottom = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.linkedConnectors_newConnection_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.text,
            textAlign = TextAlign.Center
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = stringResource(id = R.string.scanQR_connectorExtension_instructions),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text,
            textAlign = TextAlign.Center
        )

        CameraPreview(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(
                    horizontal = RadixTheme.dimensions.paddingXXLarge,
                    vertical = RadixTheme.dimensions.paddingLarge
                )
                .clip(RadixTheme.shapes.roundedRectMedium),
            disableBackHandler = false,
            isVisible = isCameraOn,
            onQrCodeDetected = onQrCodeScanned,
            onError = onQrCodeScanFailure
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                .padding(bottom = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.scanQR_connectorExtension_disclosureTitle),
            style = RadixTheme.typography.body2Header,
            color = RadixTheme.colors.text
        )

        NumberedValuesList(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                .padding(bottom = RadixTheme.dimensions.paddingDefault),
            values = persistentListOf(
                stringResource(id = R.string.scanQR_connectorExtension_disclosureItem1),
                stringResource(id = R.string.scanQR_connectorExtension_disclosureItem2)
            )
        )

        InfoButton(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge,),
            textStyle = RadixTheme.typography.body2Link,
            text = stringResource(id = R.string.infoLink_title_radixconnect),
            onClick = {
                onInfoClick(GlossaryItem.radixconnect)
            }
        )
    }
}

@Composable
private fun NameNewConnector(
    modifier: Modifier = Modifier,
    content: AddLinkConnectorUiState.Content.NameLinkConnector,
    isInProgress: Boolean,
    onConnectorDisplayNameChanged: (String) -> Unit,
    onContinueClick: () -> Unit
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
            color = RadixTheme.colors.text,
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
            color = RadixTheme.colors.text,
            textAlign = TextAlign.Center
        )
        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            onValueChanged = onConnectorDisplayNameChanged,
            value = content.connectorDisplayName,
            hint = stringResource(id = R.string.empty),
            optionalHint = stringResource(id = R.string.linkedConnectors_nameNewConnector_textFieldHint),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        RadixPrimaryButton(
            text = stringResource(id = R.string.linkedConnectors_nameNewConnector_saveLinkButtonTitle),
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(RadixTheme.dimensions.paddingMedium),
            enabled = content.isContinueButtonEnabled,
            isLoading = isInProgress
        )
    }
}

@Composable
private fun ConnectionErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    BasicPromptAlertDialog(
        finish = {
            onDismiss()
        },
        title = {
            Text(
                text = title,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.text
            )
        },
        message = {
            Text(
                text = message,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.text
            )
        },
        confirmText = stringResource(id = R.string.common_dismiss),
        dismissText = null
    )
}

@Preview(showBackground = true)
@Composable
private fun AddLinkConnectorPreview(
    @PreviewParameter(AddLinkConnectorPreviewProvider::class) state: AddLinkConnectorUiState
) {
    RadixWalletTheme {
        AddLinkConnectorContent(
            state = state,
            isCameraPermissionGranted = true,
            onQrCodeScanned = {},
            onQrCodeScanFailure = {},
            onConnectorDisplayNameChanged = {},
            onInfoClick = {},
            onContinueClick = {},
            onCloseClick = {},
            onErrorDismiss = {}
        )
    }
}

class AddLinkConnectorPreviewProvider : PreviewParameterProvider<AddLinkConnectorUiState> {
    override val values: Sequence<AddLinkConnectorUiState>
        get() = sequenceOf(
            AddLinkConnectorUiState(
                isAddingNewLinkConnectorInProgress = false,
                content = AddLinkConnectorUiState.Content.NameLinkConnector(
                    isContinueButtonEnabled = true,
                    connectorDisplayName = "Test Name"
                ),
                error = null
            ),
            AddLinkConnectorUiState(
                isAddingNewLinkConnectorInProgress = false,
                content = AddLinkConnectorUiState.Content.ApproveNewLinkConnector,
                error = null
            ),
            AddLinkConnectorUiState(
                isAddingNewLinkConnectorInProgress = false,
                content = AddLinkConnectorUiState.Content.UpdateLinkConnector,
                error = null
            ),
            AddLinkConnectorUiState(
                isAddingNewLinkConnectorInProgress = false,
                content = AddLinkConnectorUiState.Content.UpdateLinkConnector,
                error = AddLinkConnectorUiState.Error.Other(
                    message = UiMessage.ErrorMessage(null)
                )
            )
        )
}
