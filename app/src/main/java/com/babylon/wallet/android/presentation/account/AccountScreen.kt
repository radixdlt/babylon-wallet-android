package com.babylon.wallet.android.presentation.account

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.helpers.MockMainViewRepository
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.ui.composables.ResponsiveText
import com.babylon.wallet.android.presentation.ui.composables.WalletBalanceView
import com.babylon.wallet.android.presentation.ui.theme.BabylonWalletTheme

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    accountName: String,
    onMenuItemClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val state = viewModel.accountUiState.collectAsStateWithLifecycle().value
    val selectedAssetTypeTab = viewModel.selectedAssetTypeTab.collectAsStateWithLifecycle().value

    Scaffold(
        topBar = {
            AccountTopAppBar(
                accountName = accountName,
                onMenuItemClick = onMenuItemClick,
                onBackClick = onBackClick
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 32.dp, start = 14.dp, end = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                when (state) {
                    is AccountUiState.Loaded -> {
                        AccountAddressView(
                            address = state.account.hash,
                            onCopyAccountAddressClick = viewModel::onCopyAccountAddress
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

            AssetTypeTabsRow(
                selectedAssetTypeTab = selectedAssetTypeTab,
                onAssetTypeTabSelected = viewModel::onAssetTypeTabSelected,
            )
        }
    }
}

@Composable
private fun AccountTopAppBar(
    accountName: String,
    onMenuItemClick: () -> Unit,
    onBackClick: () -> Unit
) {
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
                BadgedBox(
                    badge = { Badge() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "account settings"
                    )
                }
            }
        },
        elevation = 0.dp
    )
}

@Composable
private fun AccountAddressView(
    address: String,
    onCopyAccountAddressClick: (String) -> Unit,
) {
    ResponsiveText(
        text = address,
        style = MaterialTheme.typography.h6
    )
    IconButton(
        modifier = Modifier
            .padding(start = 8.dp)
            .size(14.dp),
        onClick = {
            onCopyAccountAddressClick(address)
        },
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_copy),
            contentDescription = "copy account address"
        )
    }
}

@Composable
private fun AssetTypeTabsRow(
    selectedAssetTypeTab: AssetTypeTab,
    onAssetTypeTabSelected: (AssetTypeTab) -> Unit
) {
    val selectedIndex = AssetTypeTab.values().indexOfFirst { it == selectedAssetTypeTab }
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        divider = {}, /* Disable the built-in divider */
        edgePadding = 24.dp,
        indicator = emptyTabIndicator
    ) {
        AssetTypeTab.values().forEachIndexed { index, assetTypeTab ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onAssetTypeTabSelected(assetTypeTab) }
            ) {
                ChoiceChipContent(
                    text = stringResource(id = assetTypeTab.stringId),
                    selected = index == selectedIndex,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ChoiceChipContent(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = when {
            selected -> MaterialTheme.colors.onSurface
            else -> MaterialTheme.colors.primary
        },
        contentColor = when {
            selected -> MaterialTheme.colors.primary
            else -> MaterialTheme.colors.onPrimary
        },
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

private val emptyTabIndicator: @Composable (List<TabPosition>) -> Unit = {}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun AccountScreenPreview() {
    val savedStateHandle = SavedStateHandle()
    savedStateHandle[Screen.ARG_ACCOUNT_ID] = "1"
    val mockViewModel = AccountViewModel(
        mainViewRepository = MockMainViewRepository(),
        clipboardManager = LocalContext
            .current
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager,
        savedStateHandle = savedStateHandle
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

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun AssetTabRowPreview() {
    BabylonWalletTheme {
        AssetTypeTabsRow(
            selectedAssetTypeTab = AssetTypeTab.TOKEN_TAB,
            onAssetTypeTabSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun ChoiceChipContentPreview() {
    BabylonWalletTheme {
        ChoiceChipContent(
            text = "Tokens",
            selected = true,
            modifier = Modifier
        )
    }
}
