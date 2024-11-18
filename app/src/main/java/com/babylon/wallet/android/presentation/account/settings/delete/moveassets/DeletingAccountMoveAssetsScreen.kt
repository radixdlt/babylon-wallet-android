@file:JvmName("DeletingAccountMoveAssetsViewModelKt")

package com.babylon.wallet.android.presentation.account.settings.delete.moveassets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountSelectionCard
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.AccountMainnetSample
import com.radixdlt.sargon.samples.sampleMainnet

@Composable
fun DeletingAccountMoveAssetsScreen(
    modifier: Modifier = Modifier,
    viewModel: DeletingAccountMoveAssetsViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DeletingAccountMoveAssetsContent(
        modifier = modifier,
        state = state,
        onSkipRequested = viewModel::onSkipRequested,
        onSkipCancel = viewModel::onSkipCancelled,
        onSkipConfirm = viewModel::onSkipConfirmed,
        onAccountSelected = viewModel::onAccountSelected,
        onMessageShown = viewModel::onMessageShown,
        onSubmit = viewModel::onSubmit,
        onDismiss = onDismiss
    )
}

@Composable
private fun DeletingAccountMoveAssetsContent(
    modifier: Modifier = Modifier,
    state: DeletingAccountMoveAssetsViewModel.State,
    onSkipRequested: () -> Unit,
    onSkipConfirm: () -> Unit,
    onSkipCancel: () -> Unit,
    onAccountSelected: (Account) -> Unit,
    onMessageShown: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    when (state.warning) {
        DeletingAccountMoveAssetsViewModel.State.Warning.SkipMovingAssets -> SkipMoveAssetsDialog(
            onCancelClick = onSkipCancel,
            onContinueClick = onSkipConfirm
        )
        DeletingAccountMoveAssetsViewModel.State.Warning.CannotDeleteAccount -> CannotDeleteAccountDialog(
            onCancelClick = onDismiss
        )
        DeletingAccountMoveAssetsViewModel.State.Warning.CannotTransferSomeAssets -> CannotTransferSomeAssetsDialog(
            onCancelClick = onSkipCancel,
            onContinueClick = onSkipConfirm
        )
        null -> {}
    }

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = "",
                windowInsets = WindowInsets.statusBarsAndBanner,
                onBackClick = onDismiss,
                backIconType = BackIconType.Back
            )
        },
        bottomBar = {
            RadixBottomBar(
                additionalContent = {
                    RadixPrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(R.string.common_continue),
                        isLoading = state.isContinueLoading,
                        enabled = state.selectedAccount != null,
                        onClick = onSubmit
                    )
                },
                button = {
                    RadixTextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = RadixTheme.dimensions.paddingSmall)
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.accountSettings_deleteAccount_skipButton),
                        enabled = !state.isContinueLoading,
                        onClick = onSkipRequested
                    )
                },
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        val accounts = remember(state) { state.accounts() }

        LazyColumn(
            modifier = Modifier.padding(padding)
        ) {
            item {
                Text(
                    modifier = Modifier
                        .padding(top = RadixTheme.dimensions.paddingSemiLarge)
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                        .fillMaxWidth(),
                    text = stringResource(id = R.string.accountSettings_moveAssets_title),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1
                )
            }

            item {
                Text(
                    modifier = Modifier
                        .padding(top = RadixTheme.dimensions.paddingLarge)
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                        .fillMaxWidth(),
                    text = stringResource(id = R.string.accountSettings_moveAssets_message),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1
                )
            }

            item {
                Text(
                    modifier = Modifier
                        .padding(top = RadixTheme.dimensions.paddingLarge)
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                        .fillMaxWidth(),
                    text = stringResource(id = R.string.accountSettings_deleteAccount_note),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1
                )
            }

            item {
                val isWarningVisible = remember(state) { state.isNotAnyAccountsWithEnoughXRDWarningVisible() }

                if (isWarningVisible) {
                    WarningText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingLarge),
                        text = AnnotatedString(text = stringResource(id = R.string.accountSettings_deleteAccount_noAccountsWarning))
                    )
                } else {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
                }
            }

            item {
                if (state.isAccountsLoading) {
                    Box(
                        modifier = Modifier
                            .height(140.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = RadixTheme.colors.gray1)
                    }
                }
            }

            items(accounts) { account ->
                val (isSelected, isEnabled) = remember(state, account) {
                    state.isAccountSelected(account) to state.isAccountEnabled(account)
                }

                AccountSelectionCard(
                    modifier = Modifier
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingLarge,
                            vertical = RadixTheme.dimensions.paddingXSmall
                        )
                        .background(
                            account.appearanceId.gradient(alpha = if (isEnabled) 1f else 0.3f),
                            shape = RadixTheme.shapes.roundedRectSmall
                        )
                        .clickable(enabled = isEnabled) {
                            onAccountSelected(account)
                        },
                    accountName = account.displayName.value,
                    address = account.address,
                    checked = isSelected,
                    isSingleChoice = true,
                    isEnabledForSelection = isEnabled,
                    radioButtonClicked = { onAccountSelected(account) }
                )
            }
        }
    }
}

