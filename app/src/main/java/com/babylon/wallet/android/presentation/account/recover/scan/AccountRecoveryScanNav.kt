package com.babylon.wallet.android.presentation.account.recover.scan

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

@VisibleForTesting
internal const val ARG_FACTOR_SOURCE_ID = "factor_source_id"

internal const val ARG_IS_OLYMPIA = "is_olympia"

private const val ROUTE = "accountRecoveryScan?$ARG_FACTOR_SOURCE_ID={$ARG_FACTOR_SOURCE_ID}&$ARG_IS_OLYMPIA={$ARG_IS_OLYMPIA}"

internal class AccountRecoveryScanArgs(val factorSourceId: String?, val isOlympia: Boolean?) {
    constructor(savedStateHandle: androidx.lifecycle.SavedStateHandle) : this(
        savedStateHandle.get<String>(
            ARG_FACTOR_SOURCE_ID
        ),
        savedStateHandle.get<Boolean>(
            ARG_IS_OLYMPIA
        )
    )
}

fun NavController.accountRecoveryScan(factorSourceIdString: String? = null, isOlympia: Boolean = false) {
    navigate(route = "accountRecoveryScan?$ARG_FACTOR_SOURCE_ID=$factorSourceIdString&$ARG_IS_OLYMPIA=$isOlympia")
}

fun NavGraphBuilder.accountRecoveryScan(
    navController: NavController,
    onBackClick: () -> Unit,
    onRecoveryComplete: () -> Unit
) {
    markAsHighPriority(ROUTE)
    composable(
        route = ROUTE,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        },
        arguments = listOf(
            navArgument(ARG_FACTOR_SOURCE_ID) {
                defaultValue = null
                nullable = true
                type = NavType.StringType
            },
            navArgument(ARG_IS_OLYMPIA) {
                defaultValue = false
                type = NavType.BoolType
            }
        )
    ) { entry ->
        val viewModel = if (entry.arguments?.getString(ARG_FACTOR_SOURCE_ID) == null) {
            val parentEntry = remember(entry) {
                navController.previousBackStackEntry
            }
            checkNotNull(parentEntry, lazyMessage = { "Account recovery scan requires AccountRecoveryViewModel started by parent" })
            hiltViewModel<AccountRecoveryScanViewModel>(parentEntry)
        } else {
            hiltViewModel()
        }
        AccountRecoveryScanScreen(
            onBackClick = onBackClick,
            viewModel = viewModel,
            onRecoveryComplete = onRecoveryComplete
        )
    }
}
