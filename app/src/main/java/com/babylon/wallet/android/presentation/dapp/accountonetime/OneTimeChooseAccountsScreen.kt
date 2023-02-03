package com.babylon.wallet.android.presentation.dapp.accountonetime

import androidx.activity.compose.BackHandler
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.presentation.dapp.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.ui.composables.ChooseAccountContent
import kotlinx.collections.immutable.persistentListOf

@Composable
fun UnauthorizedChooseAccountsScreen(
    viewModel: OneTimeChooseAccountsViewModel,
    exitRequestFlow: () -> Unit,
    dismissErrorDialog: () -> Unit,
    onAccountCreationClick: () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                OneTimeChooseAccountsEvent.NavigateToCompletionScreen -> {
                    exitRequestFlow()
                }
                OneTimeChooseAccountsEvent.FailedToSendResponse -> {
                    exitRequestFlow() // TODO probably later we need to show an error message
                }
            }
        }
    }
    BackHandler(true) {}
    val state = viewModel.state
    ChooseAccountContent(
        onBackClick = viewModel::onRejectRequest,
        onContinueClick = {
            viewModel.sendAccountsResponse()
        },
        isContinueButtonEnabled = state.isContinueButtonEnabled,
        accountItems = state.availableAccountItems,
        numberOfAccounts = state.numberOfAccounts,
        quantifier = state.quantifier,
        onAccountSelect = viewModel::onAccountSelect,
        onCreateNewAccount = onAccountCreationClick,
        dappMetadata = state.dappMetadata,
        isOneTime = true,
    )
    state.error?.let { error ->
        DAppAlertDialog(
            title = stringResource(id = R.string.dapp_verification_error_title),
            body = error,
            dismissErrorDialog = dismissErrorDialog
        )
    }
}

@Composable
fun DAppAlertDialog(
    title: String,
    body: String,
    dismissErrorDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = {},
        title = { Text(text = title, color = Color.Black) },
        text = { Text(text = body, color = Color.Black) },
        confirmButton = {
            TextButton(
                onClick = dismissErrorDialog
            ) {
                Text(stringResource(id = R.string.ok), color = Color.Black)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun UnauthorizedAccountContentPreview() {
    RadixWalletTheme {
        ChooseAccountContent(
            onBackClick = {},
            onContinueClick = {},
            isContinueButtonEnabled = true,
            numberOfAccounts = 1,
            quantifier = MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier.AtLeast,
            isOneTime = true,
            dappMetadata = DappMetadata("", mapOf(MetadataConstants.KEY_NAME to "dApp")),
            onAccountSelect = {},
            onCreateNewAccount = {},
            accountItems = persistentListOf(
                AccountItemUiModel(
                    displayName = "Account name 1",
                    address = "fdj209d9320",
                    appearanceID = 1,
                    isSelected = true
                ),
                AccountItemUiModel(
                    displayName = "Account name 2",
                    address = "342f23f2",
                    appearanceID = 1,
                    isSelected = false
                )
            ),
        )
    }
}
