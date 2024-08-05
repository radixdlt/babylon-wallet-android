package com.babylon.wallet.android.presentation.settings.personas.personadetail

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.ROUTE_DAPP_DETAIL
import com.babylon.wallet.android.presentation.settings.personas.personaedit.ROUTE_EDIT_PERSONA
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.DApp

@VisibleForTesting
internal const val ARG_PERSONA_ADDRESS = "persona_address"

const val ROUTE_PERSONA_DETAIL = "persona_detail/{$ARG_PERSONA_ADDRESS}"

internal class PersonaDetailScreenArgs(val personaAddress: IdentityAddress) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        IdentityAddress.init(checkNotNull(savedStateHandle[ARG_PERSONA_ADDRESS]))
    )
}

fun NavController.personaDetailScreen(personaAddress: IdentityAddress) {
    navigate("persona_detail/${personaAddress.string}")
}

fun NavGraphBuilder.personaDetailScreen(
    onBackClick: () -> Unit,
    onPersonaEdit: (IdentityAddress) -> Unit,
    onDAppClick: (DApp) -> Unit
) {
    composable(
        route = ROUTE_PERSONA_DETAIL,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            when (targetState.destination.route) {
                ROUTE_EDIT_PERSONA, ROUTE_DAPP_DETAIL -> null
                else -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            }
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popEnterTransition = {
            null
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
            onDAppClick = onDAppClick
        )
    }
}
