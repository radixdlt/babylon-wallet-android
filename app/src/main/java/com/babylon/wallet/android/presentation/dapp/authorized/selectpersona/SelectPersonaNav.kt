package com.babylon.wallet.android.presentation.dapp.authorized.selectpersona

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
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.dapp.authorized.login.ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string

@VisibleForTesting
internal const val ARG_AUTHORIZED_REQUEST_INTERACTION_ID = "arg_authorized_request_interaction_id"

@VisibleForTesting
internal const val ARG_DAPP_DEFINITION_ADDRESS = "dapp_definition_address"

const val ROUTE_SELECT_PERSONA = "select_persona/{$ARG_AUTHORIZED_REQUEST_INTERACTION_ID}/{$ARG_DAPP_DEFINITION_ADDRESS}"

internal class SelectPersonaArgs(
    val authorizedRequestInteractionId: String,
    val dappDefinitionAddress: AccountAddress
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_AUTHORIZED_REQUEST_INTERACTION_ID]) as String,
        AccountAddress.init(checkNotNull(savedStateHandle[ARG_DAPP_DEFINITION_ADDRESS]))
    )
}

fun NavController.selectPersona(
    authorizedRequestInteractionId: String,
    dappDefinitionAddress: AccountAddress
) {
    navigate("select_persona/$authorizedRequestInteractionId/${dappDefinitionAddress.string}")
}

@Suppress("LongParameterList")
fun NavGraphBuilder.selectPersona(
    navController: NavController,
    onBackClick: () -> Unit,
    onChooseAccounts: (Event.NavigateToChooseAccounts) -> Unit,
    onLoginFlowComplete: () -> Unit,
    createNewPersona: (Boolean) -> Unit,
    onNavigateToOngoingAccounts: (Event.NavigateToOngoingAccounts) -> Unit,
    onNavigateToOngoingPersonaData: (Event.NavigateToOngoingPersonaData) -> Unit,
    onNavigateToOneTimePersonaData: (Event.NavigateToOneTimePersonaData) -> Unit
) {
    composable(
        route = ROUTE_SELECT_PERSONA,
        arguments = listOf(
            navArgument(ARG_AUTHORIZED_REQUEST_INTERACTION_ID) {
                type = NavType.StringType
            },
            navArgument(ARG_DAPP_DEFINITION_ADDRESS) {
                type = NavType.StringType
            }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        val parentEntry = remember(it) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH)
        }
        val sharedVM = hiltViewModel<DAppAuthorizedLoginViewModel>(parentEntry)
        SelectPersonaScreen(
            viewModel = hiltViewModel(),
            sharedViewModel = sharedVM,
            onBackClick = onBackClick,
            onChooseAccounts = onChooseAccounts,
            onLoginFlowComplete = onLoginFlowComplete,
            createNewPersona = createNewPersona,
            onNavigateToOngoingAccounts = onNavigateToOngoingAccounts,
            onNavigateToOngoingPersonaData = onNavigateToOngoingPersonaData,
            onNavigateToOneTimePersonaData = onNavigateToOneTimePersonaData
        )
    }
}
