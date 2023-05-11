package com.babylon.wallet.android.presentation.transfer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.transaction.composables.StrokeLine
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import kotlinx.coroutines.launch

@Composable
fun TransferScreen(
    modifier: Modifier = Modifier,
    viewModel: TransferViewModel,
    onBackClick: () -> Unit,
    onSendTransferClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TransferContent(
        modifier = modifier,
        onBackClick = onBackClick,
        onSendTransferClick = onSendTransferClick,
        state = state,
        onMessageChanged = viewModel::onMessageChanged,
        onAddressChanged = viewModel::onAddressChanged,
        onAccountSelect = viewModel::onAccountSelect,
        onChooseClick = viewModel::onChooseClick,
        onChooseDestinationAccountClick = viewModel::onChooseDestinationAccountClick,
        addAccountClick = viewModel::addAccountClick,
        deleteAccountClick = viewModel::deleteAccountClick,
        onAddressDecoded = viewModel::onAddressDecoded,
        onQrCodeIconClick = viewModel::onQrCodeIconClick,
        cancelQrScan = viewModel::cancelQrScan,
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TransferContent(
    modifier: Modifier,
    onBackClick: () -> Unit,
    onSendTransferClick: () -> Unit,
    state: TransferViewModel.State,
    onMessageChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onAccountSelect: (Int) -> Unit,
    onChooseClick: (Int) -> Unit,
    onChooseDestinationAccountClick: () -> Unit,
    addAccountClick: () -> Unit,
    deleteAccountClick: (Int) -> Unit,
    onAddressDecoded: (String) -> Unit,
    onQrCodeIconClick: () -> Unit,
    cancelQrScan: () -> Unit,
) {
    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()

    var showMessageContent by remember { mutableStateOf(false) }

    BackHandler(enabled = bottomSheetState.isVisible) {
        scope.launch {
            bottomSheetState.hide()
        }
    }

    DefaultModalSheetLayout(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize(),
        sheetState = bottomSheetState,
        sheetContent = {
            ChooseAccountSheet(
                onCloseClick = {
                    scope.launch {
                        bottomSheetState.hide()
                    }
                },
                address = state.address,
                buttonEnabled = state.buttonEnabled,
                accountsDisabled = state.accountsDisabled,
                chooseAccountSheetMode = state.chooseAccountSheetMode,
                onAddressChanged = onAddressChanged,
                receivingAccounts = state.receivingAccounts,
                onAccountSelect = onAccountSelect,
                onChooseDestinationAccountClick = {
                    scope.launch {
                        bottomSheetState.hide()
                    }
                    onChooseDestinationAccountClick()
                },
                onAddressDecoded = onAddressDecoded,
                onQrCodeIconClick = onQrCodeIconClick,
                cancelQrScan = cancelQrScan
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .background(color = RadixTheme.colors.white)
                .padding(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.Start
        ) {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                contentColor = RadixTheme.colors.gray1,
                backIconType = BackIconType.Close
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault)
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(
                                    vertical = RadixTheme.dimensions.paddingLarge,
                                    horizontal = RadixTheme.dimensions.paddingDefault
                                ),
                            text = stringResource(id = R.string.transfer),
                            style = RadixTheme.typography.title,
                            color = RadixTheme.colors.gray1,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (!showMessageContent) {
                            RadixTextButton(
                                text = stringResource(id = R.string.add_message),
                                onClick = { showMessageContent = true },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(
                                            id = R.drawable.ic_add_message
                                        ),
                                        contentDescription = ""
                                    )
                                }
                            )
                        }
                    }
                }

                if (showMessageContent) {
                    item {
                        TransferMessage(
                            message = state.message,
                            onMessageChanged = onMessageChanged,
                            onMessageClose = { showMessageContent = false }
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    }
                }

                state.fromAccount?.let { account ->
                    item {
                        Text(
                            modifier = Modifier
                                .padding(
                                    horizontal = RadixTheme.dimensions.paddingMedium,
                                    vertical = RadixTheme.dimensions.paddingXSmall
                                ),
                            text = stringResource(id = R.string.from).uppercase(),
                            style = RadixTheme.typography.body1Link,
                            color = RadixTheme.colors.gray2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        SimpleAccountCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        AccountGradientList[account.appearanceID % AccountGradientList.size]
                                    ),
                                    RadixTheme.shapes.roundedRectSmall
                                )
                                .padding(
                                    horizontal = RadixTheme.dimensions.paddingLarge,
                                    vertical = RadixTheme.dimensions.paddingDefault
                                ),
                            account = account
                        )
                    }
                }

                item {
                    StrokeLine(height = 50.dp)
                }

                item {
                    Row {
                        Text(
                            modifier = Modifier
                                .padding(
                                    horizontal = RadixTheme.dimensions.paddingMedium,
                                    vertical = RadixTheme.dimensions.paddingXSmall
                                ),
                            text = stringResource(id = R.string.to).uppercase(),
                            style = RadixTheme.typography.body1Link,
                            color = RadixTheme.colors.gray2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        StrokeLine()
                    }
                }

                itemsIndexed(state.selectedAccounts) { index, selectedAccount ->
                    TargetAccountCard(
                        onChooseAccountClick = {
                            onChooseClick(index)
                            scope.launch {
                                bottomSheetState.show()
                            }
                        },
                        onAddAssetsClick = { /* TODO */ },
                        onDeleteClick = {
                            deleteAccountClick(index)
                        },
                        isDeletable = index > 0,
                        selectedAccount = selectedAccount
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = RadixTheme.dimensions.paddingDefault),
                        horizontalArrangement = Arrangement.End
                    ) {
                        RadixTextButton(
                            text = stringResource(id = R.string.add_account),
                            onClick = addAccountClick,
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_add_account
                                    ),
                                    contentDescription = ""
                                )
                            }
                        )
                    }

                    RadixPrimaryButton(
                        modifier = Modifier
                            .padding(vertical = RadixTheme.dimensions.paddingDefault)
                            .fillMaxWidth(),
                        text = stringResource(id = R.string.send_transfer_request),
                        onClick = onSendTransferClick
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransferContentPreview() {
    RadixWalletTheme {
        TransferContent(
            modifier = Modifier
                .padding(10.dp)
                .background(color = Color.Gray),
            onBackClick = {},
            onSendTransferClick = {},
            state = TransferViewModel.State(
                message = "",
                fromAccount = AccountItemUiModel(
                    "rdx_t_12382918379821",
                    displayName = "Savings account",
                    appearanceID = 1,
                    isSelected = false
                )
            ),
            onMessageChanged = {},
            onAddressChanged = {},
            onAccountSelect = {},
            onChooseClick = {},
            onChooseDestinationAccountClick = {},
            addAccountClick = {},
            deleteAccountClick = {},
            onAddressDecoded = {},
            onQrCodeIconClick = {},
            cancelQrScan = {}
        )
    }
}
