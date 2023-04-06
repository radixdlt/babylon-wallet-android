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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.unauthorized.DappUnauthorizedLoginNavigationHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DappUnauthorizedLoginScreen(
    viewModel: DAppUnauthorizedLoginViewModel,
    onBackClick: () -> Unit,
    showSuccessDialog: (requestId: String, dAppName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        viewModel.topLevelOneOffEvent.collect { event ->
            when (event) {
                DAppUnauthorizedLoginEvent.RejectLogin -> onBackClick()
                else -> {}
            }
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
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
        AnimatedVisibility(
            visible = state.initialUnauthorizedLoginRoute != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            val navController = rememberAnimatedNavController()
            state.initialUnauthorizedLoginRoute?.let {
                DappUnauthorizedLoginNavigationHost(
                    initialUnauthorizedLoginRoute = it,
                    navController = navController,
                    finishDappLogin = onBackClick,
                    showSuccessDialog = showSuccessDialog,
                    sharedViewModel = viewModel
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
