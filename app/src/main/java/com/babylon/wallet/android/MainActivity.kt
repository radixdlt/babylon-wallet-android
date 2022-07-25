package com.babylon.wallet.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.compose.BabylonButton
import com.babylon.wallet.android.compose.RDXAppBar
import com.babylon.wallet.android.compose.WalletBalanceView
import com.babylon.wallet.android.ui.theme.BabylonWalletTheme
import com.babylon.wallet.android.ui.theme.RadixGrey2
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BabylonWalletTheme {
                Column {
                    RDXAppBar(stringResource(id = R.string.home_toolbar_title)
                    ) {}
                    Text(
                        text = stringResource(id = R.string.home_welcome_text),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.body1,
                        color = RadixGrey2
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (val result = viewModel.uiState.collectAsState().value) {
                            is UiState.Loading -> {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colors.onPrimary
                                )
                            }
                            is UiState.Loaded -> {
                                Text(
                                    text = stringResource(id = R.string.total_value).uppercase(
                                        Locale.US
                                    ),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                WalletBalanceView(
                                    result.walletData.currency,
                                    result.walletData.amount,
                                    false,
                                    {
                                        //TODO
                                    })
                            }

                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BabylonButton(title = stringResource(id = R.string.create_new_account)) {
                            /*TODO*/
                        }

                        RadarHubView({
                            /*TODO*/
                        })
                    }
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
            .clickable { onClicked() }
            .padding(45.dp, 100.dp, 45.dp, 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
        ) {
            Text(
                stringResource(id = R.string.radar_network_text),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(45.dp)
            )
        }
        BabylonButton(
            title = stringResource(id = R.string.visit_the_radar_hub),
            modifier = Modifier.fillMaxWidth()
        ) { }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BabylonWalletTheme {
        Column {
            RDXAppBar(stringResource(id = R.string.home_toolbar_title)
            ) {}
            Text(
                text = stringResource(id = R.string.home_welcome_text),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.body1,
                color = RadixGrey2
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.total_value).uppercase(Locale.US),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                WalletBalanceView(stringResource(id = R.string.dollar_sign), "1", false, {
                    //TODO
                })
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BabylonButton(title = stringResource(id = R.string.create_new_account)) {
                    /*TODO*/
                }

                RadarHubView({
                    /*TODO*/
                })
            }
        }
    }
}