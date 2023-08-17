package com.babylon.wallet.android

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.dapp.authorized.login.dAppLoginAuthorized
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.dAppLoginUnauthorized
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.main.MainEvent
import com.babylon.wallet.android.presentation.main.MainViewModel
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.navigation.PriorityRoutes
import com.babylon.wallet.android.presentation.status.dapp.dappInteractionDialog
import com.babylon.wallet.android.presentation.status.transaction.transactionStatusDialog
import com.babylon.wallet.android.presentation.transaction.transactionApproval
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.utils.AppEvent
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalAnimationApi::class)
@Composable
@Suppress("ModifierMissing")
fun WalletApp(
    mainViewModel: MainViewModel,
    onCloseApp: () -> Unit
) {
    val navController = rememberAnimatedNavController()
    var showNotSecuredDialog by remember { mutableStateOf(false) }
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
                            navController.dAppLoginUnauthorized(incomingRequest.interactionId)
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        mainViewModel.appNotSecureEvent.collect {
            showNotSecuredDialog = true
        }
    }

    HandleStatusEvents(
        navController = navController,
        statusEvents = mainViewModel.statusEvents
    )
    ObserveHighPriorityScreens(
        navController = navController,
        onLowPriorityScreen = mainViewModel::onLowPriorityScreen,
        onHighPriorityScreen = mainViewModel::onHighPriorityScreen
    )
    mainViewModel.observeP2PLinks.collectAsStateWithLifecycle(null)
    val context = LocalContext.current
    if (showNotSecuredDialog) {
        NotSecureAlertDialog(finish = {
            if (it) {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                ContextCompat.startActivity(context, intent, null)
            }
            showNotSecuredDialog = false
            onCloseApp()
        })
    }
}

@Composable
fun HandleStatusEvents(navController: NavController, statusEvents: Flow<AppEvent.Status>) {
    LaunchedEffect(Unit) {
        statusEvents.collect { event ->
            when (event) {
                is AppEvent.Status.Transaction -> {
                    navController.transactionStatusDialog(event)
                }

                is AppEvent.Status.DappInteraction -> {
                    navController.dappInteractionDialog(event)
                }
            }
        }
    }
}

@Composable
fun ObserveHighPriorityScreens(
    navController: NavController,
    onLowPriorityScreen: () -> Unit,
    onHighPriorityScreen: () -> Unit
) {
    LaunchedEffect(Unit) {
        navController.currentBackStackEntryFlow.collect { entry ->
            if (PriorityRoutes.isHighPriority(entry = entry)) {
                onHighPriorityScreen()
            } else {
                onLowPriorityScreen()
            }
        }
    }
}