@Composable
private fun SkipMoveAssetsDialog(
    modifier: Modifier = Modifier,
    onCancelClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    BasicPromptAlertDialog(
        modifier = modifier,
        finish = { accepted ->
            if (accepted) {
                onContinueClick()
            } else {
                onCancelClick()
            }
        },
        title = {
            Text(
                text = stringResource(id = R.string.accountSettings_assetsWillBeLostWarning_title),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
        },
        message = {
            Text(
                text = stringResource(id = R.string.accountSettings_assetsWillBeLostWarning_message),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1
            )
        },
        confirmText = stringResource(id = R.string.common_continue),
        dismissText = stringResource(id = R.string.common_cancel),
        confirmTextColor = RadixTheme.colors.red1
    )
}

@Composable
private fun CannotDeleteAccountDialog(
    modifier: Modifier = Modifier,
    onCancelClick: () -> Unit,
) {
    BasicPromptAlertDialog(
        modifier = modifier,
        finish = { accepted ->
            if (accepted) {
                onCancelClick()
            }
        },
        title = {
            Text(
                text = stringResource(id = R.string.accountSettings_cannotDeleteAccountWarning_title),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
        },
        message = {
            Text(
                text = stringResource(id = R.string.accountSettings_cannotDeleteAccountWarning_message),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1
            )
        },
        confirmText = stringResource(id = R.string.common_cancel),
        dismissText = null,
        confirmTextColor = RadixTheme.colors.red1
    )
}

@Composable
private fun CannotTransferSomeAssetsDialog(
    modifier: Modifier = Modifier,
    onCancelClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    BasicPromptAlertDialog(
        modifier = modifier,
        finish = { accepted ->
            if (accepted) {
                onContinueClick()
            } else {
                onCancelClick()
            }
        },
        title = {
            Text(
                text = stringResource(id = R.string.accountSettings_nonTransferableAssetsWarning_title),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
        },
        message = {
            Text(
                text = stringResource(id = R.string.accountSettings_nonTransferableAssetsWarning_message),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1
            )
        },
        confirmText = stringResource(id = R.string.common_continue),
        dismissText = stringResource(id = R.string.common_cancel),
        confirmTextColor = RadixTheme.colors.red1
    )
}

@UsesSampleValues
@Composable
@Preview
private fun DeletingAccountMoveAssetsFetchingBalancesPreview(
    @PreviewParameter(DeletingAccountMoveAssetsPreviewProvider::class) state: DeletingAccountMoveAssetsViewModel.State
) {
    RadixWalletPreviewTheme {
        DeletingAccountMoveAssetsContent(
            state = state,
            onSkipRequested = {},
            onSkipConfirm = {},
            onSkipCancel = {},
            onAccountSelected = {},
            onMessageShown = {},
            onSubmit = {},
            onDismiss = {}
        )
    }
}

@UsesSampleValues
class DeletingAccountMoveAssetsPreviewProvider : PreviewParameterProvider<DeletingAccountMoveAssetsViewModel.State> {

    override val values: Sequence<DeletingAccountMoveAssetsViewModel.State>
        get() = sequenceOf(
            DeletingAccountMoveAssetsViewModel.State(
                deletingAccountAddress = AccountAddress.sampleMainnet()
            ),
            DeletingAccountMoveAssetsViewModel.State(
                deletingAccountAddress = AccountAddress.sampleMainnet(),
                isFetchingBalances = false,
                accountsWithBalances = mapOf(
                    AccountMainnetSample.bob to 10.toDecimal192(),
                    AccountMainnetSample.alice to 0.toDecimal192(),
                    AccountMainnetSample.carol to 1.toDecimal192(),
                ),
                selectedAccount = AccountMainnetSample.bob
            ),
            DeletingAccountMoveAssetsViewModel.State(
                deletingAccountAddress = AccountAddress.sampleMainnet(),
                isFetchingBalances = false,
                accountsWithBalances = mapOf(
                    AccountMainnetSample.bob to 0.toDecimal192(),
                    AccountMainnetSample.alice to 0.toDecimal192(),
                    AccountMainnetSample.carol to 0.toDecimal192(),
                )
            ),
            DeletingAccountMoveAssetsViewModel.State(
                deletingAccountAddress = AccountAddress.sampleMainnet(),
                isFetchingBalances = false,
                accountsWithBalances = mapOf(
                    AccountMainnetSample.bob to 10.toDecimal192(),
                    AccountMainnetSample.alice to 0.toDecimal192(),
                    AccountMainnetSample.carol to 1.toDecimal192(),
                ),
                warning = DeletingAccountMoveAssetsViewModel.State.Warning.SkipMovingAssets,
            ),
            DeletingAccountMoveAssetsViewModel.State(
                deletingAccountAddress = AccountAddress.sampleMainnet(),
                isFetchingBalances = false,
                accountsWithBalances = mapOf(
                    AccountMainnetSample.bob to 10.toDecimal192(),
                    AccountMainnetSample.alice to 0.toDecimal192(),
                    AccountMainnetSample.carol to 1.toDecimal192(),
                ),
                warning = DeletingAccountMoveAssetsViewModel.State.Warning.CannotDeleteAccount
            ),
            DeletingAccountMoveAssetsViewModel.State(
                deletingAccountAddress = AccountAddress.sampleMainnet(),
                isFetchingBalances = false,
                accountsWithBalances = mapOf(
                    AccountMainnetSample.bob to 10.toDecimal192(),
                    AccountMainnetSample.alice to 0.toDecimal192(),
                    AccountMainnetSample.carol to 1.toDecimal192(),
                ),
                warning = DeletingAccountMoveAssetsViewModel.State.Warning.CannotTransferSomeAssets
            )
        )
}
