package com.babylon.wallet.android.presentation.account.settings

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

@VisibleForTesting
internal const val ARG_ACCOUNT_SETTINGS_ADDRESS = "arg_account_settings_address"

internal class AccountSettingsArgs(
    val address: String
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_ACCOUNT_SETTINGS_ADDRESS]) as String
    )
}

fun NavController.accountSettings(
    address: String
) {
    navigate("account_settings_route/$address") {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.accountSettings(
    onBackClick: () -> Unit,
    onAccountSettingItemClick: (AccountSettingItem, address: String) -> Unit,
    onHideAccountClick: () -> Unit
) {
    composable(
        route = "account_settings_route/{$ARG_ACCOUNT_SETTINGS_ADDRESS}",
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
        AccountSettingsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onSettingItemClick = onAccountSettingItemClick,
            onHideAccountClick = onHideAccountClick
        )
    }
}
