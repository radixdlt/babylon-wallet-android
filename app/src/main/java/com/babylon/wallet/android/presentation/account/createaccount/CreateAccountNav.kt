package com.babylon.wallet.android.presentation.account.createaccount

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.ARG_REQUEST_SOURCE
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.init

@VisibleForTesting
const val ARG_NETWORK_URL = "arg_network_url"

@VisibleForTesting
const val ARG_NETWORK_ID_TO_SWITCH = "arg_network_id_to_switch"

const val ROUTE_CREATE_ACCOUNT = "create_account_route" +
    "?$ARG_REQUEST_SOURCE={$ARG_REQUEST_SOURCE}" +
    "&$ARG_NETWORK_URL={$ARG_NETWORK_URL}" +
    "&$ARG_NETWORK_ID_TO_SWITCH={$ARG_NETWORK_ID_TO_SWITCH}"

internal class CreateAccountNavArgs(
    val requestSource: CreateAccountRequestSource?,
    val networkUrlEncoded: String?,
    val networkIdToSwitch: NetworkId?
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<CreateAccountRequestSource>(
            ARG_REQUEST_SOURCE
        ),
        savedStateHandle.get<String>(ARG_NETWORK_URL),
        savedStateHandle.get<Int?>(ARG_NETWORK_ID_TO_SWITCH)?.let { NetworkId.init(it.toUByte()) },
    )
}

fun NavController.createAccountScreen(
    requestSource: CreateAccountRequestSource = CreateAccountRequestSource.FirstTime,
    networkUrl: String? = null,
    networkIdToSwitch: NetworkId? = null,
    navOptions: NavOptions? = null
) {
    var route = "create_account_route?$ARG_REQUEST_SOURCE=$requestSource"
    networkUrl?.let {
        route += "&$ARG_NETWORK_URL=$it"
    }
    networkIdToSwitch?.let {
        route += "&$ARG_NETWORK_ID_TO_SWITCH=${it.value.toInt()}"
    }
    navigate(
        route = route,
        navOptions = navOptions
    )
}

fun NavGraphBuilder.createAccountScreen(
    onBackClick: () -> Unit,
    onContinueClick: (accountId: AccountAddress, requestSource: CreateAccountRequestSource?) -> Unit,
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
            navArgument(ARG_NETWORK_ID_TO_SWITCH) {
                type = NavType.IntType
                nullable = true
            }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
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
