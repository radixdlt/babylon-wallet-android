package com.babylon.wallet.android.presentation.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.presentation.ui.composables.RDXAppBar
import com.babylon.wallet.android.presentation.ui.composables.WalletBalanceView
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.util.Locale

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier,
    onAccountClick: (accountId: String, accountName: String, gradientIndex: Int) -> Unit = { _, _, _ -> },
    onAccountCreationClick: () -> Unit
) {
    val state by viewModel.walletUiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    WalletScreenContent(
        state = state,
        onAccountClick = onAccountClick,
        onAccountCreationClick = onAccountCreationClick,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        onCopyAccountAddressClick = viewModel::onCopyAccountAddress,
        modifier = modifier.systemBarsPadding(),
        balanceClicked = {}
    )
}

@Composable
private fun WalletScreenContent(
    state: WalletUiState,
    onAccountClick: (accountId: String, accountName: String, gradientIndex: Int) -> Unit,
    onAccountCreationClick: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onCopyAccountAddressClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    balanceClicked: () -> Unit
) {
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    Scaffold(
        modifier = modifier,
        topBar = {
            RDXAppBar(
                stringResource(id = R.string.home_toolbar_title)
            ) {}
        },
        contentColor = RadixTheme.colors.defaultText,
        backgroundColor = RadixTheme.colors.defaultBackground
    ) { innerPadding ->
        when (state) {
            WalletUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = RadixTheme.colors.gray1
                    )
                }
            }
            is WalletUiState.Loaded -> {
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = onRefresh,
                    indicatorPadding = innerPadding,
                    indicator = { state, dp ->
                        SwipeRefreshIndicator(
                            state = state,
                            refreshTriggerDistance = dp,
                            contentColor = RadixTheme.colors.gray1,
                            backgroundColor = RadixTheme.colors.defaultBackground,
                        )
                    },
                    refreshTriggerDistance = 100.dp,
                    content = {
                        WalletAccountList(
                            wallet = state.wallet,
                            onCopyAccountAddressClick = onCopyAccountAddressClick,
                            onAccountClick = onAccountClick,
                            onAccountCreationClick = onAccountCreationClick,
                            accounts = state.resources,
                            modifier = Modifier,
                            balanceClicked = balanceClicked
                        )
                    }
                )
            }
        }
    }
}

@Suppress("UnstableCollections")
@Composable
private fun WalletAccountList(
    wallet: WalletData,
    onCopyAccountAddressClick: (String) -> Unit,
    onAccountClick: (accountId: String, accountName: String, gradientIndex: Int) -> Unit,
    onAccountCreationClick: () -> Unit,
    accounts: List<AccountResources>,
    modifier: Modifier = Modifier,
    balanceClicked: () -> Unit
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
                Text(
                    text = stringResource(id = R.string.total_value).uppercase(
                        Locale.getDefault()
                    ),
                    style = RadixTheme.typography.body2Header,
                )
                WalletBalanceView(
                    currencySignValue = wallet.currency,
                    amount = wallet.amount,
                    hidden = false,
                    balanceClicked = balanceClicked
                )
            }
        }
        itemsIndexed(accounts) { index, account ->
            val gradientIndex = index % AccountGradientList.size
            val gradientColors = AccountGradientList[gradientIndex]
            AccountCardView(
                hashValue = account.address,
                accountName = account.address,
                accountValue = "10",
                accountCurrency = "$",
                onCopyClick = { onCopyAccountAddressClick(account.address) },
                assets = account.fungibleTokens,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                    .background(Brush.linearGradient(gradientColors), shape = RadixTheme.shapes.roundedRectMedium)
                    .clickable {
                        onAccountClick(account.address, account.address, gradientIndex)
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
                    modifier = Modifier.fillMaxWidth(0.8f),
                    text = stringResource(id = R.string.create_new_account),
                    onClick = onAccountCreationClick
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun WalletContentPreview() {
    BabylonWalletTheme {
        with(SampleDataProvider()) {
            WalletScreenContent(
                state = WalletUiState.Loaded(
                    wallet = WalletData(
                        currency = "$",
                        amount = "236246"
                    ),
                    resources = listOf(sampleAccountResource(), sampleAccountResource())
                ),
                onAccountClick = { _, _, _ -> },
                onAccountCreationClick = { },
                isRefreshing = false,
                onRefresh = { },
                onCopyAccountAddressClick = {},
                modifier = Modifier.fillMaxSize(),
                balanceClicked = {}
            )
        }
    }
}
