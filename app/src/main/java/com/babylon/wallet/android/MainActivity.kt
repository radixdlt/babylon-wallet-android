package com.babylon.wallet.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.ui.theme.BabylonWalletTheme
import com.babylon.wallet.android.ui.theme.RadixGrey2

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BabylonWalletTheme {
                Column {
                    RDXAppBar(stringResource(id = R.string.home_toolbar_title)
                    ) {
                        Toast.makeText(
                            this@MainActivity,
                            "Settings clicked",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    Text(
                        text = stringResource(id = R.string.home_welcome_text),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.body1,
                        color = RadixGrey2
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (viewModel.uiState.collectAsState().value) {
                            is UiState.Loaded -> {
                                Button(
                                    onClick = { /*TODO*/ },
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(text = stringResource(id = R.string.create_new_account))
                                }

                                Text(stringResource(id = R.string.radar_network_text),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(45.dp)
                                )

                                Button(
                                    onClick = { /*TODO*/ },
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(text = stringResource(id = R.string.visit_the_radar_hub))
                                }
                            }

                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RDXAppBar(toolbarTitle: String, onMenuItemClicked: () -> Unit) {
    TopAppBar(title = {
        Text(
            text = toolbarTitle,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold
        )
    },
        actions = {
            IconButton(onClick = { onMenuItemClicked() }) {
                BadgedBox(badge = { Badge() }, modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector =
                        ImageVector.vectorResource(id = R.drawable.ic_home_settings),
                        ""
                    )
                }
            }
        }
    )
}

//@Composable
//fun MySwitch(checked: Boolean, onCheckChanged: (Boolean) -> Unit) {
//    Switch(
//        checked = checked,
//        onCheckedChange = {
//            onCheckChanged(it)
//        }
//    )
//}

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
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { /*TODO*/ },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(text = stringResource(id = R.string.create_new_account))
                }
                
                Text(stringResource(id = R.string.radar_network_text),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(45.dp)
                )

                Button(
                    onClick = { /*TODO*/ },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(text = stringResource(id = R.string.visit_the_radar_hub))
                }
            }
        }
    }
}