package com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.presentation.dapp.DappInteractionFailureDialog
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.DAppUnauthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.Event
import com.babylon.wallet.android.presentation.status.signing.FactorSourceInteractionBottomDialog
import com.babylon.wallet.android.presentation.ui.composables.ChooseAccountContent
import com.babylon.wallet.android.presentation.ui.composables.NoMnemonicAlertDialog
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.domain.DApp

@Composable
fun OneTimeChooseAccountsScreen(
    viewModel: OneTimeChooseAccountsViewModel,
    exitRequestFlow: () -> Unit,
    onAccountCreationClick: () -> Unit,
    sharedViewModel: DAppUnauthorizedLoginViewModel,
    onLoginFlowComplete: () -> Unit,
    onPersonaOnetime: (RequiredPersonaFields) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedViewModelState by sharedViewModel.state.collectAsStateWithLifecycle()
    if (sharedViewModelState.isNoMnemonicErrorVisible) {
        NoMnemonicAlertDialog {
            sharedViewModel.dismissNoMnemonicError()
        }
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
                        context.biometricAuthenticate { result ->
                            if (result == BiometricAuthenticationResult.Succeeded) {
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
        dapp = sharedState.dapp,
        isOneTime = true,
        isSingleChoice = state.isSingleChoice,
        numberOfAccounts = state.numberOfAccounts,
        isExactAccountsCount = state.isExactAccountsCount,
        showBackButton = false,
        modifier = modifier
    )
    sharedState.interactionState?.let {
        FactorSourceInteractionBottomDialog(
            modifier = Modifier.fillMaxHeight(0.8f),
            onDismissDialogClick = sharedViewModel::onDismissSigningStatusDialog,
            interactionState = it
        )
    }
    DappInteractionFailureDialog(
        dialogState = sharedState.failureDialogState,
        onAcknowledgeFailureDialog = sharedViewModel::onAcknowledgeFailureDialog
    )
}

@UsesSampleValues
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
                    address = AccountAddress.sampleMainnet.random(),
                    appearanceID = AppearanceId(1u),
                    isSelected = true
                ),
                AccountItemUiModel(
                    displayName = "Account name 2",
                    address = AccountAddress.sampleMainnet.random(),
                    appearanceID = AppearanceId(2u),
                    isSelected = false
                )
            ),
            onAccountSelect = {},
            onCreateNewAccount = {},
            dapp = DApp.sampleMainnet(),
            isOneTime = true,
            isSingleChoice = false,
            numberOfAccounts = 1,
            isExactAccountsCount = false,
            showBackButton = false
        )
    }
}
