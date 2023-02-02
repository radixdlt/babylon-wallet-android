package com.babylon.wallet.android

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.createaccount.ROUTE_CREATE_ACCOUNT
import com.babylon.wallet.android.presentation.dapp.unauthorizedaccount.chooseAccounts1
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.navigation.dapp.dappLogin
import com.babylon.wallet.android.presentation.transaction.transactionApproval
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.pager.ExperimentalPagerApi
import kotlinx.coroutines.flow.Flow

@ExperimentalPagerApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WalletApp(
    showOnboarding: Boolean,
    hasProfile: Boolean,
    oneOffEvent: Flow<MainEvent>,
) {
    val navController = rememberAnimatedNavController()

    if (showOnboarding) {
        NavigationHost(
            Screen.OnboardingDestination.route,
            navController
        )
    } else {
        if (hasProfile) {
            NavigationHost(
                Screen.WalletDestination.route,
                navController
            )
        } else {
            NavigationHost(
                ROUTE_CREATE_ACCOUNT,
                navController
            )
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
                            navController.dappLogin(incomingRequest.requestId)
                        }
                        is MessageFromDataChannel.IncomingRequest.UnauthorizedRequest -> {
                            if (incomingRequest.oneTimeAccountsRequestItem != null) {
                                navController.chooseAccounts1(incomingRequest.requestId)
                            }
                        }
                    }
                }
            }
        }
    }
}
