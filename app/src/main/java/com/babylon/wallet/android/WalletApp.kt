package com.babylon.wallet.android

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.dapp.authorized.login.dAppLoginAuthorized
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.dAppLoginUnauthorized
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.main.MainEvent
import com.babylon.wallet.android.presentation.main.MainViewModel
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.transaction.transactionApproval
import com.babylon.wallet.android.presentation.ui.composables.status.transaction.transactionStatusDialog
import com.babylon.wallet.android.presentation.ui.composables.status.transaction.transactionStatusDialogShown
import com.babylon.wallet.android.presentation.ui.composables.status.dapp.dappInteractionDialog
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@OptIn(ExperimentalAnimationApi::class)
@Composable
@Suppress("ModifierMissing")
fun WalletApp(
    mainViewModel: MainViewModel,
    onCloseApp: () -> Unit
) {
    val navController = rememberAnimatedNavController()
    val appEventBus = rememberAppEventBus()
    NavigationHost(
        startDestination = MAIN_ROUTE,
        navController = navController,
        mainUiState = mainViewModel.state,
        onCloseApp = onCloseApp
    )
    LaunchedEffect(Unit) {
        mainViewModel.oneOffEvent.collect { event ->
            when (event) {
                is MainEvent.IncomingRequestEvent -> {
                    when (val incomingRequest = event.request) {
                        is MessageFromDataChannel.IncomingRequest.TransactionRequest -> {
                            navController.transactionApproval(
                                requestId = incomingRequest.requestId
                            )
                        }

                        is MessageFromDataChannel.IncomingRequest.AuthorizedRequest -> {
                            navController.dAppLoginAuthorized(incomingRequest.interactionId)
                        }

                        is MessageFromDataChannel.IncomingRequest.UnauthorizedRequest -> {
                            navController.dAppLoginUnauthorized(incomingRequest.requestId)
                        }
                    }
                }

                is MainEvent.HandledUsePersonaAuthRequest -> {
                    navController.dappInteractionDialog(
                        requestId = event.requestId,
                        dAppName = event.dAppName
                    )
                }
            }
        }
    }

    HandleAppEvents(navController = navController, appEventBus = appEventBus)
    mainViewModel.observeP2PLinks.collectAsStateWithLifecycle(null)
}


@Composable
fun HandleAppEvents(navController: NavController, appEventBus: AppEventBus) {
    val appEvent by appEventBus.events.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(appEvent) {
        when (val event = appEvent) {
            is AppEvent.TransactionEvent -> {
                if (!navController.transactionStatusDialogShown()) {
                    navController.transactionStatusDialog(event)
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun rememberAppEventBus(): AppEventBus {
    val context = LocalContext.current.applicationContext
    val entryPoint = remember(context) {
        EntryPoints.get(context, WalletEntryPoint::class.java)
    }

    return entryPoint.appEventBus
}


@EntryPoint
@InstallIn(SingletonComponent::class)
interface WalletEntryPoint {
    val appEventBus: AppEventBus
}
