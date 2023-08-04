package com.babylon.wallet.android.presentation.ui.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.settings.ledgerhardwarewallets.AddLedgerDeviceUiState

@Composable
fun AddLedgerDeviceScreen(
    modifier: Modifier,
    showContent: AddLedgerDeviceUiState.ShowContent,
    deviceModel: String?,
    onSendAddLedgerRequestClick: () -> Unit,
    onConfirmLedgerNameClick: (String) -> Unit,
    backIconType: BackIconType = BackIconType.Close,
    onClose: () -> Unit,
    waitingForLedgerResponse: Boolean,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RadixTheme.colors.defaultBackground)
        ) {
            var ledgerNameValue by remember {
                mutableStateOf("")
            }
            RadixCenteredTopAppBar(
                title = stringResource(id = com.babylon.wallet.android.R.string.empty),
                onBackClick = onClose,
                contentColor = RadixTheme.colors.gray1,
                modifier = Modifier.background(RadixTheme.colors.defaultBackground),
                backIconType = backIconType
            )
            Column(
                modifier = Modifier
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingXLarge
                    )
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (showContent) {
                    AddLedgerDeviceUiState.ShowContent.AddLedgerDeviceInfo -> {
                        Icon(
                            painterResource(id = R.drawable.ic_hardware_ledger_big),
                            tint = Color.Unspecified,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
                        Text(
                            text = stringResource(id = com.babylon.wallet.android.R.string.ledgerHardwareDevices_addNewLedger),
                            style = RadixTheme.typography.title,
                            color = RadixTheme.colors.gray1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                        Text(
                            text = stringResource(id = com.babylon.wallet.android.R.string.addLedgerDevice_addDevice_body1),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                        Text(
                            text = stringResource(id = com.babylon.wallet.android.R.string.addLedgerDevice_addDevice_body2),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RadixPrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onSendAddLedgerRequestClick,
                            text = stringResource(id = com.babylon.wallet.android.R.string.addLedgerDevice_addDevice_continue)
                        )
                    }

                    AddLedgerDeviceUiState.ShowContent.NameLedgerDevice -> {
                        Text(
                            text = stringResource(id = com.babylon.wallet.android.R.string.addLedgerDevice_nameLedger_title),
                            style = RadixTheme.typography.title,
                            color = RadixTheme.colors.gray1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            modifier = Modifier
                                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                            text = stringResource(id = com.babylon.wallet.android.R.string.addLedgerDevice_nameLedger_subtitle),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.gray1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        deviceModel?.let { model ->
                            Text(
                                modifier = Modifier
                                    .padding(vertical = RadixTheme.dimensions.paddingLarge),
                                text = stringResource(
                                    id = com.babylon.wallet.android.R.string.addLedgerDevice_nameLedger_detectedType,
                                    model
                                ),
                                style = RadixTheme.typography.body1Header,
                                color = RadixTheme.colors.gray2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                        RadixTextField(
                            modifier = Modifier.fillMaxWidth(),
                            onValueChanged = { ledgerNameValue = it },
                            value = ledgerNameValue,
                            hint = stringResource(id = com.babylon.wallet.android.R.string.addLedgerDevice_nameLedger_namePlaceholder),
                            hintColor = RadixTheme.colors.gray2
                        )
                        Text(
                            modifier = Modifier
                                .padding(horizontal = RadixTheme.dimensions.paddingSmall),
                            text = stringResource(id = com.babylon.wallet.android.R.string.addLedgerDevice_nameLedger_fieldHint),
                            style = RadixTheme.typography.body2Regular,
                            color = RadixTheme.colors.gray2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RadixPrimaryButton(
                            text = stringResource(com.babylon.wallet.android.R.string.addLedgerDevice_nameLedger_continueButtonTitle),
                            enabled = ledgerNameValue.trim().isNotEmpty(),
                            onClick = {
                                onConfirmLedgerNameClick(ledgerNameValue)
                                ledgerNameValue = ""
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

@Preview(showBackground = true)
@Composable
fun AddLedgerDeviceScreenPreview() {
    RadixWalletTheme {
        AddLedgerDeviceScreen(
            modifier = Modifier.fillMaxSize(),
            showContent = AddLedgerDeviceUiState.ShowContent.AddLedgerDeviceInfo,
            deviceModel = "device model",
            onSendAddLedgerRequestClick = {},
            onConfirmLedgerNameClick = { },
            backIconType = BackIconType.Back,
            onClose = {},
            waitingForLedgerResponse = false,
            onBackClick = {}
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
            deviceModel = "device model",
            onSendAddLedgerRequestClick = {},
            onConfirmLedgerNameClick = { },
            backIconType = BackIconType.Back,
            onClose = {},
            waitingForLedgerResponse = false,
            onBackClick = {}
        )
    }
}
