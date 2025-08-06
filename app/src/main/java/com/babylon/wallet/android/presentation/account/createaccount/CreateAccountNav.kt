package com.babylon.wallet.android.presentation.account.createaccount

import android.os.Build
import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.core.os.BundleCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.ARG_REQUEST_SOURCE
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.babylon.wallet.android.presentation.onboarding.eula.ROUTE_EULA_SCREEN
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.discriminant
import com.radixdlt.sargon.extensions.init

private const val ARG_NETWORK_ID_TO_SWITCH = "arg_network_id_to_switch"

const val ROUTE_CREATE_ACCOUNT = "create_account_route" +
    "?$ARG_REQUEST_SOURCE={$ARG_REQUEST_SOURCE}" +
    "&$ARG_NETWORK_ID_TO_SWITCH={$ARG_NETWORK_ID_TO_SWITCH}"

internal class CreateAccountNavArgs(
    val requestSource: CreateAccountRequestSource?,
    val networkIdToSwitch: NetworkId?
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<CreateAccountRequestSource>(ARG_REQUEST_SOURCE),
        savedStateHandle.get<Byte?>(ARG_NETWORK_ID_TO_SWITCH)?.let {
            NetworkId.init(discriminant = it.toUByte())
        },
    )
}

fun NavController.createAccountScreen(
    requestSource: CreateAccountRequestSource = CreateAccountRequestSource.FirstTimeWithCloudBackupDisabled,
    networkIdToSwitch: NetworkId? = null,
    popToRoute: String? = null
) {
    val route = "create_account_route" +
        "?$ARG_REQUEST_SOURCE=$requestSource" +
        "&$ARG_NETWORK_ID_TO_SWITCH=${networkIdToSwitch?.discriminant?.toByte()}"

    navigate(route = route) {
        if (requestSource == CreateAccountRequestSource.FirstTimeWithCloudBackupEnabled) {
            // at this point wallet navigated from ConnectCloudBackupScreen and
            // user has authenticated/authorized access to Drive therefore
            // do not navigate back to ConnectCloudBackupScreen but to EulaScreen
            popUpTo(route = ROUTE_EULA_SCREEN) {
                inclusive = false
            }
        }
        popToRoute?.let { route ->
            popUpTo(route) {
                inclusive = true
            }
        }
    }
}

fun NavGraphBuilder.createAccountScreen(
    onBackClick: () -> Unit,
    onContinueClick: (accountId: AccountAddress, requestSource: CreateAccountRequestSource?) -> Unit
) {
    markAsHighPriority(route = ROUTE_CREATE_ACCOUNT)
    composable(
        route = ROUTE_CREATE_ACCOUNT,
        arguments = listOf(
            navArgument(ARG_REQUEST_SOURCE) {
                type = NavType.EnumType(CreateAccountRequestSource::class.java)
                defaultValue = CreateAccountRequestSource.FirstTimeWithCloudBackupDisabled
            },
            navArgument(ARG_NETWORK_ID_TO_SWITCH) {
                type = OptionalNetworkIdParamType()
                nullable = true
            }
        ),
        enterTransition = {
            if (requiresHorizontalTransition(targetState.arguments)) {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
            } else {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
            }
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            if (requiresHorizontalTransition(initialState.arguments)) {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            } else {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
            }
        }
    ) {
        CreateAccountScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onContinueClick = onContinueClick
        )
    }
}

private class OptionalNetworkIdParamType : NavType<NetworkId>(
    isNullableAllowed = true
) {
    override fun get(bundle: Bundle, key: String): NetworkId? = if (bundle.containsKey(key)) {
        NetworkId.init(discriminant = bundle.getByte(key).toUByte())
    } else {
        null
    }

    override fun parseValue(value: String): NetworkId {
        return NetworkId.init(discriminant = value.toByte().toUByte())
    }

    override fun put(bundle: Bundle, key: String, value: NetworkId) {
        bundle.putByte(key, value.discriminant.toByte())
    }
}

private fun requiresHorizontalTransition(arguments: Bundle?): Boolean {
    arguments ?: return false
    val requestSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arguments.getSerializable(ARG_REQUEST_SOURCE, CreateAccountRequestSource::class.java)
    } else {
        BundleCompat.getSerializable(arguments, ARG_REQUEST_SOURCE, CreateAccountRequestSource::class.java)
    }
    return requestSource?.isFirstTime() ?: false
}
