package com.babylon.wallet.android.presentation.settings.personas.personadetail

import androidx.annotation.VisibleForTesting
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
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.FactorSourceId
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

@Suppress("LongParameterList")
fun NavGraphBuilder.personaDetailScreen(
    onBackClick: () -> Unit,
    onPersonaEdit: (IdentityAddress) -> Unit,
    onDAppClick: (DApp) -> Unit,
    onFactorSourceCardClick: (FactorSourceId) -> Unit,
    onApplyShieldClick: (AddressOfAccountOrPersona) -> Unit,
    onShieldClick: (AddressOfAccountOrPersona) -> Unit,
    onTimedRecoveryClick: (AddressOfAccountOrPersona) -> Unit
) {
    composable(
        route = ROUTE_PERSONA_DETAIL,
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
            navArgument(ARG_PERSONA_ADDRESS) {
                type = NavType.StringType
            }
        )
    ) {
        PersonaDetailScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onEditPersona = onPersonaEdit,
            onDAppClick = onDAppClick,
            onFactorSourceCardClick = onFactorSourceCardClick,
            onApplyShieldClick = onApplyShieldClick,
            onShieldClick = onShieldClick,
            onTimedRecoveryClick = onTimedRecoveryClick
        )
    }
}
