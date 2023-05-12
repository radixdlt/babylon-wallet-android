package com.babylon.wallet.android.presentation.transfer

import android.Manifest
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountSelectionCard
import com.babylon.wallet.android.presentation.settings.connector.qrcode.CameraPreview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun ChooseAccountSheet(
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit,
    address: String,
    buttonEnabled: Boolean,
    accountsDisabled: Boolean,
    chooseAccountSheetMode: ChooseAccountSheetMode,
    onAddressChanged: (String) -> Unit,
    receivingAccounts: ImmutableList<AccountItemUiModel>,
    onAccountSelect: (Int) -> Unit,
    onChooseDestinationAccountClick: () -> Unit,
    onAddressDecoded: (String) -> Unit,
    onQrCodeIconClick: () -> Unit,
    cancelQrScan: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    LazyColumn(
        modifier = modifier
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        stickyHeader {
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
                        if (chooseAccountSheetMode == ChooseAccountSheetMode.ScanQr) {
                            cancelQrScan()
                        } else {
                            onCloseClick()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (chooseAccountSheetMode == ChooseAccountSheetMode.ScanQr) {
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
                    text = if (chooseAccountSheetMode == ChooseAccountSheetMode.Default) {
                        stringResource(id = R.string.choose_receiving_account)
                    } else {
                        stringResource(id = R.string.scan_qr_code)
                    },
                    style = RadixTheme.typography.body1StandaloneLink,
                    color = RadixTheme.colors.gray1
                )
            }
        }

        when (chooseAccountSheetMode) {
            ChooseAccountSheetMode.Default -> {
                item {
                    Text(
                        modifier = Modifier
                            .padding(vertical = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.enter_account_address_manually),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1
                    )
                }

                item {
                    RadixTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingDefault),
                        onValueChanged = onAddressChanged,
                        value = address,
                        hint = stringResource(id = R.string.enter_or_paste_address),
                        hintColor = RadixTheme.colors.gray2,
                        singleLine = true,
                        trailingIcon = {
                            Row(
                                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall)
                            ) {
                                if (address.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onAddressChanged("") }
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
                        text = stringResource(id = R.string.or_choose_one_of_your_accounts),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.gray1
                    )
                }

                itemsIndexed(receivingAccounts) { index, accountItem ->
                    val gradientColor = getAccountGradientColorsFor(accountItem.appearanceID)
                    AccountSelectionCard(
                        modifier = Modifier
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                            .background(
                                brush = Brush.horizontalGradient(gradientColor),
                                shape = RadixTheme.shapes.roundedRectSmall,
                                alpha = if (accountsDisabled) 0.5f else 1f
                            )
                            .clip(RadixTheme.shapes.roundedRectSmall)
                            .clickable {
                                onAccountSelect(index)
                            },
                        accountName = accountItem.displayName.orEmpty(),
                        address = accountItem.address,
                        checked = accountItem.isSelected,
                        isSingleChoice = true,
                        radioButtonClicked = {
                            onAccountSelect(index)
                        }
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                }

                item {
                    RadixPrimaryButton(
                        modifier = Modifier
                            .padding(RadixTheme.dimensions.paddingDefault)
                            .fillMaxWidth(),
                        text = stringResource(id = R.string.choose),
                        onClick = onChooseDestinationAccountClick,
                        enabled = buttonEnabled
                    )
                }
            }
            ChooseAccountSheetMode.ScanQr -> {
                if (cameraPermissionState.status.isGranted) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(vertical = RadixTheme.dimensions.paddingDefault),
                                text = stringResource(id = R.string.scan_qr_code_of_radix_account_address),
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
                }
            }
        }
    }
}
