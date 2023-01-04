@file:Suppress("TopLevelPropertyNaming")

package com.babylon.wallet.android.presentation.accountpreference

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
internal const val AddressArg = "address"

internal class AccountPreferencesArgs(val address: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(checkNotNull(savedStateHandle.get(AddressArg)) as String)
}

fun NavController.accountPreferences(address: String) {
    navigate("account_preference_route/$address")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.accountPreferencesScreen(onBackClick: () -> Unit) {
    composable(
        route = "account_preference_route/{$AddressArg}",
        arguments = listOf(
            navArgument(AddressArg) { type = NavType.StringType }
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
            onBackClick = onBackClick,
        )
    }
}
