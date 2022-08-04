package com.babylon.wallet.android.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.presentation.helpers.MockMainViewRepository
import com.babylon.wallet.android.presentation.ui.composables.WalletBalanceView
import com.babylon.wallet.android.presentation.ui.theme.BabylonWalletTheme

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    accountName: String,
    onMenuItemClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val state = viewModel.accountUiState.collectAsState().value
    Column {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "navigate back"
                    )
                }
            },
            title = {
                Text(
                    text = accountName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            actions = {
                IconButton(onClick = { onMenuItemClick() }) {
                    BadgedBox(badge = { Badge() }, modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            "account settings"
                        )
                    }
                }
            },
            elevation = 0.dp
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 32.dp, start = 14.dp, end = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {

                when (state) {
                    is AccountUiState.Loaded -> {
                        Text(
                            text = state.account.hash,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            imageVector = Icons.Default.Share,
                            contentDescription = "copy"
                        )
                    }
                    AccountUiState.Loading -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colors.onPrimary
                        )
                    }
                }
            }

            when (state) {
                is AccountUiState.Loaded -> {
                    WalletBalanceView(
                        currencySignValue = state.account.currencySymbol,
                        amount = state.account.amount,
                        hidden = false
                    ) {
                    }

                    Button(onClick = { /*TODO*/ }) {
                        Text(text = "Transfer")
                    }
                }
                AccountUiState.Loading -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.onPrimary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun AccountScreenPreview() {
    val mockViewModel = AccountViewModel(
        mainViewRepository = MockMainViewRepository(),
        savedStateHandle = SavedStateHandle()
    )
    BabylonWalletTheme {
        AccountScreen(
            viewModel = mockViewModel,
            accountName = "account name",
            onBackClick = {},
            onMenuItemClick = {}
        )
    }
}
