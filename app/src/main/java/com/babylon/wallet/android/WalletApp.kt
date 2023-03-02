package com.babylon.wallet.android

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.createaccount.ROUTE_CREATE_ACCOUNT
import com.babylon.wallet.android.presentation.dapp.accountonetime.chooseAccountsOneTime
import com.babylon.wallet.android.presentation.dapp.login.dAppLogin
import com.babylon.wallet.android.presentation.dapp.requestsuccess.requestSuccess
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.transaction.transactionApproval
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.pager.ExperimentalPagerApi
import kotlinx.coroutines.flow.Flow

@ExperimentalPagerApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
@Suppress("ModifierMissing")
fun WalletApp(
    initialAppState: InitialAppState,
    onDeleteProfile: () -> Unit,
    onDeleteProfileDeclined: () -> Unit,
    oneOffEvent: Flow<MainEvent>,
) {
    val navController = rememberAnimatedNavController()
    when (initialAppState) {
        InitialAppState.CreateAccount -> {
            NavigationHost(
                ROUTE_CREATE_ACCOUNT,
                navController
            )
        }
        InitialAppState.Dashboard -> {
            NavigationHost(
                Screen.WalletDestination.route,
                navController
            )
        }
        is InitialAppState.IncompatibleProfile -> {
            IncompatibleProfileContent(
                onDeleteProfile = onDeleteProfile,
                onDeleteProfileDeclined = onDeleteProfileDeclined,
                modifier = Modifier
                    .fillMaxSize()
                    .background(RadixTheme.colors.blue1)
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            )
        }
        InitialAppState.Onboarding
        -> {
            NavigationHost(
                Screen.OnboardingDestination.route,
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

@Composable
private fun IncompatibleProfileContent(
    onDeleteProfile: () -> Unit,
    onDeleteProfileDeclined: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    onDeleteProfile()
                } else {
                    onDeleteProfileDeclined()
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.wallet_data_incompatible),
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray1
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.for_this_preview_wallet_version),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(id = R.string.delete_wallet_data),
            confirmTextColor = RadixTheme.colors.red1
        )
    }
}
