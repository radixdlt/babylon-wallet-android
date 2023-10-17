package com.babylon.wallet.android.presentation.dapp.authorized.login

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
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.domain.toUserFriendlyMessage
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import kotlinx.coroutines.flow.filterIsInstance

@Composable
fun DappAuthorizedLoginScreen(
    viewModel: DAppAuthorizedLoginViewModel,
    onBackClick: () -> Unit,
    navigateToChooseAccount: (Int, Boolean, Boolean, Boolean) -> Unit,
    navigateToPermissions: (Int, Boolean, Boolean, Boolean) -> Unit,
    navigateToOneTimePersonaData: (RequiredPersonaFields) -> Unit,
    navigateToSelectPersona: (String) -> Unit,
    navigateToOngoingPersonaData: (String, RequiredPersonaFields) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.filterIsInstance<Event.CloseLoginFlow>().collect {
            onBackClick()
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val route = state.initialAuthorizedLoginRoute) {
        is InitialAuthorizedLoginRoute.ChooseAccount -> navigateToChooseAccount(
            route.numberOfAccounts,
            route.isExactAccountsCount,
            route.oneTime,
            route.showBack
        )
        is InitialAuthorizedLoginRoute.OneTimePersonaData -> navigateToOneTimePersonaData(route.requiredPersonaFields)
        is InitialAuthorizedLoginRoute.OngoingPersonaData -> navigateToOngoingPersonaData(
            route.personaAddress,
            route.requiredPersonaFields
        )
        is InitialAuthorizedLoginRoute.Permission -> navigateToPermissions(
            route.numberOfAccounts,
            route.isExactAccountsCount,
            route.oneTime,
            route.showBack
        )
        is InitialAuthorizedLoginRoute.SelectPersona -> navigateToSelectPersona(route.dappDefinitionAddress)
        else -> {}
    }
    Box(
        modifier = modifier
            .systemBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
    ) {
        AnimatedVisibility(
            visible = state.initialAuthorizedLoginRoute == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            FullscreenCircularProgressContent()
        }

        when (val dialogState = state.failureDialog) {
            is DAppLoginUiState.FailureDialog.Closed -> {}
            is DAppLoginUiState.FailureDialog.Open -> {
                BasicPromptAlertDialog(
                    finish = { viewModel.onAcknowledgeFailureDialog() },
                    title = {
                        Text(
                            text = stringResource(id = R.string.error_dappRequest_invalidRequest),
                            style = RadixTheme.typography.body1Header,
                            color = RadixTheme.colors.gray1
                        )
                    },
                    text = {
                        Text(
                            text = dialogState.dappRequestException.toUserFriendlyMessage(),
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
