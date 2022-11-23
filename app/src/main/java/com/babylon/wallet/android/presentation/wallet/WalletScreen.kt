package com.babylon.wallet.android.presentation.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixGrey2
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.presentation.ui.composables.BabylonButton
import com.babylon.wallet.android.presentation.ui.composables.RDXAppBar
import com.babylon.wallet.android.presentation.ui.composables.WalletBalanceView
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.util.Locale

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier,
    onAccountClick: (accountId: String, accountName: String) -> Unit = { _: String, _: String -> },
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
        modifier = modifier.systemBarsPadding()
    )
}

@Composable
private fun WalletScreenContent(
    state: WalletUiState,
    onAccountClick: (accountId: String, accountName: String) -> Unit,
    onAccountCreationClick: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onCopyAccountAddressClick: (String) -> Unit,
    modifier: Modifier = Modifier
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
                        color = MaterialTheme.colors.onPrimary
                    )
                }
            }
            is WalletUiState.Loaded -> {
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = onRefresh,
                    indicatorPadding = innerPadding,
                    refreshTriggerDistance = 100.dp,
                    content = {
                        WalletAccountList(
                            wallet = state.wallet,
                            onCopyAccountAddressClick = onCopyAccountAddressClick,
                            onAccountClick = onAccountClick,
                            onAccountCreationClick = onAccountCreationClick,
                            accounts = state.resources,
                            modifier = Modifier
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
    onAccountClick: (String, String) -> Unit,
    onAccountCreationClick: () -> Unit,
    accounts: List<AccountResources>,
    modifier: Modifier = Modifier
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
                style = MaterialTheme.typography.body1,
                color = RadixGrey2
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                WalletBalanceView(
                    currencySignValue = wallet.currency,
                    amount = wallet.amount,
                    hidden = false
                ) {
                    // TODO
                }
            }
        }
        items(accounts) { account ->
            AccountCardView(
                onCardClick = {
                    onAccountClick(
                        account.address,
                        account.address
                    )
                },
                hashValue = account.address,
                accountName = account.address,
                accountValue = "10",
                accountCurrency = "$",
                onCopyClick = { onCopyAccountAddressClick(account.address) },
                assets = account.fungibleTokens,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = RadixTheme.dimensions.paddingLarge,
                        end = RadixTheme.dimensions.paddingLarge,
                        bottom = RadixTheme.dimensions.paddingDefault
                    )
            )
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BabylonButton(title = stringResource(id = R.string.create_new_account)) {
                    onAccountCreationClick()
                }

                RadarHubView {
                    /*TODO*/
                }
            }
        }
    }
}

@Composable
fun RadarHubView(
    modifier: Modifier = Modifier,
    onClicked: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(RadixTheme.dimensions.paddingDefault)
            .fillMaxWidth()
            .padding(start = 45.dp, top = 40.dp, end = 45.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card {
            Text(
                stringResource(id = R.string.radar_network_text),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(45.dp)
            )
        }
        BabylonButton(
            title = stringResource(id = R.string.visit_the_radar_hub),
            modifier = Modifier.fillMaxWidth()
        ) { onClicked() }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun RadarHubPreview() {
    BabylonWalletTheme {
        RadarHubView {}
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
                onAccountClick = { _, _ -> },
                onAccountCreationClick = { },
                isRefreshing = false,
                onRefresh = { },
                onCopyAccountAddressClick = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
