package com.babylon.wallet.android.presentation.settings.personadetail

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
import com.babylon.wallet.android.presentation.settings.dappdetail.ROUTE_DAPP_DETAIL
import com.babylon.wallet.android.presentation.settings.personaedit.ROUTE_EDIT_PERSONA
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_PERSONA_ADDRESS = "persona_address"

const val ROUTE_PERSONA_DETAIL = "persona_detail/{$ARG_PERSONA_ADDRESS}"

internal class PersonaDetailScreenArgs(val personaAddress: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_PERSONA_ADDRESS]) as String
    )
}

fun NavController.personaDetailScreen(personaAddress: String) {
    navigate("persona_detail/$personaAddress")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.personaDetailScreen(
    onBackClick: () -> Unit,
    onPersonaEdit: (String) -> Unit,
    onDappClick: (String) -> Unit,
    goToTransactionApproval: (String) -> Unit
) {
    composable(
        route = ROUTE_PERSONA_DETAIL,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            when (targetState.destination.route) {
                ROUTE_EDIT_PERSONA, ROUTE_DAPP_DETAIL -> null
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
            navArgument(ARG_PERSONA_ADDRESS) {
                type = NavType.StringType
            }
        )
    ) {
        PersonaDetailScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onEditPersona = onPersonaEdit,
            onDappClick = onDappClick,
            goToTransactionApproval = goToTransactionApproval
        )
    }
}
