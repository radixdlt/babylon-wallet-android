package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.addfactor

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.factorsready.factorsReady
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.regularaccess.regularAccess

private const val ROUTE_ADD_FACTOR = "add_factor"

private const val ARG_MODE = "arg_mode"

data class AddFactorScreenArgs(
    val mode: Mode
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        requireNotNull(savedStateHandle.get<Mode>(ARG_MODE))
    )

    enum class Mode {
        HARDWARE_ONLY,
        ANY
    }

    companion object {

        fun from(savedStateHandle: SavedStateHandle): AddFactorScreenArgs {
            return AddFactorScreenArgs(savedStateHandle)
        }
    }
}

fun NavController.addHardwareDevice() {
    navigate("$ROUTE_ADD_FACTOR?$ARG_MODE=${AddFactorScreenArgs.Mode.HARDWARE_ONLY}")
}

fun NavController.addAnyFactor() {
    navigate("$ROUTE_ADD_FACTOR?$ARG_MODE=${AddFactorScreenArgs.Mode.ANY}")
}

fun NavGraphBuilder.addFactor(
    navController: NavController
) {
    composable(
        route = "$ROUTE_ADD_FACTOR?$ARG_MODE={$ARG_MODE}",
        arguments = listOf(
            navArgument(ARG_MODE) {
                type = NavType.EnumType(
                    AddFactorScreenArgs.Mode::class.java
                )
            }
        ),
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        AddFactorScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onInfoClick = { glossaryItem -> navController.infoDialog(glossaryItem) },
            onAddAnotherFactor = { navController.addAnyFactor() },
            onAddHardwareDevice = { navController.addHardwareDevice() },
            toRegularAccess = { navController.regularAccess() },
            onFactorsReady = { navController.factorsReady() }
        )
    }
}
