package com.babylon.wallet.android.presentation.account.settings.devsettings

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string

@VisibleForTesting
internal const val ARG_ACCOUNT_SETTINGS_ADDRESS = "arg_account_settings_address"

internal class DevSettingsArgs(
    val address: AccountAddress
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        AccountAddress.init(checkNotNull(savedStateHandle[ARG_ACCOUNT_SETTINGS_ADDRESS]) as String)
    )
}

fun NavController.devSettings(address: AccountAddress) {
    navigate("dev_account_settings_route/${address.string}")
}

fun NavGraphBuilder.devSettings(
    onBackClick: () -> Unit
) {
    composable(
        route = "dev_account_settings_route/{$ARG_ACCOUNT_SETTINGS_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_ACCOUNT_SETTINGS_ADDRESS) { type = NavType.StringType }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            null
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        },
        popEnterTransition = {
            EnterTransition.None
        }
    ) {
        DevSettingsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
