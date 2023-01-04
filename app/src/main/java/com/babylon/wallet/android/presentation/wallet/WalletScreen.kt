package com.babylon.wallet.android.presentation.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.RDXAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAccountClick: (accountId: String, accountName: String) -> Unit = { _, _ -> },
    onAccountCreationClick: () -> Unit,
) {
    val state by viewModel.walletUiState.collectAsStateWithLifecycle()
    WalletScreenContent(
        onMenuClick = onMenuClick,
        onAccountClick = onAccountClick,
        onAccountCreationClick = onAccountCreationClick,
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
        onCopyAccountAddressClick = viewModel::onCopyAccountAddress,
        modifier = modifier.systemBarsPadding(),
        isLoading = state.isLoading,
        accounts = state.resources,
        error = state.error,
        onMessageShown = viewModel::onMessageShown
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun WalletScreenContent(
    onMenuClick: () -> Unit,
    onAccountClick: (accountId: String, accountName: String) -> Unit,
    onAccountCreationClick: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onCopyAccountAddressClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    accounts: ImmutableList<AccountResources>,
    error: UiMessage?,
    onMessageShown: () -> Unit,
) {
    Box(modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                RDXAppBar(
                    toolbarTitle = stringResource(id = R.string.home_toolbar_title),
                    onMenuClick = onMenuClick
                )
            },
            contentColor = RadixTheme.colors.defaultText,
            backgroundColor = RadixTheme.colors.defaultBackground
        ) { innerPadding ->
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = RadixTheme.colors.gray1
                    )
                }
            }
            AnimatedVisibility(visible = !isLoading, enter = fadeIn()) {
                val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = onRefresh)
                Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
                    WalletAccountList(
                        onCopyAccountAddressClick = onCopyAccountAddressClick,
                        onAccountClick = onAccountClick,
                        onAccountCreationClick = onAccountCreationClick,
                        accounts = accounts,
                        modifier = Modifier
                    )
                    PullRefreshIndicator(
                        refreshing = isRefreshing,
                        state = pullRefreshState,
                        contentColor = RadixTheme.colors.gray1,
                        backgroundColor = RadixTheme.colors.defaultBackground,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
        SnackbarUiMessageHandler(message = error, onMessageShown = onMessageShown)
    }
}

@Suppress("UnstableCollections")
@Composable
private fun WalletAccountList(
    onCopyAccountAddressClick: (String) -> Unit,
    onAccountClick: (accountId: String, accountName: String) -> Unit,
    onAccountCreationClick: () -> Unit,
    accounts: List<AccountResources>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = stringResource(id = R.string.home_welcome_text),
                modifier = Modifier.padding(
                    top = RadixTheme.dimensions.paddingMedium,
                    start = RadixTheme.dimensions.paddingDefault,
                    end = RadixTheme.dimensions.paddingDefault
                ),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = RadixTheme.dimensions.paddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
//                Text(
//                    text = stringResource(id = R.string.total_value).uppercase(
//                        Locale.getDefault()
//                    ),
//                    style = RadixTheme.typography.body2Header,
//                )
//                wallet?.let { wallet ->
//                    WalletBalanceView(
//                        currencySignValue = wallet.currency,
//                        amount = wallet.amount,
//                        hidden = false,
//                        balanceClicked = balanceClicked
//                    )
//                }
            }
        }
        itemsIndexed(accounts) { _, account ->
            val gradientColors = AccountGradientList[account.appearanceID]
            AccountCardView(
                hashValue = account.address,
                accountName = account.displayName,
                onCopyClick = { onCopyAccountAddressClick(account.address) },
                assets = account.fungibleTokens,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                    .background(Brush.linearGradient(gradientColors), shape = RadixTheme.shapes.roundedRectMedium)
                    .throttleClickable {
                        onAccountClick(account.address, account.address)
                    }
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = RadixTheme.dimensions.paddingDefault),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RadixSecondaryButton(
                    text = stringResource(id = R.string.create_new_account),
                    onClick = onAccountCreationClick,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun WalletContentPreview() {
    RadixWalletTheme {
        with(SampleDataProvider()) {
            WalletScreenContent(
                onMenuClick = {},
                onAccountClick = { _, _ -> },
                onAccountCreationClick = { },
                isRefreshing = false,
                onRefresh = { },
                onCopyAccountAddressClick = {},
                modifier = Modifier.fillMaxSize(),
                isLoading = false,
                accounts = persistentListOf(sampleAccountResource(), sampleAccountResource()),
                error = null
            ) {}
        }
    }
}
