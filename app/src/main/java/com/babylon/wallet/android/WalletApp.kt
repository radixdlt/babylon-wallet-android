package com.babylon.wallet.android

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.dapp.authorized.login.dAppLoginAuthorized
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.dAppLoginUnauthorized
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.main.MainEvent
import com.babylon.wallet.android.presentation.main.MainViewModel
import com.babylon.wallet.android.presentation.main.OlympiaErrorState
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.navigation.PriorityRoutes
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.RestoreMnemonicsArgs
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.restoreMnemonics
import com.babylon.wallet.android.presentation.status.dapp.dappInteractionDialog
import com.babylon.wallet.android.presentation.status.transaction.transactionStatusDialog
import com.babylon.wallet.android.presentation.transaction.transactionReview
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.LocalDevBannerState
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.utils.AppEvent
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
@Suppress("ModifierMissing")
fun WalletApp(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    onCloseApp: () -> Unit
) {
    val context = LocalContext.current
    val state by mainViewModel.state.collectAsStateWithLifecycle()
    val navController = rememberAnimatedNavController()
    var showNotSecuredDialog by remember { mutableStateOf(false) }
    NavigationHost(
        modifier = modifier,
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
                            navController.transactionReview(
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
    SyncStatusBarWithScreenChanges(navController)

    LaunchedEffect(Unit) {
        mainViewModel.appNotSecureEvent.collect {
            showNotSecuredDialog = true
        }
    }
    LaunchedEffect(Unit) {
        mainViewModel.babylonMnemonicNeedsRecoveryEvent.collect {
            navController.restoreMnemonics(
                args = RestoreMnemonicsArgs.RestoreSpecificMnemonic(
                    factorSourceId = it.factorSourceID.body,
                    isMandatory = true
                )
            )
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
    val olympiaErrorState = state.olympiaErrorState
    if (olympiaErrorState != OlympiaErrorState.None) {
        BackHandler {}
        val confirmText = if (olympiaErrorState is OlympiaErrorState.Countdown) {
            stringResource(id = R.string.profileOlympiaError_okCountdown, olympiaErrorState.secondsLeft)
        } else {
            stringResource(
                id = R.string.common_ok
            )
        }
        BasicPromptAlertDialog(
            finish = {
                if (state.olympiaErrorState == OlympiaErrorState.CanDismiss) {
                    mainViewModel.clearOlympiaError()
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.profileOlympiaError_title),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.profileOlympiaError_subtitle),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = confirmText,
            dismissText = null
        )
    }
    state.dappVerificationError?.let {
        BasicPromptAlertDialog(
            finish = {
                mainViewModel.onInvalidRequestMessageShown()
            },
            title = {
                Text(
                    text = stringResource(id = R.string.dAppRequest_validationOutcome_invalidRequestTitle),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.dAppRequest_validationOutcome_invalidRequestMessage),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(
                id = R.string.common_ok
            ),
            dismissText = null
        )
    }
}

@Composable
private fun SyncStatusBarWithScreenChanges(navController: NavHostController) {
    val devBannerState = LocalDevBannerState.current
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(navController, devBannerState) {
        // When a new screen is appeared we might need to reset the status bar's icons appearance.
        // Each screen composable can override the darkIcons parameter (such as AccountScreen), since
        // their invocation comes later.
        navController.currentBackStackEntryFlow.collect {
            systemUiController.setStatusBarColor(color = Color.Transparent, darkIcons = !devBannerState.isVisible)
        }
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
