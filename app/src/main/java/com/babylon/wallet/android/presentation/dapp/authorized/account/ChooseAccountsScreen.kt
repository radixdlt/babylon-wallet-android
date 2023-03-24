package com.babylon.wallet.android.presentation.dapp.authorized.account

import androidx.activity.compose.BackHandler
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
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginEvent
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.ui.composables.ChooseAccountContent
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ChooseAccountsScreen(
    viewModel: ChooseAccountsViewModel,
    sharedViewModel: DAppAuthorizedLoginViewModel,
    dismissErrorDialog: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onChooseAccounts: (DAppAuthorizedLoginEvent.ChooseAccounts) -> Unit,
    onLoginFlowComplete: (String?) -> Unit,
    onBackClick: () -> Boolean,
    onPersonaOngoingData: (
        DAppAuthorizedLoginEvent.PersonaDataOngoing
    ) -> Unit
) {
    LaunchedEffect(Unit) {
        sharedViewModel.oneOffEvent.collect { event ->
            when (event) {
                is DAppAuthorizedLoginEvent.ChooseAccounts -> onChooseAccounts(event)
                is DAppAuthorizedLoginEvent.LoginFlowCompleted -> onLoginFlowComplete(event.dappName)
                is DAppAuthorizedLoginEvent.PersonaDataOngoing -> onPersonaOngoingData(event)
                is DAppAuthorizedLoginEvent.RejectLogin -> onLoginFlowComplete(null)
                else -> {}
            }
        }
    }

    val state = viewModel.state
    val sharedState by sharedViewModel.state.collectAsState()
    BackHandler {
        if (state.showBackButton) {
            onBackClick()
        } else {
            sharedViewModel.onRejectLogin()
        }
    }

    ChooseAccountContent(
        onBackClick = {
            if (state.showBackButton) {
                onBackClick()
            } else {
                sharedViewModel.onRejectLogin()
            }
        },
        onContinueClick = {
            sharedViewModel.onAccountsSelected(state.selectedAccounts, state.oneTimeRequest)
        },
        isContinueButtonEnabled = state.isContinueButtonEnabled,
        accountItems = state.availableAccountItems,
        numberOfAccounts = state.numberOfAccounts,
        isExactAccountsCount = state.isExactAccountsCount,
        onAccountSelect = viewModel::onAccountSelect,
        onCreateNewAccount = onAccountCreationClick,
        dappMetadata = sharedState.dappMetadata,
        isOneTime = state.oneTimeRequest,
        isSingleChoice = state.isSingleChoice,
        showBackButton = state.showBackButton
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
            isContinueButtonEnabled = true,
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
            onAccountSelect = {},
            onCreateNewAccount = {},
            dappMetadata = DappMetadata("", mapOf(MetadataConstants.KEY_NAME to "dApp")),
            isOneTime = false,
            isSingleChoice = false,
            numberOfAccounts = 1,
            isExactAccountsCount = false,
            showBackButton = true
        )
    }
}
