package com.babylon.wallet.android

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import com.babylon.wallet.android.domain.model.IncomingRequest
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.navigation.Screen
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.pager.ExperimentalPagerApi
import timber.log.Timber

@ExperimentalPagerApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WalletApp(
    showOnboarding: Boolean,
    hasProfile: Boolean,
    incomingRequest: IncomingRequest
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

    when (incomingRequest) {
        is IncomingRequest.AccountsRequest -> {
            navController.navigate(
                route = Screen.RequestAccountsDestination.routeWithArgs(
                    incomingRequest.requestId,
                    incomingRequest.isOngoing,
                    incomingRequest.requiresProofOfOwnership,
                    incomingRequest.numberOfAccounts
                )
            )
        }
        IncomingRequest.Empty -> { /* nothing */
        }
        IncomingRequest.SomeOtherRequest -> {
            // this should be replaced for new requests
        }
        IncomingRequest.ParsingError -> {
            Timber.d("Failed to parse incoming request")
        }
    }
}
