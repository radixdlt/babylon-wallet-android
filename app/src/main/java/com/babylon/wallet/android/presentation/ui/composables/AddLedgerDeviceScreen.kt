package com.babylon.wallet.android.presentation.ui.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.themedColorTint
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.ledgerdevice.AddLedgerDeviceUiState
import com.radixdlt.sargon.LedgerHardwareWalletModel
import rdx.works.core.sargon.displayName

@Composable
fun AddLedgerDeviceScreen(
    modifier: Modifier = Modifier,
    showContent: AddLedgerDeviceUiState.ShowContent,
    uiMessage: UiMessage? = null,
    deviceModel: LedgerHardwareWalletModel?,
    onSendAddLedgerRequestClick: () -> Unit,
    onConfirmLedgerNameClick: (String) -> Unit,
    backIconType: BackIconType = BackIconType.Close,
    onMessageShown: () -> Unit = {},
    onClose: () -> Unit,
    waitingForLedgerResponse: Boolean,
    isAddingLedgerDeviceInProgress: Boolean,
    isAddingNewLinkConnectorInProgress: Boolean = false
) {
    BackHandler(onBack = onClose)

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = com.babylon.wallet.android.R.string.empty),
                onBackClick = onClose,
                backIconType = backIconType,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val imeController = LocalSoftwareKeyboardController.current
                var ledgerNameValue by remember {
                    mutableStateOf("")
                }

                Column(
                    modifier = Modifier
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingDefault,
                            vertical = RadixTheme.dimensions.paddingXXLarge
                        )
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (showContent) {
                        AddLedgerDeviceUiState.ShowContent.AddLedgerDeviceInfo -> {
                            Icon(
                                painterResource(id = R.drawable.ic_hardware_ledger_big),
                                tint = themedColorTint(),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXXLarge))
                            Text(
                                text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_addNewLedger),
                                style = RadixTheme.typography.title,
                                color = RadixTheme.colors.text,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                            Text(
                                text = stringResource(id = com.babylon.wallet.android.R.string.addLedgerDevice_addDevice_body1),
                                style = RadixTheme.typography.body1Regular,
                                color = RadixTheme.colors.text,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                            Text(
                                text = stringResource(id = com.babylon.wallet.android.R.string.addLedgerDevice_addDevice_body2),
                                style = RadixTheme.typography.body1Regular,
                                color = RadixTheme.colors.text,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            RadixPrimaryButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onSendAddLedgerRequestClick,
                                text = stringResource(id = com.babylon.wallet.android.R.string.addLedgerDevice_addDevice_continue),
                                isLoading = isAddingLedgerDeviceInProgress || isAddingNewLinkConnectorInProgress
                            )
                        }

                        AddLedgerDeviceUiState.ShowContent.NameLedgerDevice -> {
                            Text(
                                text = stringResource(id = com.babylon.wallet.android.R.string.addLedgerDevice_nameLedger_title),
                                style = RadixTheme.typography.title,
                                color = RadixTheme.colors.text,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                                text = stringResource(id = com.babylon.wallet.android.R.string.addLedgerDevice_nameLedger_subtitle),
                                style = RadixTheme.typography.body1Regular,
                                color = RadixTheme.colors.text,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            deviceModel?.let { model ->
                                Text(
                                    modifier = Modifier
                                        .padding(vertical = RadixTheme.dimensions.paddingLarge),
                                    text = stringResource(
                                        id = com.babylon.wallet.android.R.string.addLedgerDevice_nameLedger_detectedType,
                                        model.displayName()
                                    ),
                                    style = RadixTheme.typography.body1Header,
                                    color = RadixTheme.colors.textSecondary,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                            RadixTextField(
                                modifier = Modifier.fillMaxWidth(),
                                onValueChanged = { ledgerNameValue = it },
                                value = ledgerNameValue,
                                hint = stringResource(id = com.babylon.wallet.android.R.string.addLedgerDevice_nameLedger_namePlaceholder),
                            )
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                                text = stringResource(id = com.babylon.wallet.android.R.string.addLedgerDevice_nameLedger_fieldHint),
                                style = RadixTheme.typography.body2Regular,
                                color = RadixTheme.colors.textSecondary,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            RadixPrimaryButton(
                                text = stringResource(com.babylon.wallet.android.R.string.addLedgerDevice_nameLedger_continueButtonTitle),
                                enabled = ledgerNameValue.trim().isNotEmpty(),
                                onClick = {
                                    imeController?.hide()
                                    onConfirmLedgerNameClick(ledgerNameValue)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .imePadding()
                            )
                        }
                    }
                }
            }
            if (waitingForLedgerResponse) {
                FullscreenCircularProgressContent()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddLedgerDeviceScreenPreview() {
    RadixWalletTheme {
        AddLedgerDeviceScreen(
            modifier = Modifier.fillMaxSize(),
            showContent = AddLedgerDeviceUiState.ShowContent.AddLedgerDeviceInfo,
            uiMessage = null,
            deviceModel = LedgerHardwareWalletModel.NANO_S,
            onSendAddLedgerRequestClick = {},
            onConfirmLedgerNameClick = { },
            backIconType = BackIconType.Back,
            onMessageShown = {},
            onClose = {},
            waitingForLedgerResponse = false,
            isAddingLedgerDeviceInProgress = false,
            isAddingNewLinkConnectorInProgress = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddLedgerDeviceContentPreview3() {
    RadixWalletTheme {
        AddLedgerDeviceScreen(
            modifier = Modifier.fillMaxSize(),
            showContent = AddLedgerDeviceUiState.ShowContent.NameLedgerDevice,
            uiMessage = null,
            deviceModel = LedgerHardwareWalletModel.NANO_S_PLUS,
            onSendAddLedgerRequestClick = {},
            onConfirmLedgerNameClick = { },
            backIconType = BackIconType.Back,
            onMessageShown = {},
            onClose = {},
            waitingForLedgerResponse = false,
            isAddingLedgerDeviceInProgress = false,
            isAddingNewLinkConnectorInProgress = false
        )
    }
}
