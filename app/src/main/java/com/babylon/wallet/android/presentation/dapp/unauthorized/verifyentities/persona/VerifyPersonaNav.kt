package com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities.persona

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.DAppUnauthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH
import com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities.ARG_CAN_NAVIGATE_BACK
import com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities.ARG_ENTITIES_FOR_PROOF
import com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities.ARG_UNAUTHORIZED_REQUEST_INTERACTION_ID
import com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities.EntitiesForProofWithSignatures
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val ROUTE = "verify_persona_route"

fun NavController.verifyPersona(
    walletUnauthorizedRequestInteractionId: String,
    entitiesForProofWithSignatures: EntitiesForProofWithSignatures,
    canNavigateBack: Boolean
) {
    val requestedEntities = Json.encodeToString(entitiesForProofWithSignatures)
    navigate(route = "$ROUTE/$walletUnauthorizedRequestInteractionId/$requestedEntities/$canNavigateBack")
}

fun NavGraphBuilder.verifyPersona(
    navController: NavController,
    onNavigateToVerifyAccounts: (String, EntitiesForProofWithSignatures) -> Unit,
    onVerificationFlowComplete: () -> Unit,
    onBackClick: () -> Unit
) {
    composable(
        route = "$ROUTE/{$ARG_UNAUTHORIZED_REQUEST_INTERACTION_ID}/{$ARG_ENTITIES_FOR_PROOF}/{$ARG_CAN_NAVIGATE_BACK}",
        arguments = listOf(
            navArgument(ARG_UNAUTHORIZED_REQUEST_INTERACTION_ID) { type = NavType.StringType },
            navArgument(ARG_ENTITIES_FOR_PROOF) { type = NavType.StringType },
            navArgument(ARG_CAN_NAVIGATE_BACK) { type = NavType.BoolType }
        ),
        enterTransition = { // TODO update transition similar to PersonaDataOneTimeNav
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
    ) { navBackStackEntry ->
        val parentEntry = remember(navBackStackEntry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH)
        }
        val sharedViewModel = hiltViewModel<DAppUnauthorizedLoginViewModel>(parentEntry)

        VerifyPersonaScreen(
            viewModel = hiltViewModel(),
            sharedViewModel = sharedViewModel,
            onNavigateToVerifyAccounts = onNavigateToVerifyAccounts,
            onVerificationFlowComplete = onVerificationFlowComplete,
            onBackClick = onBackClick
        )
    }
}
