@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)

package com.babylon.wallet.android.presentation.transfer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
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
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.transaction.composables.StrokeLine
import com.babylon.wallet.android.presentation.transfer.TransferViewModel.State
import com.babylon.wallet.android.presentation.transfer.accounts.ChooseAccountSheet
import com.babylon.wallet.android.presentation.transfer.assets.ChooseAssetsSheet
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun TransferScreen(
    modifier: Modifier = Modifier,
    viewModel: TransferViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TransferContent(
        modifier = modifier,
        onBackClick = onBackClick,
        state = state,
        onMessageStateChanged = viewModel::onMessageStateChanged,
        onMessageChanged = viewModel::onMessageChanged,
        onAddressTyped = viewModel::onAddressTyped,
        onOwnedAccountSelected = viewModel::onOwnedAccountSelected,
        onChooseAccountForSkeleton = viewModel::onChooseAccountForSkeleton,
        onChooseAccountSubmitted = viewModel::onChooseAccountSubmitted,
        addAccountClick = viewModel::addAccountClick,
        deleteAccountClick = viewModel::deleteAccountClick,
        onAddressDecoded = viewModel::onQRAddressDecoded,
        onQrCodeIconClick = viewModel::onQrCodeIconClick,
        cancelQrScan = viewModel::cancelQrScan,
        onChooseAssetTabSelected = viewModel::onChooseAssetTabSelected,
        onSheetClosed = viewModel::onSheetClose,
        onAddAssetsClick = viewModel::onAddAssetsClick,
        onRemoveAssetClick = viewModel::onRemoveAsset,
        onAmountTyped = viewModel::onAmountTyped,
        onMaxAmountClicked = viewModel::onMaxAmount,
        onAssetSelectionChanged = viewModel::onAssetSelectionChanged,
        onUiMessageShown = viewModel::onUiMessageShown,
        onChooseAssetsSubmitted = viewModel::onChooseAssetsSubmitted,
        onTransferSubmit = viewModel::onTransferSubmit
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TransferContent(
    modifier: Modifier,
    onBackClick: () -> Unit,
    state: State,
    onMessageStateChanged: (Boolean) -> Unit,
    onMessageChanged: (String) -> Unit,
    onAddressTyped: (String) -> Unit,
    onOwnedAccountSelected: (Network.Account) -> Unit,
    onChooseAccountForSkeleton: (TargetAccount) -> Unit,
    onChooseAccountSubmitted: () -> Unit,
    addAccountClick: () -> Unit,
    deleteAccountClick: (TargetAccount) -> Unit,
    onAddressDecoded: (String) -> Unit,
    onQrCodeIconClick: () -> Unit,
    cancelQrScan: () -> Unit,
    onChooseAssetTabSelected: (State.Sheet.ChooseAssets.Tab) -> Unit,
    onSheetClosed: () -> Unit,
    onAddAssetsClick: (TargetAccount) -> Unit,
    onRemoveAssetClick: (TargetAccount, SpendingAsset) -> Unit,
    onAmountTyped: (TargetAccount, SpendingAsset, String) -> Unit,
    onMaxAmountClicked: (TargetAccount, SpendingAsset) -> Unit,
    onAssetSelectionChanged: (SpendingAsset, Boolean) -> Unit,
    onUiMessageShown: () -> Unit,
    onChooseAssetsSubmitted: () -> Unit,
    onTransferSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)

    SyncSheetState(
        bottomSheetState = bottomSheetState,
        isSheetVisible = state.isSheetVisible,
        onSheetClosed = onSheetClosed
    )

    DefaultModalSheetLayout(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding(),
        sheetState = bottomSheetState,
        sheetContent = {
            when (val sheetState = state.sheet) {
                is State.Sheet.ChooseAccounts -> {
                    ChooseAccountSheet(
                        onCloseClick = onSheetClosed,
                        state = sheetState,
                        onOwnedAccountSelected = onOwnedAccountSelected,
                        onChooseAccountSubmitted = onChooseAccountSubmitted,
                        onAddressDecoded = onAddressDecoded,
                        onQrCodeIconClick = onQrCodeIconClick,
                        cancelQrScan = cancelQrScan,
                        onAddressChanged = onAddressTyped
                    )
                }
                is State.Sheet.ChooseAssets -> {
                    ChooseAssetsSheet(
                        state = sheetState,
                        onTabSelected = { onChooseAssetTabSelected(it) },
                        onCloseClick = onSheetClosed,
                        onAssetSelectionChanged = onAssetSelectionChanged,
                        onUiMessageShown = onUiMessageShown,
                        onChooseAssetsSubmitted = onChooseAssetsSubmitted
                    )
                }
                is State.Sheet.None -> {}
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            focusManager.clearFocus()
                        }
                    )
                }
                .background(color = RadixTheme.colors.white),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        start = RadixTheme.dimensions.paddingMedium,
                        top = RadixTheme.dimensions.paddingMedium
                    ),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = onBackClick
                ) {
                    Icon(
                        painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                        tint = RadixTheme.colors.gray1,
                        contentDescription = "close"
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .padding(bottom = RadixTheme.dimensions.paddingDefault),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(
                                    start = RadixTheme.dimensions.paddingSmall
                                ),
                            text = stringResource(id = R.string.assetTransfer_header_transfer),
                            style = RadixTheme.typography.title,
                            color = RadixTheme.colors.gray1,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (state.messageState is State.Message.None) {
                            RadixTextButton(
                                text = stringResource(id = R.string.assetTransfer_header_addMessageButton),
                                onClick = { onMessageStateChanged(true) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_add_message),
                                        contentDescription = ""
                                    )
                                }
                            )
                        }
                    }
                }

                if (state.messageState is State.Message.Added) {
                    item {
                        TransferMessage(
                            message = state.messageState.message,
                            onMessageChanged = onMessageChanged,
                            onMessageClose = { onMessageStateChanged(false) }
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
                            text = stringResource(
                                id = R.string.assetTransfer_accountList_fromLabel
                            ).uppercase(),
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
                            text = stringResource(
                                id = R.string.assetTransfer_accountList_toLabel
                            ).uppercase(),
                            style = RadixTheme.typography.body1Link,
                            color = RadixTheme.colors.gray2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        StrokeLine(height = 30.dp)
                    }
                }

                items(state.targetAccounts.size) { index ->
                    val targetAccount = state.targetAccounts[index]
                    TargetAccountCard(
                        onChooseAccountClick = {
                            focusManager.clearFocus()
                            onChooseAccountForSkeleton(targetAccount)
                        },
                        onAddAssetsClick = {
                            focusManager.clearFocus()
                            onAddAssetsClick(targetAccount)
                        },
                        onRemoveAssetClicked = { spendingAsset ->
                            onRemoveAssetClick(targetAccount, spendingAsset)
                        },
                        onAmountTyped = { spendingAsset, amount ->
                            onAmountTyped(targetAccount, spendingAsset, amount)
                        },
                        onMaxAmountClicked = { spendingAsset ->
                            focusManager.clearFocus()
                            onMaxAmountClicked(targetAccount, spendingAsset)
                        },
                        onDeleteClick = {
                            deleteAccountClick(targetAccount)
                        },
                        isDeletable = !(targetAccount is TargetAccount.Skeleton && index == 0),
                        targetAccount = targetAccount
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
                            text = stringResource(
                                id = R.string.assetTransfer_accountList_addAccountButton
                            ),
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

                    val isEnabled = remember(state) { state.isSubmitEnabled }

                    RadixPrimaryButton(
                        modifier = Modifier
                            .padding(vertical = RadixTheme.dimensions.paddingDefault)
                            .fillMaxWidth(),
                        text = stringResource(id = R.string.assetTransfer_sendTransferButton),
                        enabled = isEnabled,
                        onClick = onTransferSubmit
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncSheetState(
    bottomSheetState: ModalBottomSheetState,
    isSheetVisible: Boolean,
    onSheetClosed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    BackHandler(enabled = isSheetVisible) {
        onSheetClosed()
    }

    LaunchedEffect(isSheetVisible) {
        if (isSheetVisible) {
            scope.launch { bottomSheetState.show() }
        } else {
            scope.launch { bottomSheetState.hide() }
        }
    }

    LaunchedEffect(bottomSheetState.isVisible) {
        if (!bottomSheetState.isVisible) {
            onSheetClosed()
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
            state = State(
                fromAccount = SampleDataProvider().sampleAccount(
                    address = "rdx_t_12382918379821",
                    name = "Savings account"
                )
            ),
            onMessageStateChanged = {},
            onMessageChanged = {},
            onAddressTyped = {},
            onOwnedAccountSelected = {},
            onChooseAccountForSkeleton = {},
            onChooseAccountSubmitted = {},
            addAccountClick = {},
            deleteAccountClick = {},
            onAddressDecoded = {},
            onQrCodeIconClick = {},
            cancelQrScan = {},
            onChooseAssetTabSelected = {},
            onSheetClosed = {},
            onAddAssetsClick = {},
            onRemoveAssetClick = { _, _ -> },
            onAmountTyped = { _, _, _ -> },
            onMaxAmountClicked = { _, _ -> },
            onAssetSelectionChanged = { _, _ -> },
            onUiMessageShown = {},
            onChooseAssetsSubmitted = {},
            onTransferSubmit = {}
        )
    }
}
