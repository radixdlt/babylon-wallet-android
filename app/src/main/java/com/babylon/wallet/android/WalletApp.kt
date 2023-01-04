package com.babylon.wallet.android

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.transaction.transactionApproval
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.pager.ExperimentalPagerApi
import timber.log.Timber

@ExperimentalPagerApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WalletApp(
    showOnboarding: Boolean,
    hasProfile: Boolean,
    incomingRequest: MessageFromDataChannel.IncomingRequest,
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
                Screen.CreateAccountDestination.route(),
                navController
            )
        }
    }

    LaunchedEffect(incomingRequest) {
        when (incomingRequest) {
            is MessageFromDataChannel.IncomingRequest.AccountsRequest -> {
                navController.navigate(
                    route = Screen.RequestAccountsDestination.routeWithArgs(
                        incomingRequest.requestId,
                        incomingRequest.isOngoing,
                        incomingRequest.requiresProofOfOwnership,
                        incomingRequest.numberOfAccounts
                    )
                )
            }
            MessageFromDataChannel.IncomingRequest.None -> {
            }
            MessageFromDataChannel.IncomingRequest.ParsingError -> {
                Timber.d("Failed to parse incoming request")
            }
            is MessageFromDataChannel.IncomingRequest.TransactionWriteRequest -> {
                navController.transactionApproval(incomingRequest.requestId)
            }
            MessageFromDataChannel.IncomingRequest.Unknown -> {}
        }
    }
}
