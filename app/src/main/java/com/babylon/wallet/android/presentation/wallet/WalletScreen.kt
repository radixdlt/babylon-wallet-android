package com.babylon.wallet.android.presentation.wallet

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import com.babylon.wallet.android.presentation.helpers.MockMainViewRepository
import com.babylon.wallet.android.presentation.ui.composables.AccountCardView
import com.babylon.wallet.android.presentation.ui.composables.BabylonButton
import com.babylon.wallet.android.presentation.ui.composables.RDXAppBar
import com.babylon.wallet.android.presentation.ui.composables.WalletBalanceView
import com.babylon.wallet.android.presentation.ui.theme.BabylonWalletTheme
import com.babylon.wallet.android.presentation.ui.theme.RadixGrey2
import java.util.Locale

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onAccountClick: (accountId: String, accountName: String) -> Unit = { _: String, _: String -> },
) {
    Column {
        RDXAppBar(
            stringResource(id = R.string.home_toolbar_title)
        ) {}
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(id = R.string.home_welcome_text),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.body1,
                color = RadixGrey2
            )

            val walletState = viewModel.walletUiState.collectAsStateWithLifecycle().value
            val accountState = viewModel.accountUiState.collectAsStateWithLifecycle().value
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (walletState) {
                    is WalletUiState.Loading -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colors.onPrimary
                        )
                    }
                    is WalletUiState.Loaded -> {
                        Text(
                            text = stringResource(id = R.string.total_value).uppercase(
                                Locale.US
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        WalletBalanceView(
                            currencySignValue = walletState.walletData.currency,
                            amount = walletState.walletData.amount,
                            hidden = false
                        ) {
                            // TODO
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 30.dp, 0.dp, 0.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (accountState) {
                    is AccountsUiState.Loading -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colors.onPrimary
                        )
                    }
                    is AccountsUiState.Loaded -> {
                        // TODO build a list of cards like in the figma prototype
                        val accountHash = accountState.accounts[0].hash
                        AccountCardView(
                            onCardClick = {
                                onAccountClick(
                                    accountState.accounts[0].id,
                                    accountState.accounts[0].name
                                )
                            },
                            hashValue = accountHash,
                            accountName = accountState.accounts[0].name,
                            accountValue = accountState.accounts[0].amount,
                            accountCurrency = accountState.accounts[0].currencySymbol,
                            onCopyClick = { viewModel.onCopy(accountHash) },
                            assetsIcons = accountState.accounts[0].accountAssets
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 48.dp, 0.dp, 0.dp),
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
}

@Composable
fun RadarHubView(onClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(15.dp)
            .fillMaxWidth()
            .padding(45.dp, 40.dp, 45.dp, 0.dp),
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
@Composable
fun RadarHubPreview() {
    BabylonWalletTheme {
        RadarHubView {}
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
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
