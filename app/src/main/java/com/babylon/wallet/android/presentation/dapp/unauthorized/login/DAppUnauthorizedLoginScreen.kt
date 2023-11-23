@file:OptIn(ExperimentalAnimationApi::class, ExperimentalAnimationApi::class, ExperimentalAnimationApi::class)

package com.babylon.wallet.android.presentation.dapp.unauthorized.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
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
import com.babylon.wallet.android.domain.userFriendlyMessage
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.FailureDialogState
import com.babylon.wallet.android.presentation.dapp.InitialUnauthorizedLoginRoute
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import kotlinx.coroutines.flow.filterIsInstance

@Composable
fun DappUnauthorizedLoginScreen(
    viewModel: DAppUnauthorizedLoginViewModel,
    navigateToChooseAccount: (Int, Boolean) -> Unit,
    navigateToOneTimePersonaData: (RequiredPersonaFields) -> Unit,
    onLoginFlowComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val route = state.initialUnauthorizedLoginRoute) {
        is InitialUnauthorizedLoginRoute.ChooseAccount -> navigateToChooseAccount(
            route.numberOfAccounts,
            route.isExactAccountsCount
        )
        is InitialUnauthorizedLoginRoute.OnetimePersonaData -> navigateToOneTimePersonaData(route.requiredPersonaFields)
        null -> {}
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
                    text = {
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
