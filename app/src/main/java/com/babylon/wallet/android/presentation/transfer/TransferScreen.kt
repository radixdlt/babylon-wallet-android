package com.babylon.wallet.android.presentation.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.plus
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.transaction.composables.StrokeLine
import com.babylon.wallet.android.presentation.transfer.TransferViewModel.State
import com.babylon.wallet.android.presentation.transfer.accounts.ChooseAccountSheet
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.transfer.assets.ChooseAssetsSheet
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.resources.Resource

@Composable
fun TransferScreen(
    modifier: Modifier = Modifier,
    viewModel: TransferViewModel,
    onBackClick: () -> Unit,
    onShowAssetDetails: (SpendingAsset, Account) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is TransferViewModel.Event.ShowAssetDetails -> onShowAssetDetails(event.asset, event.fromAccount)
            }
        }
    }

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
        onChooseAssetTabClick = viewModel::onChooseAssetTabSelected,
        onChooseAssetCollectionClick = viewModel::onChooseAssetCollectionToggle,
        onSheetClosed = viewModel::onSheetClose,
        onAddAssetsClick = viewModel::onAddAssetsClick,
        onRemoveAssetClick = viewModel::onRemoveAsset,
        onAmountTyped = viewModel::onAmountTyped,
        onMaxAmountClicked = viewModel::onMaxAmount,
        onMaxAmountApplied = viewModel::onMaxAmountApplied,
        onLessThanFeeApplied = viewModel::onLessThanFeeApplied,
        onAssetSelectionChanged = viewModel::onAssetSelectionChanged,
        onAssetClicked = viewModel::onAssetClicked,
        onUiMessageShown = viewModel::onUiMessageShown,
        onChooseAssetsSubmitted = viewModel::onChooseAssetsSubmitted,
        onNextNFTsPageRequest = viewModel::onNextNFTsPageRequest,
        onStakesRequest = viewModel::onStakesRequest,
        onTransferSubmit = viewModel::onTransferSubmit,
        onInfoClick = onInfoClick
    )
}

