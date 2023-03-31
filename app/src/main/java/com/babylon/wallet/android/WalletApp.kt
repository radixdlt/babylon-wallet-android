package com.babylon.wallet.android

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.ROUTE_INCOMPATIBLE_PROFILE
import com.babylon.wallet.android.presentation.createaccount.ROUTE_CREATE_ACCOUNT
import com.babylon.wallet.android.presentation.dapp.authorized.login.dAppLoginAuthorized
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.dAppLoginUnauthorized
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.transaction.transactionApproval
import com.babylon.wallet.android.presentation.ui.composables.requestresult.success.requestResultSuccess
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.pager.ExperimentalPagerApi
import kotlinx.coroutines.flow.Flow

@ExperimentalPagerApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
@Suppress("ModifierMissing")
fun WalletApp(
    mainViewModel: MainViewModel,
    appNavigationState: AppNavigationState,
    oneOffEvent: Flow<MainEvent>,
) {
    val navController = rememberAnimatedNavController()
    when (appNavigationState) {
        AppNavigationState.CreateAccount -> {
            NavigationHost(
                mainViewModel = mainViewModel,
                startDestination = ROUTE_CREATE_ACCOUNT,
                navController = navController
            )
        }
        AppNavigationState.Wallet -> {
            NavigationHost(
                mainViewModel = mainViewModel,
                startDestination = Screen.WalletDestination.route,
                navController = navController
            )
        }
        is AppNavigationState.IncompatibleProfile -> {
            NavigationHost(
                mainViewModel = mainViewModel,
                startDestination = ROUTE_INCOMPATIBLE_PROFILE,
                navController = navController
            )
        }
        AppNavigationState.Onboarding -> {
            NavigationHost(
                mainViewModel = mainViewModel,
                startDestination = Screen.OnboardingDestination.route,
                navController = navController
            )
        }
        AppNavigationState.Init -> {
            // TODO this doesn't seem to escalate well and as documentation suggests
            //  we should better have fixed start destination, in our case the Wallet,
            //  and let the Wallet screen to decide where and when to navigate.
        }
    }

    LaunchedEffect(Unit) {
        oneOffEvent.collect { event ->
            when (event) {
                is MainEvent.IncomingRequestEvent -> {
                    when (val incomingRequest = event.request) {
                        is MessageFromDataChannel.IncomingRequest.TransactionRequest -> {
                            navController.transactionApproval(
                                requestId = incomingRequest.requestId
                            )
                        }
                        is MessageFromDataChannel.IncomingRequest.AuthorizedRequest -> {
                            navController.dAppLoginAuthorized(incomingRequest.requestId)
                        }
                        is MessageFromDataChannel.IncomingRequest.UnauthorizedRequest -> {
                            navController.dAppLoginUnauthorized(incomingRequest.requestId)
                        }
                    }
                }
                is MainEvent.HandledUsePersonaAuthRequest -> {
                    navController.requestResultSuccess(
                        requestId = event.requestId,
                        dAppName = event.dAppName
                    )
                }
            }
        }
    }
}
