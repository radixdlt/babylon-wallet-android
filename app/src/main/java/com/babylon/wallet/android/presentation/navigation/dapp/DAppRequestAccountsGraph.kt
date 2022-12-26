package com.babylon.wallet.android.presentation.navigation.dapp

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.babylon.wallet.android.MainActivity
import com.babylon.wallet.android.MainViewModel
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.dapp.account.ChooseAccountsScreen
import com.babylon.wallet.android.presentation.dapp.account.ChooseAccountsViewModel
import com.babylon.wallet.android.presentation.dapp.completion.DAppCompletionScreen
import com.babylon.wallet.android.presentation.navigation.Screen
import com.google.accompanist.navigation.animation.composable

@OptIn(ExperimentalAnimationApi::class, ExperimentalLifecycleComposeApi::class)
fun NavGraphBuilder.dAppRequestAccountsGraph(
    navController: NavController
) {
    navigation(
        startDestination = Screen.ChooseAccountsDestination.route,
        route = Screen.RequestAccountsDestination.route
    ) {
        composable(
            route = Screen.ChooseAccountsDestination.route
        ) {
            val mainViewModel = ViewModelProvider(LocalContext.current as MainActivity)[MainViewModel::class.java]
            val viewModel = hiltViewModel<ChooseAccountsViewModel>()
            viewModel.setAccountsRequest(
                request = (mainViewModel.incomingRequest as MessageFromDataChannel.IncomingRequest.AccountsRequest)
            )

            ChooseAccountsScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.navigateUp()
                },
                exitRequestFlow = {
                    navController.navigateUp()
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
