package com.babylon.wallet.android

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.ROUTE_INCOMPATIBLE_PROFILE
import com.babylon.wallet.android.presentation.createaccount.ROUTE_CREATE_ACCOUNT
import com.babylon.wallet.android.presentation.dapp.accountonetime.chooseAccountsOneTime
import com.babylon.wallet.android.presentation.dapp.login.dAppLogin
import com.babylon.wallet.android.presentation.dapp.requestsuccess.requestSuccess
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.transaction.transactionApproval
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.pager.ExperimentalPagerApi
import kotlinx.coroutines.flow.Flow

@ExperimentalPagerApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
@Suppress("ModifierMissing")
fun WalletApp(
    appNavigationState: AppNavigationState,
    oneOffEvent: Flow<MainEvent>,
) {
    val navController = rememberAnimatedNavController()
    when (appNavigationState) {
        AppNavigationState.CreateAccount -> {
            NavigationHost(
                startDestination = ROUTE_CREATE_ACCOUNT,
                navController = navController
            )
        }
        AppNavigationState.Wallet -> {
            NavigationHost(
                startDestination = Screen.WalletDestination.route,
                navController = navController
            )
        }
        is AppNavigationState.IncompatibleProfile -> {
            NavigationHost(
                startDestination = ROUTE_INCOMPATIBLE_PROFILE,
                navController = navController
            )
        }
        AppNavigationState.Onboarding -> {
            NavigationHost(
                startDestination = Screen.OnboardingDestination.route,
                navController = navController
            )
        }
        AppNavigationState.Init -> {
            // TODO this doesn't seem to escalate well and as documentation suggests
            //  we should better have fixed start destination and, in our case the Wallet,
            //  and let the Wallet screen to decide where and when to navigate.
        }
    }

    LaunchedEffect(Unit) {
        oneOffEvent.collect { event ->
            when (event) {
                is MainEvent.IncomingRequestEvent -> {
                    when (val incomingRequest = event.request) {
                        is MessageFromDataChannel.IncomingRequest.TransactionRequest -> {
                            navController.transactionApproval(incomingRequest.requestId)
                        }
                        is MessageFromDataChannel.IncomingRequest.AuthorizedRequest -> {
                            navController.dAppLogin(incomingRequest.requestId)
                        }
                        is MessageFromDataChannel.IncomingRequest.UnauthorizedRequest -> {
                            if (incomingRequest.oneTimeAccountsRequestItem != null) {
                                navController.chooseAccountsOneTime(incomingRequest.requestId)
                            }
                        }
                    }
                }
                is MainEvent.HandledUsePersonaAuthRequest -> {
                    navController.requestSuccess(event.dAppName)
                }
            }
        }
    }
}
