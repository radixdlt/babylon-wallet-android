package com.babylon.wallet.android.presentation.navigation.dapp

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.account.ChooseAccountsScreen
import com.babylon.wallet.android.presentation.dapp.completion.DAppCompletionScreen
import com.babylon.wallet.android.presentation.navigation.Screen
import com.google.accompanist.navigation.animation.composable

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.dAppRequestAccountsGraph(
    navController: NavController,
) {
    navigation(
        startDestination = Screen.ChooseAccountsDestination.route,
        route = Screen.RequestAccountsDestination.route + "/{${Screen.ARG_INCOMING_REQUEST_ID}}",
        arguments = listOf(
            navArgument(Screen.ARG_INCOMING_REQUEST_ID) { type = NavType.StringType }
        )
    ) {
        composable(
            route = Screen.ChooseAccountsDestination.route
        ) {
            ChooseAccountsScreen(
                viewModel = hiltViewModel(),
                onBackClick = {
                    navController.navigateUp()
                },
                exitRequestFlow = {
                    navController.navigateUp()
                },
                dismissErrorDialog = {
                    navController.navigateUp()
                },
                onAccountCreationClick = {
                    navController.navigate(
                        Screen.CreateAccountDestination.route()
                    )
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
