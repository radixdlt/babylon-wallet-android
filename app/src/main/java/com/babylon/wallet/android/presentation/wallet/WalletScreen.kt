package com.babylon.wallet.android.presentation.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
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
import com.babylon.wallet.android.data.mockdata.mockAccountUiList
import com.babylon.wallet.android.presentation.model.AccountUi
import com.babylon.wallet.android.presentation.ui.composables.BabylonButton
import com.babylon.wallet.android.presentation.ui.composables.RDXAppBar
import com.babylon.wallet.android.presentation.ui.composables.WalletBalanceView
import com.babylon.wallet.android.presentation.ui.theme.BabylonWalletTheme
import com.babylon.wallet.android.presentation.ui.theme.RadixGrey2
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.util.*

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
        modifier = modifier,
        state = state,
        onAccountClick = onAccountClick,
        onAccountCreationClick = onAccountCreationClick,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        onCopyAccountAddressClick = viewModel::onCopyAccountAddress
    )
}

@Composable
private fun WalletScreenContent(
    modifier: Modifier,
    state: WalletUiState,
    onAccountClick: (accountId: String, accountName: String) -> Unit,
    onAccountCreationClick: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onCopyAccountAddressClick: (String) -> Unit
) {
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    Scaffold(
        modifier = modifier,
        topBar = {
            RDXAppBar(
                stringResource(id = R.string.home_toolbar_title)
            ) {}
        }
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
                            accounts = state.accounts,
                            onCopyAccountAddressClick = onCopyAccountAddressClick,
                            onAccountClick = onAccountClick,
                            onAccountCreationClick = onAccountCreationClick,
                            modifier = Modifier.verticalScroll(rememberScrollState())
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
    accounts: List<AccountUi>,
    onCopyAccountAddressClick: (String) -> Unit,
    onAccountClick: (String, String) -> Unit,
    onAccountCreationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.home_welcome_text),
            modifier = Modifier.padding(top = 10.dp, start = 16.dp, end = 16.dp),
            style = MaterialTheme.typography.body1,
            color = RadixGrey2
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 20.dp),
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

        for (account in accounts) {
            AccountCardView(
                onCardClick = {
                    onAccountClick(
                        account.id,
                        account.name
                    )
                },
                hashValue = account.hash,
                accountName = account.name,
                accountValue = account.amount,
                accountCurrency = account.currencySymbol,
                onCopyClick = { onCopyAccountAddressClick(account.hash) },
                assets = account.tokens,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 25.dp, end = 25.dp, bottom = 20.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 0.dp, top = 48.dp, end = 0.dp, bottom = 0.dp),
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

@Composable
fun RadarHubView(
    modifier: Modifier = Modifier,
    onClicked: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(15.dp)
            .fillMaxWidth()
            .padding(start = 45.dp, top = 40.dp, end = 45.dp, bottom = 0.dp),
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
        WalletScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = WalletUiState.Loaded(
                WalletData(
                    currency = "$",
                    amount = "236246"
                ), mockAccountUiList
            ),
            onAccountClick = { _, _ -> },
            onAccountCreationClick = { },
            isRefreshing = false,
            onRefresh = { },
            onCopyAccountAddressClick = {}
        )
    }
}