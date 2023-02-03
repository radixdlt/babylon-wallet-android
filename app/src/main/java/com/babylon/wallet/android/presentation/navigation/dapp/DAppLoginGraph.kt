package com.babylon.wallet.android.presentation.navigation.dapp

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.createaccount.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.createpersona.createPersonaScreen
import com.babylon.wallet.android.presentation.dapp.account.chooseAccounts
import com.babylon.wallet.android.presentation.dapp.login.ROUTE_DAPP_LOGIN
import com.babylon.wallet.android.presentation.dapp.login.dAppLogin
import com.babylon.wallet.android.presentation.dapp.permission.dappPermission
import com.babylon.wallet.android.presentation.dapp.requestsuccess.requestSuccess

internal const val ARG_REQUEST_ID = "request_id"

const val ROUTE_DAPP_FLOW = "authorized_request_route/{$ARG_REQUEST_ID}"

fun NavController.dappLogin(requestId: String) {
    navigate("authorized_request_route/$requestId")
}

fun NavGraphBuilder.dAppLoginGraph(
    navController: NavController,
) {
    navigation(
        startDestination = ROUTE_DAPP_LOGIN,
        route = ROUTE_DAPP_FLOW,
        arguments = listOf(
            navArgument(ARG_REQUEST_ID) { type = NavType.StringType }
        )
    ) {
        dAppLogin(
            onBackClick = {
                navController.popBackStack()
            },
            navController = navController,
            onHandleOngoingAccounts = { event ->
                navController.dappPermission(event.numberOfAccounts, event.quantifier, event.oneTime)
            },
            onChooseAccounts = { event ->
                navController.chooseAccounts(event.numberOfAccounts, event.quantifier, event.oneTime)
            },
            createNewPersona = {
                navController.createPersonaScreen()
            },
            onLoginFlowComplete = { dappName ->
                navController.popBackStack(ROUTE_DAPP_FLOW, true)
                navController.requestSuccess(dappName)
            }
        )
        dappPermission(
            navController = navController,
            onChooseAccounts = { event ->
                navController.chooseAccounts(event.numberOfAccounts, event.quantifier, event.oneTime)
            }
        )
        chooseAccounts(
            navController = navController,
            onBackClick = {
                navController.navigateUp()
            },
            dismissErrorDialog = {
                navController.navigateUp()
            },
            onChooseAccounts = { event ->
                navController.chooseAccounts(event.numberOfAccounts, event.quantifier, event.oneTime)
            },
            onAccountCreationClick = {
                navController.createAccountScreen(CreateAccountRequestSource.ChooseAccount)
            },
            onLoginFlowComplete = { dappName ->
                navController.popBackStack(ROUTE_DAPP_FLOW, true)
                navController.requestSuccess(dappName)
            }
        )
        requestSuccess(onBackPress = {
            navController.popBackStack()
        })
    }
}
