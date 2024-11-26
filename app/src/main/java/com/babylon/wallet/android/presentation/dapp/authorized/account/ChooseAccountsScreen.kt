package com.babylon.wallet.android.presentation.dapp.authorized.account

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.presentation.dapp.DappInteractionFailureDialog
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.dapp.authorized.verifyentities.EntitiesForProofWithSignatures
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.ChooseAccountContent
import com.babylon.wallet.android.presentation.ui.composables.NoMnemonicAlertDialog
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import rdx.works.core.domain.DApp

@Composable
fun ChooseAccountsScreen(
    viewModel: ChooseAccountsViewModel,
    sharedViewModel: DAppAuthorizedLoginViewModel,
    onAccountCreationClick: () -> Unit,
    onChooseAccounts: (Event.ChooseAccounts) -> Unit,
    onLoginFlowComplete: () -> Unit,
    onBackClick: () -> Boolean,
    onPersonaOngoingData: (Event.PersonaDataOngoing) -> Unit,
    onPersonaDataOnetime: (Event.PersonaDataOnetime) -> Unit,
    onNavigateToVerifyPersona: (interactionId: String, EntitiesForProofWithSignatures) -> Unit,
    onNavigateToVerifyAccounts: (interactionId: String, EntitiesForProofWithSignatures) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()

    if (sharedState.isNoMnemonicErrorVisible) {
        NoMnemonicAlertDialog {
            sharedViewModel.dismissNoMnemonicError()
        }
    }

    BackHandler {
        if (state.showBackButton) {
            onBackClick()
        } else {
            sharedViewModel.onAbortDappLogin()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is ChooseAccountsEvent.AccountsCollected -> {
                    sharedViewModel.onAccountsCollected(
                        accountsWithSignatures = event.accountsWithSignatures,
                        isOneTimeRequest = event.isOneTimeRequest
                    )
                }
                ChooseAccountsEvent.TerminateFlow -> sharedViewModel.onAbortDappLogin()
            }
        }
    }

    HandleOneOffEvents(
        oneOffEvent = sharedViewModel.oneOffEvent,
        onChooseAccounts = onChooseAccounts,
        onLoginFlowComplete = onLoginFlowComplete,
        onPersonaOngoingData = onPersonaOngoingData,
        onPersonaDataOnetime = onPersonaDataOnetime,
        onNavigateToVerifyPersona = onNavigateToVerifyPersona,
        onNavigateToVerifyAccounts = onNavigateToVerifyAccounts
    )

    ChooseAccountContent(
        onBackClick = {
            if (state.showBackButton) {
                onBackClick()
            } else {
                sharedViewModel.onAbortDappLogin()
            }
        },
        onContinueClick = viewModel::onContinueClick,
        isContinueButtonEnabled = state.isContinueButtonEnabled,
        accountItems = state.availableAccountItems,
        numberOfAccounts = state.numberOfAccounts,
        isExactAccountsCount = state.isExactAccountsCount,
        onAccountSelected = viewModel::onAccountSelected,
        onCreateNewAccount = onAccountCreationClick,
        dapp = sharedState.dapp,
        isOneTimeRequest = state.isOneTimeRequest,
        isSingleChoice = state.isSingleChoice,
        showBackButton = state.showBackButton,
        isSigningInProgress = state.isSigningInProgress,
        modifier = modifier
    )

    DappInteractionFailureDialog(
        dialogState = sharedState.failureDialog,
        onAcknowledgeFailureDialog = sharedViewModel::onAcknowledgeFailureDialog
    )
}

@Composable
private fun HandleOneOffEvents(
    oneOffEvent: Flow<Event>,
    onChooseAccounts: (Event.ChooseAccounts) -> Unit,
    onLoginFlowComplete: () -> Unit,
    onPersonaOngoingData: (Event.PersonaDataOngoing) -> Unit,
    onPersonaDataOnetime: (Event.PersonaDataOnetime) -> Unit,
    onNavigateToVerifyPersona: (interactionId: String, EntitiesForProofWithSignatures) -> Unit,
    onNavigateToVerifyAccounts: (interactionId: String, EntitiesForProofWithSignatures) -> Unit,
) {
    LaunchedEffect(Unit) {
        oneOffEvent.collect { event ->
            when (event) {
                is Event.ChooseAccounts -> onChooseAccounts(event)
                is Event.LoginFlowCompleted -> onLoginFlowComplete()
                is Event.PersonaDataOngoing -> onPersonaOngoingData(event)
                is Event.PersonaDataOnetime -> onPersonaDataOnetime(event)
                is Event.NavigateToVerifyPersona -> onNavigateToVerifyPersona(
                    event.walletUnauthorizedRequestInteractionId,
                    event.entitiesForProofWithSignatures
                )
                is Event.NavigateToVerifyAccounts -> onNavigateToVerifyAccounts(
                    event.walletUnauthorizedRequestInteractionId,
                    event.entitiesForProofWithSignatures
                )
                is Event.CloseLoginFlow -> onLoginFlowComplete()
                else -> {}
            }
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun ChooseAccountsScreenPreview() {
    RadixWalletPreviewTheme {
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
            onAccountSelected = {},
            onCreateNewAccount = {},
            dapp = DApp.sampleMainnet(),
            isOneTimeRequest = false,
            isSingleChoice = false,
            numberOfAccounts = 1,
            isExactAccountsCount = false,
            isSigningInProgress = false,
            showBackButton = true
        )
    }
}
