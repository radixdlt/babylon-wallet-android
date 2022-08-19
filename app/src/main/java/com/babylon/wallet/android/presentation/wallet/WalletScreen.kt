package com.babylon.wallet.android.presentation.wallet

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.babylon.wallet.android.presentation.helpers.MockMainViewRepository
import com.babylon.wallet.android.presentation.model.AccountUi
import com.babylon.wallet.android.presentation.ui.composables.BabylonButton
import com.babylon.wallet.android.presentation.ui.composables.RDXAppBar
import com.babylon.wallet.android.presentation.ui.composables.WalletBalanceView
import com.babylon.wallet.android.presentation.ui.theme.BabylonWalletTheme
import com.babylon.wallet.android.presentation.ui.theme.RadixGrey2
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.util.Locale

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onAccountClick: (accountId: String, accountName: String) -> Unit = { _: String, _: String -> },
) {

    val state: WalletUiState by viewModel.walletUiState.collectAsStateWithLifecycle()
    val swipeRefreshState = rememberSwipeRefreshState(viewModel.isRefreshing.collectAsStateWithLifecycle().value)

    Scaffold(
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
                    onRefresh = { viewModel.refresh() },
                    indicatorPadding = innerPadding,
                    refreshTriggerDistance = 100.dp,
                    content = {
                        WalletContent(
                            wallet = (state as WalletUiState.Loaded).wallet,
                            accounts = (state as WalletUiState.Loaded).accounts,
                            onCopyAccountAddressClick = viewModel::onCopyAccountAddress,
                            onAccountClick = onAccountClick,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun WalletContent(
    wallet: WalletData,
    accounts: List<AccountUi>,
    onCopyAccountAddressClick: (String) -> Unit,
    onAccountClick: (String, String) -> Unit,
    modifier: Modifier
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
                /*TODO*/
            }

            RadarHubView {
                /*TODO*/
            }
        }
    }
}

@Composable
fun RadarHubView(onClicked: () -> Unit) {
    Column(
        modifier = Modifier
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
        WalletContent(
            wallet = WalletData(
                currency = "$",
                amount = "236246"
            ),
            accounts = mockAccountUiList,
            onCopyAccountAddressClick = {},
            onAccountClick = { _, _ -> },
            modifier = Modifier
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun WalletScreenPreview() {
    val mockViewModel = WalletViewModel(
        mainViewRepository = MockMainViewRepository(),
        clipboardManager = LocalContext
            .current
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    )

    BabylonWalletTheme {
        WalletScreen(
            viewModel = mockViewModel,
            onAccountClick = { _, _ ->
            }
        )
    }
}
