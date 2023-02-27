package com.babylon.wallet.android.presentation.settings.dappdetail

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable
import rdx.works.profile.data.model.pernetwork.OnNetwork

@VisibleForTesting
internal const val ARG_DAPP_ADDRESS = "dapp_definition_address"

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
    onEditPersona: (OnNetwork.Persona) -> Unit
) {
    composable(
        route = "settings_dapp_detail/{$ARG_DAPP_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_DAPP_ADDRESS) {
                type = NavType.StringType
            }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            ExitTransition.None
        }
    ) {
        DappDetailScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onEditPersona = onEditPersona
        )
    }
}
