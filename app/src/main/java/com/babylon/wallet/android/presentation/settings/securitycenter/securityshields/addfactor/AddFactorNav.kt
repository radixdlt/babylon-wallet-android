package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.addfactor

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog
import timber.log.Timber

const val ROUTE_ADD_FACTOR = "add_factor"

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

fun NavGraphBuilder.addFactorScreen(
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
            toFactorSetup = { kind ->
                Timber.d("Selected kind: $kind")
//                when (kind) {
//                    FactorSourceKind.DEVICE -> TODO()
//                    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> TODO()
//                    FactorSourceKind.OFF_DEVICE_MNEMONIC -> TODO()
//                    FactorSourceKind.TRUSTED_CONTACT -> TODO()
//                    FactorSourceKind.SECURITY_QUESTIONS -> TODO()
//                    FactorSourceKind.ARCULUS_CARD -> TODO()
//                    FactorSourceKind.PASSWORD -> TODO()
//                }
            }
        )
    }
}
