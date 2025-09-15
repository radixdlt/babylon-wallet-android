package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.accounts

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.ROUTE_APPLY_SHIELD_GRAPH
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.ApplyShieldSharedViewModel
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.personas.choosePersonas

const val ROUTE_CHOOSE_ACCOUNTS = "choose_accounts"

fun NavController.chooseAccounts() {
    navigate(ROUTE_CHOOSE_ACCOUNTS)
}

fun NavGraphBuilder.chooseAccounts(
    navController: NavController
) {
    composable(
        route = ROUTE_CHOOSE_ACCOUNTS,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        val parentEntry = remember(it) { navController.getBackStackEntry(ROUTE_APPLY_SHIELD_GRAPH) }
        val sharedVM = hiltViewModel<ApplyShieldSharedViewModel>(parentEntry)

        ChooseAccountsScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onSelected = { addresses ->
                sharedVM.onAccountsSelected(addresses)
                navController.choosePersonas(mustSelectAtLeastOne = sharedVM.mustSelectAtLeastOnePersona)
            }
        )
    }
}
