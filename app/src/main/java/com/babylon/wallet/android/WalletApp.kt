package com.babylon.wallet.android

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.domain.model.messages.WalletAuthorizedRequest
import com.babylon.wallet.android.domain.model.messages.WalletUnauthorizedRequest
import com.babylon.wallet.android.presentation.accessfactorsources.derivepublickeys.derivePublicKeysDialog
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.getSignatures
import com.babylon.wallet.android.presentation.account.settings.delete.success.deletedAccountSuccess
import com.babylon.wallet.android.presentation.dapp.authorized.login.dAppLoginAuthorized
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.dAppLoginUnauthorized
import com.babylon.wallet.android.presentation.dialogs.address.addressDetails
import com.babylon.wallet.android.presentation.dialogs.dapp.dappInteractionDialog
import com.babylon.wallet.android.presentation.dialogs.preauthorization.preAuthorizationStatusDialog
import com.babylon.wallet.android.presentation.dialogs.transaction.transactionStatusDialog
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.main.MainViewModel
import com.babylon.wallet.android.presentation.mobileconnect.mobileConnect
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.navigation.PriorityRoutes
import com.babylon.wallet.android.presentation.rootdetection.ROUTE_ROOT_DETECTION
import com.babylon.wallet.android.presentation.transaction.transactionReview
import com.babylon.wallet.android.presentation.ui.composables.BDFSErrorDialog
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.FullScreen
import com.babylon.wallet.android.presentation.ui.composables.LocalDevBannerState
import com.babylon.wallet.android.presentation.ui.composables.LockScreenBackground
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.presentation.walletclaimed.navigateToClaimedByAnotherDevice
import com.babylon.wallet.android.utils.AppEvent
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.Flow