@Composable
fun TransferContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    state: State,
    onMessageStateChanged: (Boolean) -> Unit,
    onMessageChanged: (String) -> Unit,
    onAddressTyped: (String) -> Unit,
    onOwnedAccountSelected: (Account) -> Unit,
    onChooseAccountForSkeleton: (TargetAccount) -> Unit,
    onChooseAccountSubmitted: () -> Unit,
    addAccountClick: () -> Unit,
    deleteAccountClick: (TargetAccount) -> Unit,
    onAddressDecoded: (String) -> Unit,
    onQrCodeIconClick: () -> Unit,
    cancelQrScan: () -> Unit,
    onChooseAssetTabClick: (AssetsTab) -> Unit,
    onChooseAssetCollectionClick: (String) -> Unit,
    onSheetClosed: () -> Unit,
    onAddAssetsClick: (TargetAccount) -> Unit,
    onRemoveAssetClick: (TargetAccount, SpendingAsset) -> Unit,
    onAmountTyped: (TargetAccount, SpendingAsset, String) -> Unit,
    onMaxAmountClicked: (TargetAccount, SpendingAsset) -> Unit,
    onMaxAmountApplied: (Boolean) -> Unit,
    onLessThanFeeApplied: (Boolean) -> Unit,
    onAssetSelectionChanged: (SpendingAsset, Boolean) -> Unit,
    onAssetClicked: (SpendingAsset) -> Unit,
    onNextNFTsPageRequest: (Resource.NonFungibleResource) -> Unit,
    onStakesRequest: () -> Unit,
    onUiMessageShown: () -> Unit,
    onChooseAssetsSubmitted: () -> Unit,
    onTransferSubmit: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val focusManager = LocalFocusManager.current
    state.maxXrdError?.let { error ->
        if (error.maxAccountAmountLessThanFee) {
            BasicPromptAlertDialog(
                finish = onLessThanFeeApplied,
                titleText = stringResource(id = R.string.assetTransfer_maxAmountDialog_title),
                messageText = "Sending the full amount of XRD in this account will require you to pay the transaction fee " +
                    "from a different account.", // TODO R.string.assetTransfer_maxAmountDialog_body2),
                confirmText = stringResource(id = R.string.common_ok),
                dismissText = stringResource(id = R.string.common_cancel)
            )
        } else {
            BasicPromptAlertDialog(
                finish = onMaxAmountApplied,
                titleText = stringResource(id = R.string.assetTransfer_maxAmountDialog_title),
                messageText = stringResource(id = R.string.assetTransfer_maxAmountDialog_body),
                confirmText = stringResource(
                    id = R.string.assetTransfer_maxAmountDialog_sendAllButton,
                    error.maxAccountAmount.formatted()
                ),
                dismissText = stringResource(
                    id = R.string.assetTransfer_maxAmountDialog_saveXrdForFeeButton,
                    error.amountWithoutFees.formatted()
                )
            )
        }
    }

    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                backIconType = BackIconType.Close,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
            contentPadding = padding + PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault),
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
                                vertical = RadixTheme.dimensions.paddingXXSmall
                            ),
                        text = stringResource(
                            id = R.string.assetTransfer_accountList_fromLabel
                        ).uppercase(),
                        style = RadixTheme.typography.body1Link,
                        color = RadixTheme.colors.gray2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    SimpleAccountCard(
                        modifier = Modifier.fillMaxWidth(),
                        account = account
                    )
                }
            }

            item {
                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    StrokeLine(modifier = Modifier.padding(end = RadixTheme.dimensions.paddingLarge), height = 50.dp)
                }
            }

            item {
                Row {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(
                                horizontal = RadixTheme.dimensions.paddingMedium,
                                vertical = RadixTheme.dimensions.paddingXXSmall
                            ),
                        text = stringResource(
                            id = R.string.assetTransfer_accountList_toLabel
                        ).uppercase(),
                        style = RadixTheme.typography.body1Link,
                        color = RadixTheme.colors.gray2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    StrokeLine(modifier = Modifier.padding(end = RadixTheme.dimensions.paddingLarge), height = 30.dp)
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
                    onAssetClick = { asset ->
                        onAssetClicked(asset)
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
                    text = stringResource(id = R.string.assetTransfer_sendTransferButton),
                    onClick = onTransferSubmit,
                    modifier = Modifier
                        .padding(vertical = RadixTheme.dimensions.paddingDefault)
                        .fillMaxWidth(),
                    enabled = isEnabled,
                    isLoading = state.isLoadingAccountDepositResourceRules
                )
            }
        }
    }

    if (state.isSheetVisible) {
        // here we use a BottomSheetDialogWrapper instead of ModalBottomSheetLayout because TextField
        // inside ModalBottomSheetLayout is not properly displaying contextual text editing options like copy/paste.
        // See here https://issuetracker.google.com/issues/327085717
        BottomSheetDialogWrapper(
            addScrim = true,
            showDragHandle = true,
            onDismiss = onSheetClosed,
            showDefaultTopBar = false
        ) {
            when (val sheetState = state.sheet) {
                is State.Sheet.ChooseAccounts -> {
                    ChooseAccountSheet(
                        modifier = Modifier
                            .background(RadixTheme.colors.gray5)
                            .fillMaxHeight(0.9f)
                            .imePadding(),
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
                        modifier = Modifier
                            .background(RadixTheme.colors.defaultBackground)
                            .fillMaxHeight(0.9f)
                            .imePadding(),
                        state = sheetState,
                        onTabClick = onChooseAssetTabClick,
                        onCollectionClick = onChooseAssetCollectionClick,
                        onCloseClick = onSheetClosed,
                        onAssetSelectionChanged = onAssetSelectionChanged,
                        onNextNFtsPageRequest = onNextNFTsPageRequest,
                        onStakesRequest = onStakesRequest,
                        onUiMessageShown = onUiMessageShown,
                        onChooseAssetsSubmitted = onChooseAssetsSubmitted,
                        onInfoClick = onInfoClick
                    )
                }

                is State.Sheet.None -> {}
            }
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun TransferContentPreview() {
    RadixWalletTheme {
        TransferContent(
            modifier = Modifier
                .padding(10.dp)
                .background(color = Color.Gray),
            onBackClick = {},
            state = State(fromAccount = Account.sampleMainnet()),
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
            onChooseAssetTabClick = {},
            onChooseAssetCollectionClick = {},
            onSheetClosed = {},
            onAddAssetsClick = {},
            onRemoveAssetClick = { _, _ -> },
            onAmountTyped = { _, _, _ -> },
            onMaxAmountClicked = { _, _ -> },
            onMaxAmountApplied = {},
            onLessThanFeeApplied = {},
            onAssetSelectionChanged = { _, _ -> },
            onAssetClicked = {},
            onUiMessageShown = {},
            onChooseAssetsSubmitted = {},
            onNextNFTsPageRequest = {},
            onStakesRequest = {},
            onTransferSubmit = {},
            onInfoClick = {}
        )
    }
}
