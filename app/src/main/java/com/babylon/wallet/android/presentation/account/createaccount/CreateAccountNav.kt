package com.babylon.wallet.android.presentation.account.createaccount

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
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.ARG_REQUEST_SOURCE
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.babylon.wallet.android.utils.Constants
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
const val ARG_NETWORK_URL = "arg_network_url"

@VisibleForTesting
const val ARG_NETWORK_ID = "arg_network_id"

@VisibleForTesting
const val ARG_SWITCH_NETWORK = "arg_switch_network"

const val ROUTE_CREATE_ACCOUNT = "create_account_route" +
    "?$ARG_REQUEST_SOURCE={$ARG_REQUEST_SOURCE}" +
    "&$ARG_NETWORK_URL={$ARG_NETWORK_URL}" +
    "&$ARG_NETWORK_ID={$ARG_NETWORK_ID}" +
    "&$ARG_SWITCH_NETWORK={$ARG_SWITCH_NETWORK}"

internal class CreateAccountNavArgs(
    val requestSource: CreateAccountRequestSource?,
    val networkUrlEncoded: String?,
    val networkId: Int,
    val switchNetwork: Boolean?,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<CreateAccountRequestSource>(
            ARG_REQUEST_SOURCE
        ),
        savedStateHandle.get<String>(ARG_NETWORK_URL),
        checkNotNull(savedStateHandle.get<Int>(ARG_NETWORK_ID)),
        savedStateHandle.get<Boolean>(ARG_SWITCH_NETWORK)
    )
}

fun NavController.createAccountScreen(
    requestSource: CreateAccountRequestSource = CreateAccountRequestSource.FirstTime,
    networkUrl: String? = null,
    networkId: Int = Constants.USE_CURRENT_NETWORK,
    switchNetwork: Boolean? = null,
    navOptions: NavOptions? = null
) {
    var route = "create_account_route?$ARG_REQUEST_SOURCE=$requestSource"
    networkUrl?.let {
        route += "&$ARG_NETWORK_URL=$it"
    }
    networkId.let {
        route += "&$ARG_NETWORK_ID=$it"
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
    onBackClick: () -> Unit,
    onContinueClick: (accountId: String, requestSource: CreateAccountRequestSource?) -> Unit,
    onAddLedgerDevice: (Int) -> Unit
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
            navArgument(ARG_NETWORK_ID) {
                type = NavType.IntType
                defaultValue = Constants.USE_CURRENT_NETWORK
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
            onContinueClick = onContinueClick,
            onAddLedgerDevice = onAddLedgerDevice
        )
    }
}
