package com.babylon.wallet.android.presentation.settings.accountsecurity.accountrecoveryscan.scan

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

@VisibleForTesting
internal const val ARG_FACTOR_SOURCE_ID = "arg_factor_source_id"
internal const val ARG_IS_OLYMPIA = "arg_is_olympia"

private const val ROUTE = "account_recovery_scan?" +
    "$ARG_FACTOR_SOURCE_ID={$ARG_FACTOR_SOURCE_ID}" +
    "&$ARG_IS_OLYMPIA={$ARG_IS_OLYMPIA}"

internal class AccountRecoveryScanArgs(
    val factorSourceId: String?,
    val isOlympia: Boolean?
) {
    constructor(savedStateHandle: androidx.lifecycle.SavedStateHandle) : this(
        savedStateHandle.get<String>(ARG_FACTOR_SOURCE_ID),
        savedStateHandle.get<Boolean>(ARG_IS_OLYMPIA),
    )
}

fun NavController.accountRecoveryScan(
    factorSourceId: String? = null,
    isOlympia: Boolean = false
) {
    navigate(
        route = "account_recovery_scan?" +
            "$ARG_FACTOR_SOURCE_ID=$factorSourceId" +
            "&$ARG_IS_OLYMPIA=$isOlympia"
    )
}

fun NavGraphBuilder.accountRecoveryScan(
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
    ) {
        AccountRecoveryScanScreen(
            onBackClick = onBackClick,
            viewModel = hiltViewModel(),
            onRecoveryComplete = onRecoveryComplete
        )
    }
}
