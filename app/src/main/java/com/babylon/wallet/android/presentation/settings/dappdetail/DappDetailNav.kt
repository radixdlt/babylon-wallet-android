package com.babylon.wallet.android.presentation.settings.dappdetail

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
import com.babylon.wallet.android.presentation.settings.personaedit.ROUTE_EDIT_PERSONA
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_DAPP_ADDRESS = "dapp_definition_address"

const val ROUTE_DAPP_DETAIL = "settings_dapp_detail/{$ARG_DAPP_ADDRESS}"

internal class DappDetailScreenArgs(val dappDefinitionAddress: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_DAPP_ADDRESS]) as String
    )
}

fun NavController.dappDetailScreen(dappDefinitionAddress: String) {
    navigate("settings_dapp_detail/$dappDefinitionAddress")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.dappDetailScreen(
    onBackClick: () -> Unit,
    onEditPersona: (String, String) -> Unit
) {
    composable(
        route = ROUTE_DAPP_DETAIL,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            when (targetState.destination.route) {
                ROUTE_EDIT_PERSONA -> null
                else -> slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
            }
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        },
        arguments = listOf(
            navArgument(ARG_DAPP_ADDRESS) {
                type = NavType.StringType
            }
        )
    ) {
        DappDetailScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onEditPersona = onEditPersona
        )
    }
}
