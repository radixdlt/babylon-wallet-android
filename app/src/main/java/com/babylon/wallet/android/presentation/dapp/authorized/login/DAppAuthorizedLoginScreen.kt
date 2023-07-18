package com.babylon.wallet.android.presentation.dapp.authorized.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.RequiredFields
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler

@Composable
fun DappAuthorizedLoginScreen(
    viewModel: DAppAuthorizedLoginViewModel,
    onBackClick: () -> Unit,
    navigateToChooseAccount: (Int, Boolean, Boolean, Boolean) -> Unit,
    navigateToPermissions: (Int, Boolean, Boolean, Boolean) -> Unit,
    navigateToOneTimePersonaData: (RequiredFields) -> Unit,
    navigateToSelectPersona: (String) -> Unit,
    navigateToOngoingPersonaData: (String, RequiredFields) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                Event.RejectLogin -> onBackClick()
                else -> {}
            }
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
        is InitialAuthorizedLoginRoute.OneTimePersonaData -> navigateToOneTimePersonaData(route.requiredFields)
        is InitialAuthorizedLoginRoute.OngoingPersonaData -> navigateToOngoingPersonaData(
            route.personaAddress,
            route.requiredFields
        )
        is InitialAuthorizedLoginRoute.Permission -> navigateToPermissions(
            route.numberOfAccounts,
            route.isExactAccountsCount,
            route.oneTime,
            route.showBack
        )
        is InitialAuthorizedLoginRoute.SelectPersona -> navigateToSelectPersona(route.reqId)
        else -> {}
    }
    Box(
        modifier = modifier
//            .systemBarsPadding()
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
        SnackbarUiMessageHandler(
            message = state.uiMessage,
            onMessageShown = viewModel::onMessageShown,
            modifier = Modifier.imePadding()
        )
    }
}
