package com.babylon.wallet.android.presentation.settings.personadetail

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

@VisibleForTesting
internal const val ARG_PERSONA_ADDRESS = "persona_address"

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
    onDappClick: (String) -> Unit
) {
    composable(
        route = "persona_detail/{$ARG_PERSONA_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_PERSONA_ADDRESS) {
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
        PersonaDetailScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onEditPersona = onPersonaEdit,
            onDappClick = onDappClick
        )
    }
}
