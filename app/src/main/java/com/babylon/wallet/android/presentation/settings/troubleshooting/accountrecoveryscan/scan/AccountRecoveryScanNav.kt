package com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.scan

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.toJson

@VisibleForTesting
internal const val ARG_FACTOR_SOURCE_ID = "arg_factor_source_id"
internal const val ARG_IS_OLYMPIA = "arg_is_olympia"

private const val ROUTE = "account_recovery_scan?" +
    "$ARG_FACTOR_SOURCE_ID={$ARG_FACTOR_SOURCE_ID}" +
    "&$ARG_IS_OLYMPIA={$ARG_IS_OLYMPIA}"

internal class AccountRecoveryScanArgs(
    val factorSourceId: FactorSourceId.Hash?,
    val isOlympia: Boolean?
) {
    constructor(savedStateHandle: androidx.lifecycle.SavedStateHandle) : this(
        savedStateHandle.get<String>(ARG_FACTOR_SOURCE_ID)?.let {
            FactorSourceId.Hash.fromJson(it)
        },
        savedStateHandle.get<Boolean>(ARG_IS_OLYMPIA),
    )
}

fun NavController.accountRecoveryScan(
    factorSourceId: FactorSourceId.Hash? = null,
    isOlympia: Boolean = false
) {
    navigate(
        route = "account_recovery_scan?" +
            "$ARG_FACTOR_SOURCE_ID=${factorSourceId?.toJson()}" +
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
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
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
