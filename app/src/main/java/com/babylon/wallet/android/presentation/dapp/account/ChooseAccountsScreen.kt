package com.babylon.wallet.android.presentation.dapp.account

import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginEvent
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginViewModel
import com.babylon.wallet.android.presentation.ui.composables.ChooseAccountContent
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ChooseAccountsScreen(
    viewModel: ChooseAccountsViewModel,
    sharedViewModel: DAppLoginViewModel,
    onBackClick: () -> Unit,
    dismissErrorDialog: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onChooseAccounts: (DAppLoginEvent.ChooseAccounts) -> Unit,
    onLoginFlowComplete: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        sharedViewModel.oneOffEvent.collect { event ->
            when (event) {
                is DAppLoginEvent.ChooseAccounts -> onChooseAccounts(event)
                is DAppLoginEvent.LoginFlowCompleted -> onLoginFlowComplete(event.dappName)
                else -> {}
            }
        }
    }

    val state = viewModel.state
    val sharedState by sharedViewModel.state.collectAsState()
    ChooseAccountContent(
        onBackClick = onBackClick,
        onContinueClick = {
            sharedViewModel.onAccountsSelected(state.selectedAccounts)
        },
        onRejectClick = {
            // Reject entire flow, not applicable right now ?
        },
        isContinueButtonEnabled = state.isContinueButtonEnabled,
        isDismissable = false,
        accountItems = state.availableAccountItems,
        numberOfAccounts = state.numberOfAccounts,
        isExactAccountsCount = state.isExactAccountsCount,
        onAccountSelect = viewModel::onAccountSelect,
        onCreateNewAccount = onAccountCreationClick,
        dappMetadata = sharedState.dappMetadata,
        isOneTime = state.oneTimeRequest,
        isSingleChoice = state.isSingleChoice
    )

    state.error?.let { error ->
        ErrorAlertDialog(
            title = stringResource(id = R.string.dapp_verification_error_title),
            body = error,
            dismissErrorDialog = dismissErrorDialog
        )
    }
}

@Composable
private fun ErrorAlertDialog(
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
fun ChooseAccountContentPreview() {
    RadixWalletTheme {
        ChooseAccountContent(
            onBackClick = {},
            onContinueClick = {},
            onRejectClick = {},
            isContinueButtonEnabled = true,
            isDismissable = false,
            numberOfAccounts = 1,
            isExactAccountsCount = false,
            isOneTime = false,
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
            isSingleChoice = false,
        )
    }
}
