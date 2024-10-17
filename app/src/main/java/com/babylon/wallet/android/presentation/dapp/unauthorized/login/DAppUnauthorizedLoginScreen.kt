package com.babylon.wallet.android.presentation.dapp.unauthorized.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.babylon.wallet.android.domain.userFriendlyMessage
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.FailureDialogState
import com.babylon.wallet.android.presentation.dapp.unauthorized.InitialUnauthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities.EntitiesForProofWithSignatures
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import kotlinx.coroutines.flow.filterIsInstance

@Composable
fun DappUnauthorizedLoginScreen(
    viewModel: DAppUnauthorizedLoginViewModel,
    onNavigateToChooseAccount: (String, Int, Boolean) -> Unit,
    onNavigateToOneTimePersonaData: (RequiredPersonaFields) -> Unit,
    onNavigateToVerifyPersona: (String, EntitiesForProofWithSignatures) -> Unit,
    onNavigateToVerifyAccounts: (String, EntitiesForProofWithSignatures) -> Unit,
    onLoginFlowComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.initialUnauthorizedLoginRoute) {
        when (val route = state.initialUnauthorizedLoginRoute) {
            is InitialUnauthorizedLoginRoute.ChooseAccount -> onNavigateToChooseAccount(
                route.walletUnauthorizedRequestInteractionId,
                route.numberOfAccounts,
                route.isExactAccountsCount
            )
            is InitialUnauthorizedLoginRoute.OnetimePersonaData -> onNavigateToOneTimePersonaData(route.requiredPersonaFields)
            is InitialUnauthorizedLoginRoute.VerifyPersona -> onNavigateToVerifyPersona(
                route.walletUnauthorizedRequestInteractionId,
                route.entitiesForProofWithSignatures
            )
            is InitialUnauthorizedLoginRoute.VerifyAccounts -> onNavigateToVerifyAccounts(
                route.walletUnauthorizedRequestInteractionId,
                route.entitiesForProofWithSignatures
            )
            null -> {}
        }
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.filterIsInstance<Event.CloseLoginFlow>().collect {
            onLoginFlowComplete()
        }
    }

    Box(
        modifier = modifier
            .systemBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
    ) {
        AnimatedVisibility(
            visible = state.initialUnauthorizedLoginRoute == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            FullscreenCircularProgressContent()
        }

        when (val dialogState = state.failureDialogState) {
            is FailureDialogState.Closed -> {}
            is FailureDialogState.Open -> {
                BasicPromptAlertDialog(
                    finish = { viewModel.onAcknowledgeFailureDialog() },
                    title = {
                        Text(
                            text = stringResource(id = R.string.error_dappRequest_invalidRequest),
                            style = RadixTheme.typography.body1Header,
                            color = RadixTheme.colors.gray1
                        )
                    },
                    message = {
                        Text(
                            text = dialogState.dappRequestException.userFriendlyMessage(),
                            style = RadixTheme.typography.body2Regular,
                            color = RadixTheme.colors.gray1
                        )
                    },
                    confirmText = stringResource(id = R.string.common_cancel),
                    dismissText = null
                )
            }
        }

        SnackbarUiMessageHandler(
            message = state.uiMessage,
            onMessageShown = viewModel::onMessageShown,
            modifier = Modifier.imePadding()
        )
    }
}
