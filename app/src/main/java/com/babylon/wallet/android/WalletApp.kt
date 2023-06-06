package com.babylon.wallet.android

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.dapp.authorized.login.dAppLoginAuthorized
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.dAppLoginUnauthorized
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.main.MainEvent
import com.babylon.wallet.android.presentation.main.MainViewModel
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.transaction.ROUTE_TRANSACTION_APPROVAL
import com.babylon.wallet.android.presentation.transaction.transactionApproval
import com.babylon.wallet.android.presentation.transactionstatus.transactionStatusDialog
import com.babylon.wallet.android.presentation.transfer.ROUTE_TRANSFER
import com.babylon.wallet.android.presentation.ui.composables.resultdialog.success.successBottomDialog
import com.babylon.wallet.android.utils.routeExist
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

@OptIn(ExperimentalAnimationApi::class)
@Composable
@Suppress("ModifierMissing")
fun WalletApp(
    mainViewModel: MainViewModel,
    onCloseApp: () -> Unit
) {
    val navController = rememberAnimatedNavController()
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
                    navController.successBottomDialog(
                        isFromTransaction = false,
                        requestId = event.requestId,
                        dAppName = event.dAppName
                    )
                }
                is MainEvent.TransactionStatusEvent -> {
                    val transferScreenExist = navController.routeExist(ROUTE_TRANSFER)
                    if (transferScreenExist) {
                        navController.popBackStack(ROUTE_TRANSFER, true)
                    } else {
                        navController.popBackStack(ROUTE_TRANSACTION_APPROVAL, true)
                    }
                    navController.transactionStatusDialog(
                        requestId = event.requestId
                    )
                }
            }
        }
    }
    mainViewModel.observeP2PLinks.collectAsStateWithLifecycle(null)
}
