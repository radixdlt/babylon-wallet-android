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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.InitialUnauthorizedLoginRoute
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler

@Composable
fun DappUnauthorizedLoginScreen(
    viewModel: DAppUnauthorizedLoginViewModel,
    navigateToChooseAccount: (Int, Boolean) -> Unit,
    navigateToOneTimePersonaData: (MessageFromDataChannel.IncomingRequest.PersonaRequestItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val route = state.initialUnauthorizedLoginRoute) {
        is InitialUnauthorizedLoginRoute.ChooseAccount -> navigateToChooseAccount(route.numberOfAccounts, route.isExactAccountsCount)
        is InitialUnauthorizedLoginRoute.OnetimePersonaData -> navigateToOneTimePersonaData(route.request)
        null -> {}
    }
    Box(
        modifier = modifier
//            .systemBarsPadding()
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
        SnackbarUiMessageHandler(
            message = state.uiMessage,
            onMessageShown = viewModel::onMessageShown,
            modifier = Modifier.imePadding()
        )
    }
}
