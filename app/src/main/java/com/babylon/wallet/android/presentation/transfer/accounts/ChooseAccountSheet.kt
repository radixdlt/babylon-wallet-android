@file:OptIn(ExperimentalComposeUiApi::class)

package com.babylon.wallet.android.presentation.transfer.accounts

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountSelectionCard
import com.babylon.wallet.android.presentation.settings.connector.qrcode.CameraPreview
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel.State.Sheet.ChooseAccounts
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import rdx.works.profile.data.model.pernetwork.Network

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChooseAccountSheet(
    modifier: Modifier = Modifier,
    state: ChooseAccounts,
    onCloseClick: () -> Unit,
    onAddressChanged: (String) -> Unit,
    onOwnedAccountSelected: (Network.Account) -> Unit,
    onChooseAccountSubmitted: () -> Unit,
    onQrCodeIconClick: () -> Unit,
    onAddressDecoded: (String) -> Unit,
    cancelQrScan: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = RadixTheme.colors.white)
            ) {
                Box(
                    modifier = Modifier
                        .align(alignment = Alignment.TopCenter)
                        .padding(RadixTheme.dimensions.paddingDefault)
                        .size(38.dp, 4.dp)
                        .background(color = RadixTheme.colors.gray4, shape = RadixTheme.shapes.circle)
                )
                IconButton(
                    modifier = Modifier
                        .padding(
                            vertical = RadixTheme.dimensions.paddingLarge
                        )
                        .align(Alignment.CenterStart),
                    onClick = {
                        if (state.mode == ChooseAccounts.Mode.QRScanner) {
                            cancelQrScan()
                        } else {
                            onCloseClick()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (state.mode == ChooseAccounts.Mode.QRScanner) {
                            Icons.Filled.ArrowBack
                        } else {
                            Icons.Filled.Clear
                        },
                        tint = RadixTheme.colors.gray1,
                        contentDescription = "clear"
                    )
                }
                Text(
                    modifier = Modifier
                        .align(alignment = Alignment.Center)
                        .padding(RadixTheme.dimensions.paddingLarge),
                    text = if (state.mode == ChooseAccounts.Mode.Chooser) {
                        stringResource(id = R.string.assetTransfer_chooseReceivingAccount_navigationTitle)
                    } else {
                        stringResource(id = R.string.assetTransfer_chooseReceivingAccount_scanQRNavigationTitle)
                    },
                    style = RadixTheme.typography.body1StandaloneLink,
                    color = RadixTheme.colors.gray1
                )
            }
        },
        bottomBar = {
            if (state.mode == ChooseAccounts.Mode.Chooser) {
                RadixPrimaryButton(
                    modifier = Modifier
                        .padding(RadixTheme.dimensions.paddingDefault)
                        .fillMaxWidth(),
                    text = stringResource(id = R.string.common_choose),
                    onClick = onChooseAccountSubmitted,
                    enabled = state.isChooseButtonEnabled
                )
            }
        }
    ) { padding ->
        when (state.mode) {
            ChooseAccounts.Mode.Chooser -> {
                ChooseAccountContent(
                    modifier = Modifier
                        .background(color = RadixTheme.colors.white)
                        .padding(padding),
                    onAddressChanged = onAddressChanged,
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
                            .background(color = RadixTheme.colors.white)
                            .padding(padding),
                        onAddressDecoded = onAddressDecoded
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
    focusManager: FocusManager,
    cameraPermissionState: PermissionState,
    state: ChooseAccounts,
    onAddressChanged: (String) -> Unit,
    onQrCodeIconClick: () -> Unit,
    onOwnedAccountSelected: (Network.Account) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.assetTransfer_chooseReceivingAccount_enterManually),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
        }

        item {
            val (typedAddress, errorResource) = remember(state.selectedAccount) {
                if (state.selectedAccount is TargetAccount.Other) {
                    val address = state.selectedAccount.address

                    // Do not show the error when the field has an empty value
                    if (address.isBlank()) {
                        return@remember "" to null
                    }

                    val errorResource = when (state.selectedAccount.validity) {
                        TargetAccount.Other.AddressValidity.VALID -> null
                        TargetAccount.Other.AddressValidity.INVALID -> R.string.invalid_address
                        TargetAccount.Other.AddressValidity.USED -> R.string.used_address
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
                onValueChanged = onAddressChanged,
                value = typedAddress,
                hint = stringResource(id = R.string.assetTransfer_chooseReceivingAccount_addressFieldPlaceholder),
                hintColor = RadixTheme.colors.gray2,
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
                                    onAddressChanged("")
                                    focusManager.clearFocus()
                                }
                            ) {
                                Icon(
                                    painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                                    contentDescription = "clear"
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
                                contentDescription = ""
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RadixTheme.colors.gray1,
                    unfocusedBorderColor = RadixTheme.colors.gray1,
                    focusedContainerColor = RadixTheme.colors.gray5,
                    unfocusedContainerColor = RadixTheme.colors.gray5
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                )
            )
            Divider(
                Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                1.dp,
                RadixTheme.colors.gray4
            )
        }

        item {
            Text(
                modifier = Modifier
                    .padding(RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.assetTransfer_chooseReceivingAccount_chooseOwnAccount),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
        }

        items(state.ownedAccounts.size) { index ->
            val accountItem = state.ownedAccounts[index]

            val gradientColor = getAccountGradientColorsFor(accountItem.appearanceID)
            AccountSelectionCard(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                    .background(
                        brush = Brush.horizontalGradient(gradientColor),
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
                accountName = accountItem.displayName,
                address = accountItem.address,
                checked = state.isOwnedAccountSelected(account = accountItem),
                isSingleChoice = true,
                radioButtonClicked = {
                    if (state.isOwnedAccountsEnabled) {
                        onOwnedAccountSelected(accountItem)
                        focusManager.clearFocus(true)
                    }
                }
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }
    }
}

@Composable
fun ScanQRContent(
    modifier: Modifier = Modifier,
    onAddressDecoded: (String) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .padding(vertical = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.assetTransfer_qrScanInstructions),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1
        )

        CameraPreview(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                .clip(RadixTheme.shapes.roundedRectMedium)
        ) {
            onAddressDecoded(it)
        }
    }
}
