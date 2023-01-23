package com.babylon.wallet.android

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.createaccount.ROUTE_CREATE_ACCOUNT
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.transaction.transactionApproval
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.pager.ExperimentalPagerApi
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

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
                        is MessageFromDataChannel.IncomingRequest.AccountsRequest -> {
                            navController.navigate(
                                route = Screen.RequestAccountsDestination.routeWithArgs(
                                    incomingRequest.requestId
                                )
                            )
                        }
                        MessageFromDataChannel.IncomingRequest.None -> {
                        }
                        MessageFromDataChannel.IncomingRequest.ParsingError -> {
                            Timber.d("Failed to parse incoming request")
                        }
                        is MessageFromDataChannel.IncomingRequest.TransactionItem -> {
                            navController.transactionApproval(incomingRequest.requestId)
                        }
                        MessageFromDataChannel.IncomingRequest.Unknown -> {}
                        is MessageFromDataChannel.IncomingRequest.PersonaRequest -> {
                        }
                    }
                }
            }
        }
    }
}
