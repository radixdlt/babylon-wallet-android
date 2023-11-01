package com.babylon.wallet.android.presentation.dapp.authorized.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
import androidx.compose.material3.Text
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
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.status.signing.SigningStatusBottomDialog
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.ChooseAccountContent
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow

@Composable
fun ChooseAccountsScreen(
    viewModel: ChooseAccountsViewModel,
    sharedViewModel: DAppAuthorizedLoginViewModel,
    dismissErrorDialog: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onChooseAccounts: (Event.ChooseAccounts) -> Unit,
    onLoginFlowComplete: () -> Unit,
    onBackClick: () -> Boolean,
    onPersonaOngoingData: (Event.PersonaDataOngoing) -> Unit,
    onPersonaDataOnetime: (Event.PersonaDataOnetime) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    if (sharedState.isNoMnemonicErrorVisible) {
        BasicPromptAlertDialog(
            finish = {
                sharedViewModel.dismissNoMnemonicError()
            },
            title = stringResource(id = R.string.transactionReview_noMnemonicError_title),
            text = stringResource(id = R.string.transactionReview_noMnemonicError_text),
            dismissText = null
        )
    }
    HandleOneOffEvents(
        oneOffEvent = sharedViewModel.oneOffEvent,
        onChooseAccounts = onChooseAccounts,
        onLoginFlowComplete = onLoginFlowComplete,
        onPersonaOngoingData = onPersonaOngoingData,
        onPersonaDataOnetime = onPersonaDataOnetime,
        completeRequestHandling = sharedViewModel::completeRequestHandling
    )
    BackHandler {
        if (state.showBackButton) {
            onBackClick()
        } else {
            sharedViewModel.onAbortDappLogin()
        }
    }

    ChooseAccountContent(
        onBackClick = {
            if (state.showBackButton) {
                onBackClick()
            } else {
                sharedViewModel.onAbortDappLogin()
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
        dappWithMetadata = sharedState.dappWithMetadata,
        isOneTime = state.oneTimeRequest,
        isSingleChoice = state.isSingleChoice,
        showBackButton = state.showBackButton,
        modifier = modifier
    )

    state.error?.let { error ->
        ErrorAlertDialog(
            title = stringResource(id = R.string.dAppRequest_chooseAccounts_verificationErrorTitle),
            body = error,
            dismissErrorDialog = dismissErrorDialog
        )
    }
    sharedState.interactionState?.let {
        SigningStatusBottomDialog(
            modifier = Modifier.fillMaxHeight(0.8f),
            onDismissDialogClick = sharedViewModel::onDismissSigningStatusDialog,
            interactionState = it
        )
    }
}

@Composable
private fun HandleOneOffEvents(
    oneOffEvent: Flow<Event>,
    onChooseAccounts: (Event.ChooseAccounts) -> Unit,
    onLoginFlowComplete: () -> Unit,
    onPersonaOngoingData: (Event.PersonaDataOngoing) -> Unit,
    onPersonaDataOnetime: (Event.PersonaDataOnetime) -> Unit,
    completeRequestHandling: (deviceBiometricAuthenticationProvider: (suspend () -> Boolean)) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        oneOffEvent.collect { event ->
            when (event) {
                is Event.ChooseAccounts -> onChooseAccounts(event)
                is Event.LoginFlowCompleted -> onLoginFlowComplete()
                is Event.PersonaDataOngoing -> onPersonaOngoingData(event)
                is Event.PersonaDataOnetime -> onPersonaDataOnetime(event)
                is Event.CloseLoginFlow -> onLoginFlowComplete()
                is Event.RequestCompletionBiometricPrompt -> {
                    if (event.signatureRequired) {
                        completeRequestHandling {
                            context.biometricAuthenticateSuspend()
                        }
                    } else {
                        context.biometricAuthenticate { authenticated ->
                            if (authenticated) {
                                completeRequestHandling { true }
                            }
                        }
                    }
                }

                else -> {}
            }
        }
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
                Text(stringResource(id = R.string.common_ok), color = Color.Black)
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
            dappWithMetadata = DAppWithMetadata(
                dAppAddress = "account_tdx_abc",
                nameItem = NameMetadataItem("dApp")
            ),
            isOneTime = false,
            isSingleChoice = false,
            numberOfAccounts = 1,
            isExactAccountsCount = false,
            showBackButton = true
        )
    }
}
