package com.babylon.wallet.android.presentation.dapp.authorized.verifyentities.accounts

import android.os.Bundle
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
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH
import com.babylon.wallet.android.presentation.dapp.authorized.verifyentities.ARG_AUTHORIZED_REQUEST_INTERACTION_ID
import com.babylon.wallet.android.presentation.dapp.authorized.verifyentities.ARG_CAN_NAVIGATE_BACK
import com.babylon.wallet.android.presentation.dapp.authorized.verifyentities.ARG_ENTITIES_FOR_PROOF
import com.babylon.wallet.android.presentation.dapp.authorized.verifyentities.EntitiesForProofWithSignatures
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val ROUTE = "verify_accounts_route"

fun NavController.verifyAccounts(
    walletAuthorizedRequestInteractionId: String,
    entitiesForProofWithSignatures: EntitiesForProofWithSignatures,
    canNavigateBack: Boolean
) {
    val requestedEntities = Json.encodeToString(entitiesForProofWithSignatures)
    navigate(route = "$ROUTE/$walletAuthorizedRequestInteractionId/$requestedEntities/$canNavigateBack")
}

fun NavGraphBuilder.verifyAccounts(
    navController: NavController,
    onVerificationFlowComplete: () -> Unit,
    onBackClick: () -> Unit
) {
    composable(
        route = "$ROUTE/{$ARG_AUTHORIZED_REQUEST_INTERACTION_ID}/{$ARG_ENTITIES_FOR_PROOF}/{$ARG_CAN_NAVIGATE_BACK}",
        arguments = listOf(
            navArgument(ARG_AUTHORIZED_REQUEST_INTERACTION_ID) { type = NavType.StringType },
            navArgument(ARG_ENTITIES_FOR_PROOF) { type = NavType.StringType },
            navArgument(ARG_CAN_NAVIGATE_BACK) { type = NavType.BoolType }
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
    ) { navBackStackEntry ->
        val parentEntry = remember(navBackStackEntry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH)
        }
        val sharedViewModel = hiltViewModel<DAppAuthorizedLoginViewModel>(parentEntry)

        VerifyAccountsScreen(
            viewModel = hiltViewModel(),
            sharedViewModel = sharedViewModel,
            onVerificationFlowComplete = onVerificationFlowComplete,
            onBackClick = onBackClick
        )
    }
}

private fun requiresHorizontalTransition(arguments: Bundle?): Boolean {
    arguments ?: return false
    return arguments.getBoolean(ARG_CAN_NAVIGATE_BACK)
}
