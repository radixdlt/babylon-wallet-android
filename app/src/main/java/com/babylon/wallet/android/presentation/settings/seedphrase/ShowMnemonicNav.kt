package com.babylon.wallet.android.presentation.settings.seedphrase

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
internal const val ARG_FACTOR_SOURCE_ID = "factor_source_id"

const val ROUTE_SETTINGS_SHOW_MNEMONIC = "settings_show_mnemonic/{$ARG_FACTOR_SOURCE_ID}"

internal class ShowMnemonicArgs(val factorSourceId: String?) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle[ARG_FACTOR_SOURCE_ID]
    )
}

fun NavController.settingsShowMnemonic(factorSourceId: String? = null) {
    navigate("settings_show_mnemonic/$factorSourceId")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.settingsShowMnemonic(
    onBackClick: () -> Unit,
    onNavigateToRecoverMnemonic: (String) -> Unit
) {
    composable(
        route = ROUTE_SETTINGS_SHOW_MNEMONIC,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        },
        arguments = listOf(
            navArgument(ARG_FACTOR_SOURCE_ID) {
                type = NavType.StringType
                nullable = true
            }
        )
    ) {
        ShowMnemonicScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onNavigateToRecoverMnemonic = onNavigateToRecoverMnemonic
        )
    }
}
