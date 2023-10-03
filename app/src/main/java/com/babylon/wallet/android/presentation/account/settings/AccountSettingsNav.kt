package com.babylon.wallet.android.presentation.account.settings

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_ACCOUNT_SETTINGS_ADDRESS = "arg_account_settings_address"
internal const val ARG_ACCOUNT_SETTINGS_NAME = "arg_account_settings_name"

internal class AccountSettingsArgs(
    val address: String,
    val name: String
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_ACCOUNT_SETTINGS_ADDRESS]) as String,
        checkNotNull(savedStateHandle[ARG_ACCOUNT_SETTINGS_NAME]) as String
    )
}

fun NavController.accountSettings(
    address: String,
    name: String
) {
    navigate("account_settings_route/$address/$name") {
        launchSingleTop
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.accountSettings(
    onBackClick: () -> Unit,
    onAccountSettingItemClick: (AccountSettingItem, address: String) -> Unit
) {
    composable(
        route = "account_settings_route/{$ARG_ACCOUNT_SETTINGS_ADDRESS}/{$ARG_ACCOUNT_SETTINGS_NAME}",
        arguments = listOf(
            navArgument(ARG_ACCOUNT_SETTINGS_ADDRESS) { type = NavType.StringType },
            navArgument(ARG_ACCOUNT_SETTINGS_NAME) { type = NavType.StringType }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            null
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
        },
        popEnterTransition = {
            EnterTransition.None
        }
    ) {
        AccountSettingsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onSettingItemClick = onAccountSettingItemClick
        )
    }
}
