package com.babylon.wallet.android.presentation.transfer.accounts

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountSelectionCard
import com.babylon.wallet.android.presentation.settings.linkedconnectors.qrcode.CameraPreview
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel.State.Sheet.ChooseAccounts
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogHeader
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.radixdlt.sargon.Account

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChooseAccountSheet(
    modifier: Modifier = Modifier,
    state: ChooseAccounts,
    onCloseClick: () -> Unit,
    onReceiverChanged: (String) -> Unit,
    onOwnedAccountSelected: (Account) -> Unit,
    onChooseAccountSubmitted: () -> Unit,
    onQrCodeIconClick: () -> Unit,
    onQRDecoded: (String) -> Unit,
    cancelQrScan: () -> Unit,
    onErrorMessageShown: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val focusManager = LocalFocusManager.current

    if (state.uiMessage != null) {
        ErrorAlertDialog(
            cancel = onErrorMessageShown,
            errorMessage = state.uiMessage
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            BottomDialogHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = RadixTheme.colors.background,
                        shape = RadixTheme.shapes.roundedRectTopDefault
                    ),
                title = if (state.mode == ChooseAccounts.Mode.Chooser) {
                    stringResource(id = R.string.assetTransfer_chooseReceivingAccount_navigationTitle)
                } else {
                    stringResource(id = R.string.assetTransfer_chooseReceivingAccount_scanQRNavigationTitle)
                },
                backIcon = if (state.mode == ChooseAccounts.Mode.QRScanner) {
                    Icons.AutoMirrored.Filled.ArrowBack
                } else {
                    Icons.Filled.Clear
                },
                onDismissRequest = {
                    if (state.mode == ChooseAccounts.Mode.QRScanner) {
                        cancelQrScan()
                    } else {
                        onCloseClick()
                    }
                }
            )
        },
        bottomBar = {
            if (state.mode == ChooseAccounts.Mode.Chooser) {
                RadixBottomBar(
                    onClick = {
                        focusManager.clearFocus()
                        onChooseAccountSubmitted()
                    },
                    text = stringResource(id = R.string.common_choose),
                    enabled = state.isChooseButtonEnabled,
                    isLoading = state.isResolving,
                    insets = WindowInsets.navigationBars.union(WindowInsets.ime)
                )
            }
        }
    ) { padding ->
        when (state.mode) {
            ChooseAccounts.Mode.Chooser -> {
                ChooseAccountContent(
                    modifier = Modifier
                        .background(color = RadixTheme.colors.background),
                    contentPadding = padding,
                    onReceiverChanged = onReceiverChanged,
                    state = state,
                    cameraPermissionState = cameraPermissionState,
                    onQrCodeIconClick = onQrCodeIconClick,
                    onOwnedAccountSelected = onOwnedAccountSelected,
                    focusManager = focusManager
                )
            }
            ChooseAccounts.Mode.QRScanner -> {
                if (cameraPermissionState.status.isGranted) {
                    ScanQRContent(
                        modifier = Modifier
                            .background(color = RadixTheme.colors.background)
                            .padding(padding),
                        onQRDecoded = onQRDecoded
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ChooseAccountContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    focusManager: FocusManager,
    cameraPermissionState: PermissionState,
    state: ChooseAccounts,
    onReceiverChanged: (String) -> Unit,
    onQrCodeIconClick: () -> Unit,
    onOwnedAccountSelected: (Account) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = contentPadding
    ) {
        item {
            Text(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.assetTransfer_chooseReceivingAccount_enterManually),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text
            )
        }

        item {
            val (typedAddress, errorResource) = remember(state.selectedAccount) {
                if (state.selectedAccount is TargetAccount.Other) {
                    val address = state.selectedAccount.typed

                    // Do not show the error when the field has an empty value
                    if (address.isBlank()) {
                        return@remember "" to null
                    }

                    val errorResource = when (state.selectedAccount.validity) {
                        TargetAccount.Other.InputValidity.VALID -> null
                        TargetAccount.Other.InputValidity.INVALID ->
                            R.string.assetTransfer_chooseReceivingAccount_invalidAddressError
                        TargetAccount.Other.InputValidity.ADDRESS_USED ->
                            R.string.assetTransfer_chooseReceivingAccount_alreadyAddedError
                    }

                    address to errorResource
                } else {
                    "" to null
                }
            }

            val focusRequester = remember { FocusRequester() }
            var isFocused by remember { mutableStateOf(false) }
            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused
                    }
                    .padding(RadixTheme.dimensions.paddingDefault),
                onValueChanged = onReceiverChanged,
                value = typedAddress,
                hint = stringResource(id = R.string.assetTransfer_chooseReceivingAccount_addressFieldPlaceholder),
                error = if (!isFocused) {
                    errorResource?.let { stringResource(id = it) }
                } else {
                    null
                },
                singleLine = true,
                trailingIcon = {
                    Row(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall)
                    ) {
                        if (typedAddress.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onReceiverChanged("")
                                    focusManager.clearFocus()
                                }
                            ) {
                                Icon(
                                    painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                                    contentDescription = "clear",
                                    tint = RadixTheme.colors.icon
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                cameraPermissionState.launchPermissionRequest()
                                onQrCodeIconClick()
                            }
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = com.babylon.wallet.android.designsystem.R.drawable.ic_qr_code_scanner
                                ),
                                contentDescription = "",
                                tint = RadixTheme.colors.icon
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    platformImeOptions = PlatformImeOptions()
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                )
            )
            HorizontalDivider(
                Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                1.dp,
                RadixTheme.colors.divider
            )
        }

        if (state.ownedAccounts.isNotEmpty()) {
            item {
                Text(
                    modifier = Modifier
                        .padding(RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.assetTransfer_chooseReceivingAccount_chooseOwnAccount),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.text
                )
            }
        }

        items(state.ownedAccounts.size) { index ->
            val accountItem = state.ownedAccounts[index]

            AccountSelectionCard(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                    .background(
                        brush = accountItem.appearanceId.gradient(),
                        shape = RadixTheme.shapes.roundedRectSmall,
                        alpha = if (state.isOwnedAccountsEnabled) 1f else 0.5f
                    )
                    .clip(RadixTheme.shapes.roundedRectSmall)
                    .clickable {
                        if (state.isOwnedAccountsEnabled) {
                            onOwnedAccountSelected(accountItem)
                            focusManager.clearFocus(true)
                        }
                    },
                accountName = accountItem.displayName.value,
                address = accountItem.address,
                checked = state.isOwnedAccountSelected(account = accountItem),
                isSingleChoice = true,
                radioButtonClicked = {
                    if (state.isOwnedAccountsEnabled) {
                        onOwnedAccountSelected(accountItem)
                        focusManager.clearFocus(true)
                    }
                },
                isEnabledForSelection = state.isResolving.not()
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }
    }
}

@Composable
fun ScanQRContent(
    modifier: Modifier = Modifier,
    onQRDecoded: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(RadixTheme.dimensions.paddingXXLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Text(
            text = stringResource(id = R.string.scanQR_account_instructions),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text,
            textAlign = TextAlign.Center
        )

        CameraPreview(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RadixTheme.shapes.roundedRectMedium),
            disableBackHandler = false
        ) {
            onQRDecoded(it)
        }
    }
}
