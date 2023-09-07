package com.babylon.wallet.android.presentation.account.accountpreference

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentScope
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

internal class AccountPreferencesArgs(val address: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(checkNotNull(savedStateHandle[ARG_ADDRESS]) as String)
}

fun NavController.accountPreferences(address: String) {
    navigate("account_preference_route/$address") {
        launchSingleTop = true
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.accountPreferencesScreen(onBackClick: () -> Unit) {
    composable(
        route = "account_preference_route/{$ARG_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_ADDRESS) { type = NavType.StringType }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
        }
    ) {
        AccountPreferenceScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
