package com.babylon.wallet.android.presentation.settings.backup

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
private const val ROUTE_RESTORE_MNEMONIC = "restore_mnemonic/{$ARG_FACTOR_SOURCE_ID}"

internal class RestoreMnemonicArgs(savedStateHandle: SavedStateHandle) {
    val factorSourceId: String = checkNotNull(savedStateHandle[ARG_FACTOR_SOURCE_ID])
}

fun NavController.restoreMnemonic(factorSourceId: String) {
    navigate(route = "restore_mnemonic/$factorSourceId")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.restoreMnemonicScreen(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_RESTORE_MNEMONIC,
        arguments = listOf(
            navArgument(ARG_FACTOR_SOURCE_ID) {
                type = NavType.StringType
                nullable = false
            }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
        }
    ) {
        RestoreMnemonicScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
