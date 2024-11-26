package com.babylon.wallet.android.presentation.dapp.unauthorized.personaonetime

import android.net.Uri
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.DAppUnauthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH
import com.babylon.wallet.android.presentation.navigation.RequiredPersonaFieldsParameterType
import kotlinx.serialization.encodeToString

@VisibleForTesting
private const val ARG_REQUIRED_FIELDS = "required_fields"

@VisibleForTesting
private const val ARG_SHOW_BACK = "show_back"

internal class OneTimeChoosePersonaArgs(
    val requiredPersonaFields: RequiredPersonaFields,
    val showBack: Boolean
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_REQUIRED_FIELDS]) as RequiredPersonaFields,
        checkNotNull(savedStateHandle[ARG_SHOW_BACK])
    )
}

const val ONE_TIME_CHOOSE_PERSONA_ROUTE = "route_persona_data_onetime_unauthorized/{$ARG_REQUIRED_FIELDS}/{$ARG_SHOW_BACK}"

fun NavController.oneTimeChoosePersona(requiredPersonaFields: RequiredPersonaFields, showBack: Boolean) {
    val argument = Uri.encode(Serializer.kotlinxSerializationJson.encodeToString(requiredPersonaFields))
    navigate("route_persona_data_onetime_unauthorized/$argument/$showBack")
}

@Suppress("LongParameterList")
fun NavGraphBuilder.oneTimeChoosePersona(
    onEdit: (OneTimeChoosePersonaEvent.OnEditPersona) -> Unit,
    onBackClick: () -> Unit,
    onLoginFlowComplete: () -> Unit,
    onCreatePersona: (Boolean) -> Unit,
    navController: NavController,
    onLoginFlowCancelled: () -> Unit
) {
    composable(
        route = ONE_TIME_CHOOSE_PERSONA_ROUTE,
        arguments = listOf(
            navArgument(ARG_REQUIRED_FIELDS) {
                type = RequiredPersonaFieldsParameterType
            },
            navArgument(ARG_SHOW_BACK) {
                type = NavType.BoolType
            }
        ),
        enterTransition = {
            if (requiresHorizontalTransition(targetState.arguments)) {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
            } else {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
            }
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            if (requiresHorizontalTransition(initialState.arguments)) {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            } else {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
            }
        }
    ) { entry ->
        val parentEntry = remember(entry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH)
        }
        val sharedVM = hiltViewModel<DAppUnauthorizedLoginViewModel>(parentEntry)

        OneTimeChoosePersonaScreen(
            sharedViewModel = sharedVM,
            onEdit = onEdit,
            onBackClick = onBackClick,
            viewModel = hiltViewModel(),
            onLoginFlowComplete = onLoginFlowComplete,
            onCreatePersona = onCreatePersona,
            onLoginFlowCancelled = onLoginFlowCancelled
        )
    }
}

private fun requiresHorizontalTransition(arguments: Bundle?): Boolean {
    arguments ?: return false
    return arguments.getBoolean(ARG_SHOW_BACK)
}
