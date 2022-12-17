package com.babylon.wallet.android.presentation.navigation.dapp

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.account.ChooseAccountsScreen
import com.babylon.wallet.android.presentation.dapp.completion.DAppCompletionScreen
import com.babylon.wallet.android.presentation.navigation.Screen

fun NavGraphBuilder.dAppRequestAccountsGraph(
    navController: NavController
) {
    navigation(
        startDestination = Screen.ChooseAccountsDestination.route,
        route = Screen.RequestAccountsDestination.route
    ) {
        composable(route = Screen.ChooseAccountsDestination.route) {
            ChooseAccountsScreen(
                viewModel = hiltViewModel(),
                onBackClick = {
                    navController.navigateUp()
                },
                onContinueClick = { dAppName ->
                    navController.navigate(Screen.ChooseAccountsCompleteDestination.routeWithArgs(dAppName)) {
                        popUpTo(Screen.ChooseAccountsDestination.route) { inclusive = true }
                    }
                },
                dismissErrorDialog = {
                    navController.navigateUp()
                }
            )
        }

        composable(
            route = Screen.ChooseAccountsCompleteDestination.route + "/{${Screen.ARG_DAPP_NAME}}",
            arguments = listOf(
                navArgument(Screen.ARG_DAPP_NAME) { type = NavType.StringType }
            )
        ) {
            DAppCompletionScreen(
                viewModel = hiltViewModel(),
                onContinueClick = {
                    navController.navigateUp()
                }
            )
        }
    }
}