@Composable
fun WalletApp(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    onCloseApp: () -> Unit
) {
    val context = LocalContext.current
    val state by mainViewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    var showSecureFolderWarning by rememberSaveable { mutableStateOf(false) }
    if (state.isAppLocked) {
        FullScreen {
            LockScreenBackground()
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        NavigationHost(
            modifier = Modifier.fillMaxSize(),
            startDestination = MAIN_ROUTE,
            navController = navController,
            state = mainViewModel.state,
            onCloseApp = onCloseApp
        )
        LaunchedEffect(Unit) {
            mainViewModel.oneOffEvent.collect { event ->
                when (event) {
                    is MainViewModel.Event.IncomingRequestEvent -> {
                        if (event.request.needVerification) {
                            navController.mobileConnect(event.request.interactionId)
                            return@collect
                        }
                        when (val dappToWalletInteraction = event.request) {
                            is TransactionRequest -> {
                                navController.transactionReview(
                                    requestId = dappToWalletInteraction.interactionId
                                )
                            }

                            is WalletAuthorizedRequest -> {
                                navController.dAppLoginAuthorized(dappToWalletInteraction.interactionId)
                            }

                            is WalletUnauthorizedRequest -> {
                                navController.dAppLoginUnauthorized(dappToWalletInteraction.interactionId)
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(
            state.claimedByAnotherDeviceError,
            state.showDeviceRootedWarning
        ) {
            val claimedByAnotherDeviceError = state.claimedByAnotherDeviceError
            if (claimedByAnotherDeviceError != null) {
                navController.navigateToClaimedByAnotherDevice(claimedByAnotherDeviceError)
            } else if (state.showDeviceRootedWarning) {
                navController.navigate(ROUTE_ROOT_DETECTION)
            }
        }

        LaunchedEffect(Unit) {
            mainViewModel.secureFolderWarning.collect {
                showSecureFolderWarning = true
            }
        }
        HandleAccessFactorSourcesEvents(
            navController = navController,
            accessFactorSourcesEvents = mainViewModel.accessFactorSourcesEvents
        )
        HandleStatusEvents(
            navController = navController,
            statusEvents = mainViewModel.statusEvents
        )
        HandleAddressDetailsEvents(
            navController = navController,
            addressDetailsEvents = mainViewModel.addressDetailsEvents
        )
        HandleAccountDeletedEvent(
            navController = navController,
            accountDeletedEvents = mainViewModel.accountDeletedEvents
        )
        HandleDeletedAccountsDetectedEvent(
            viewModel = mainViewModel
        )
        ObserveHighPriorityScreens(
            navController = navController,
            onLowPriorityScreen = mainViewModel::onLowPriorityScreen,
            onHighPriorityScreen = mainViewModel::onHighPriorityScreen
        )
        mainViewModel.observeP2PLinks.collectAsStateWithLifecycle(null)
        if (state.showDeviceNotSecureDialog) {
            NotSecureAlertDialog(finish = {
                if (it) {
                    val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                    ContextCompat.startActivity(context, intent, null)
                }
                onCloseApp()
            })
        }
        if (showSecureFolderWarning) {
            BasicPromptAlertDialog(
                finish = {
                    showSecureFolderWarning = false
                },
                message = {
                    Text(text = stringResource(id = R.string.homePage_secureFolder_warning))
                },
                dismissText = null
            )
        }
        val olympiaErrorState = state.olympiaErrorState
        if (olympiaErrorState != null) {
            BackHandler {}
            BDFSErrorDialog(
                finish = {
                    if (!olympiaErrorState.isCountdownActive) {
                        mainViewModel.clearOlympiaError()
                    }
                },
                title = stringResource(id = R.string.homePage_profileOlympiaError_title),
                message = stringResource(id = R.string.homePage_profileOlympiaError_subtitle),
                state = olympiaErrorState
            )
        }
        state.dappRequestFailure?.let {
            BasicPromptAlertDialog(
                finish = {
                    mainViewModel.onInvalidRequestMessageShown()
                },
                titleText = stringResource(id = R.string.dAppRequest_validationOutcome_invalidRequestTitle),
                messageText = it.getMessage(),
                confirmText = stringResource(
                    id = R.string.common_ok
                ),
                dismissText = null
            )
        }
        if (state.showMobileConnectWarning) {
            BasicPromptAlertDialog(
                finish = {
                    mainViewModel.onMobileConnectWarningShown()
                },
                titleText = stringResource(id = R.string.mobileConnect_noProfileDialog_title),
                messageText = stringResource(id = R.string.mobileConnect_noProfileDialog_subtitle),
                confirmText = stringResource(id = R.string.common_ok),
                dismissText = null
            )
        }
        SyncStatusBarWithScreenChanges(navController)
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
            systemUiController.setStatusBarColor(
                color = Color.Transparent,
                darkIcons = !devBannerState.isVisible
            )
        }
    }
}

@Composable
private fun HandleAccessFactorSourcesEvents(
    navController: NavController,
    accessFactorSourcesEvents: Flow<AppEvent.AccessFactorSources>
) {
    LaunchedEffect(Unit) {
        accessFactorSourcesEvents.collect { event ->
            when (event) {
                AppEvent.AccessFactorSources.DerivePublicKeys -> navController.derivePublicKeysDialog()
                AppEvent.AccessFactorSources.GetSignatures -> navController.getSignatures()
                is AppEvent.AccessFactorSources.SelectLedgerOutcome -> {}
            }
        }
    }
}

@Composable
private fun HandleStatusEvents(
    navController: NavController,
    statusEvents: Flow<AppEvent.Status>
) {
    LaunchedEffect(Unit) {
        statusEvents.collect { event ->
            when (event) {
                is AppEvent.Status.Transaction -> {
                    navController.transactionStatusDialog(event)
                }

                is AppEvent.Status.DappInteraction -> {
                    navController.dappInteractionDialog(event)
                }

                is AppEvent.Status.PreAuthorization -> {
                    navController.preAuthorizationStatusDialog(event)
                }
            }
        }
    }
}

@Composable
private fun HandleAddressDetailsEvents(
    navController: NavController,
    addressDetailsEvents: Flow<AppEvent.AddressDetails>
) {
    LaunchedEffect(Unit) {
        addressDetailsEvents.collect { event ->
            navController.addressDetails(actionableAddress = event.address)
        }
    }
}

@Composable
private fun HandleAccountDeletedEvent(
    navController: NavController,
    accountDeletedEvents: Flow<AppEvent.AccountDeleted>
) {
    LaunchedEffect(Unit) {
        accountDeletedEvents.collect {
            navController.deletedAccountSuccess()
        }
    }
}

@Composable
private fun HandleDeletedAccountsDetectedEvent(
    viewModel: MainViewModel
) {
    var isAccountsPreviouslyDeletedDetected by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.accountsDetectedDeletedEvents.collect {
            isAccountsPreviouslyDeletedDetected = true
        }
    }

    if (isAccountsPreviouslyDeletedDetected) {
        BasicPromptAlertDialog(
            finish = { accepted ->
                if (accepted) {
                    isAccountsPreviouslyDeletedDetected = false
                }
            },
            titleText = stringResource(id = R.string.homePage_deletedAccountWarning_title),
            messageText = stringResource(id = R.string.homePage_deletedAccountWarning_message),
            dismissText = null,
            confirmText = stringResource(R.string.common_continue)
        )
    }
}

@Composable
private fun ObserveHighPriorityScreens(
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
