package com.babylon.wallet.android.presentation.createaccount

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.createaccount.confirmation.ARG_REQUEST_SOURCE
import com.babylon.wallet.android.presentation.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
const val ARG_NETWORK_URL = "arg_network_url"

@VisibleForTesting
const val ARG_NETWORK_NAME = "arg_network_name"

@VisibleForTesting
const val ARG_SWITCH_NETWORK = "arg_switch_network"

const val ROUTE_CREATE_ACCOUNT = "create_account_route" +
    "?$ARG_REQUEST_SOURCE={$ARG_REQUEST_SOURCE}" +
    "&$ARG_NETWORK_URL={$ARG_NETWORK_URL}" +
    "&$ARG_NETWORK_NAME={$ARG_NETWORK_NAME}" +
    "&$ARG_SWITCH_NETWORK={$ARG_SWITCH_NETWORK}"

internal class CreateAccountNavArgs(
    val requestSource: CreateAccountRequestSource?,
    val networkUrlEncoded: String?,
    val networkName: String?,
    val switchNetwork: Boolean?,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<CreateAccountRequestSource>(
            ARG_REQUEST_SOURCE
        ),
        savedStateHandle.get<String>(ARG_NETWORK_URL),
        savedStateHandle.get<String>(ARG_NETWORK_NAME),
        savedStateHandle.get<Boolean>(ARG_SWITCH_NETWORK)
    )
}

fun NavController.createAccountScreen(
    requestSource: CreateAccountRequestSource = CreateAccountRequestSource.FirstTime,
    networkUrl: String? = null,
    networkName: String? = null,
    switchNetwork: Boolean? = null,
    navOptions: NavOptions? = null
) {
    var route = "create_account_route?$ARG_REQUEST_SOURCE=$requestSource"
    networkUrl?.let {
        route += "&$ARG_NETWORK_URL=$it"
    }
    networkName?.let {
        route += "&$ARG_NETWORK_NAME=$it"
    }
    switchNetwork?.let {
        route += "&$ARG_SWITCH_NETWORK=$it"
    }
    navigate(
        route = route,
        navOptions = navOptions
    )
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.createAccountScreen(
    startDestination: String,
    onBackClick: () -> Unit,
    onContinueClick: (accountId: String, requestSource: CreateAccountRequestSource?) -> Unit,
    onCloseApp: () -> Unit,
    onAddLedgerDevice: () -> Unit
) {
    markAsHighPriority(route = ROUTE_CREATE_ACCOUNT)
    composable(
        route = ROUTE_CREATE_ACCOUNT,
        arguments = listOf(
            navArgument(ARG_REQUEST_SOURCE) {
                type = NavType.EnumType(CreateAccountRequestSource::class.java)
                defaultValue = CreateAccountRequestSource.FirstTime
            },
            navArgument(ARG_NETWORK_URL) {
                type = NavType.StringType
                nullable = true
            },
            navArgument(ARG_NETWORK_NAME) {
                type = NavType.StringType
                nullable = true
            },
            navArgument(ARG_SWITCH_NETWORK) {
                type = NavType.BoolType
                defaultValue = false
            }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
        }
    ) {
        CreateAccountScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            cancelable = startDestination != ROUTE_CREATE_ACCOUNT,
            onContinueClick = onContinueClick,
            onAddLedgerDevice = onAddLedgerDevice,
            onCloseApp = onCloseApp
        )
    }
}
