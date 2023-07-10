package com.babylon.wallet.android.presentation.settings.account

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
internal const val ARG_ADDRESS = "arg_address"

internal class AccountSettingsArgs(val address: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(checkNotNull(savedStateHandle[ARG_ADDRESS]) as String)
}

fun NavController.accountSettings(address: String) {
    navigate("account_settings_route/$address")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.accountSettings(onBackClick: () -> Unit, onSettingClick: (AccountSettingItem, String) -> Unit) {
    composable(
        route = "account_settings_route/{$ARG_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_ADDRESS) { type = NavType.StringType }
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
            onSettingClick = onSettingClick
        )
    }
}
