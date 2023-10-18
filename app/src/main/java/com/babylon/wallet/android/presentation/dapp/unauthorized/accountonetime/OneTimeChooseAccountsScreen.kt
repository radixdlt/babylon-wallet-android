package com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.DAppUnauthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.Event
import com.babylon.wallet.android.presentation.status.signing.SigningStatusBottomDialog
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.ChooseAccountContent
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import kotlinx.collections.immutable.persistentListOf

@Composable
fun OneTimeChooseAccountsScreen(
    viewModel: OneTimeChooseAccountsViewModel,
    exitRequestFlow: () -> Unit,
    dismissErrorDialog: () -> Unit,
    onAccountCreationClick: () -> Unit,
    sharedViewModel: DAppUnauthorizedLoginViewModel,
    onLoginFlowComplete: () -> Unit,
    onPersonaOnetime: (RequiredPersonaFields) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedViewModelState by sharedViewModel.state.collectAsStateWithLifecycle()
    if (sharedViewModelState.isNoMnemonicErrorVisible) {
        BasicPromptAlertDialog(
            finish = {
                sharedViewModel.dismissNoMnemonicError()
            },
            title = stringResource(id = R.string.transactionReview_noMnemonicError_title),
            text = stringResource(id = R.string.transactionReview_noMnemonicError_text),
            dismissText = null
        )
    }
    LaunchedEffect(Unit) {
        sharedViewModel.oneOffEvent.collect { event ->
            when (event) {
                is Event.LoginFlowCompleted -> onLoginFlowComplete()
                is Event.PersonaDataOnetime -> onPersonaOnetime(event.requiredPersonaFields)
                Event.CloseLoginFlow -> onLoginFlowComplete()
                is Event.RequestCompletionBiometricPrompt -> {
                    if (event.requestDuringSigning) {
                        sharedViewModel.sendRequestResponse(deviceBiometricAuthenticationProvider = {
                            context.biometricAuthenticateSuspend()
                        })
                    } else {
                        context.biometricAuthenticate { authenticated ->
                            if (authenticated) {
                                sharedViewModel.sendRequestResponse()
                            }
                        }
                    }
                }
            }
        }
    }
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
    BackHandler {
        sharedViewModel.onRejectRequest()
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    ChooseAccountContent(
        onBackClick = sharedViewModel::onRejectRequest,
        onContinueClick = {
            sharedViewModel.onAccountsSelected(state.selectedAccounts())
        },
        isContinueButtonEnabled = state.isContinueButtonEnabled,
        accountItems = state.availableAccountItems,
        onAccountSelect = viewModel::onAccountSelect,
        onCreateNewAccount = onAccountCreationClick,
        dappWithMetadata = sharedState.dappWithMetadata,
        isOneTime = true,
        isSingleChoice = state.isSingleChoice,
        numberOfAccounts = state.numberOfAccounts,
        isExactAccountsCount = state.isExactAccountsCount,
        showBackButton = false,
        modifier = modifier
    )
    sharedState.interactionState?.let {
        SigningStatusBottomDialog(
            modifier = Modifier.fillMaxHeight(0.8f),
            onDismissDialogClick = sharedViewModel::onDismissSigningStatusDialog,
            interactionState = it
        )
    }
    state.error?.let { error ->
        DAppAlertDialog(
            title = stringResource(id = R.string.dAppRequest_chooseAccounts_verificationErrorTitle),
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
                Text(stringResource(id = R.string.common_ok), color = Color.Black)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun OneTimeAccountContentPreview() {
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
            dappWithMetadata = DAppWithMetadata(
                dAppAddress = "account_tdx_abc",
                nameItem = NameMetadataItem("dApp")
            ),
            isOneTime = true,
            isSingleChoice = false,
            numberOfAccounts = 1,
            isExactAccountsCount = false,
            showBackButton = false
        )
    }
}
