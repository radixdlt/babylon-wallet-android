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
internal const val ARG_DAPP_DEFINITION_ADDRESS = "dapp_definition_address"

const val ROUTE_SELECT_PERSONA = "select_persona/{$ARG_DAPP_DEFINITION_ADDRESS}"

internal class SelectPersonaArgs(val dappDefinitionAddress: AccountAddress) {
    constructor(savedStateHandle: SavedStateHandle) : this(AccountAddress.init(checkNotNull(savedStateHandle[ARG_DAPP_DEFINITION_ADDRESS])))
}

fun NavController.selectPersona(
    dappDefinitionAddress: AccountAddress
) {
    navigate("select_persona/${dappDefinitionAddress.string}")
}

@Suppress("LongParameterList")
fun NavGraphBuilder.selectPersona(
    navController: NavController,
    onBackClick: () -> Unit,
    onChooseAccounts: (Event.ChooseAccounts) -> Unit,
    onLoginFlowComplete: () -> Unit,
    createNewPersona: (Boolean) -> Unit,
    onDisplayPermission: (Event.DisplayPermission) -> Unit,
    onPersonaDataOngoing: (Event.PersonaDataOngoing) -> Unit,
    onPersonaDataOnetime: (Event.PersonaDataOnetime) -> Unit
) {
    composable(
        route = ROUTE_SELECT_PERSONA,
        arguments = listOf(
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
            onDisplayPermission = onDisplayPermission,
            onPersonaDataOngoing = onPersonaDataOngoing,
            onPersonaDataOnetime = onPersonaDataOnetime
        )
    }
}
