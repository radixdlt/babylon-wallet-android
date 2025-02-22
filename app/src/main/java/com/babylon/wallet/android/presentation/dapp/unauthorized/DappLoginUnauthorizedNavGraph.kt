package com.babylon.wallet.android.presentation.dapp.unauthorized

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.account.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime.oneTimeChooseAccounts
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.ROUTE_DAPP_LOGIN_UNAUTHORIZED_SCREEN
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.dAppLoginUnauthorized
import com.babylon.wallet.android.presentation.dapp.unauthorized.personaonetime.oneTimeChoosePersona
import com.babylon.wallet.android.presentation.settings.personas.createpersona.CreatePersonaRequestSource
import com.babylon.wallet.android.presentation.settings.personas.createpersona.createPersonaScreen
import com.babylon.wallet.android.presentation.settings.personas.createpersona.personaInfoScreen
import com.babylon.wallet.android.presentation.settings.personas.personaedit.personaEditScreen

fun NavGraphBuilder.dappLoginUnauthorizedNavGraph(navController: NavController) {
    navigation(startDestination = ROUTE_DAPP_LOGIN_UNAUTHORIZED_SCREEN, route = ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH) {
        dAppLoginUnauthorized(
            navController,
            onNavigateToChooseAccount = { unauthorizedRequestInteractionId, numberOfAccounts, isExactAccountsCount ->
                navController.oneTimeChooseAccounts(
                    unauthorizedRequestInteractionId = unauthorizedRequestInteractionId,
                    numberOfAccounts = numberOfAccounts,
                    isExactAccountsCount = isExactAccountsCount
                )
            },
            onNavigateToOneTimePersonaData = {
                navController.oneTimeChoosePersona(it, false)
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH, true)
            }
        )

        oneTimeChooseAccounts(
            exitRequestFlow = {
                navController.popBackStack()
            },
            onAccountCreationClick = {
                navController.createAccountScreen(CreateAccountRequestSource.ChooseAccount)
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH, true)
            },
            onNavigateToChoosePersonaOnetime = {
                navController.oneTimeChoosePersona(it, true)
            },
            navController = navController
        )

        oneTimeChoosePersona(
            navController = navController,
            onEdit = {
                navController.personaEditScreen(it.personaAddress, it.requiredPersonaFields)
            },
            onBackClick = {
                navController.navigateUp()
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH, true)
            },
            onCreatePersona = { isFirstPersonaCreated ->
                if (isFirstPersonaCreated) {
                    navController.createPersonaScreen(CreatePersonaRequestSource.DappRequest)
                } else {
                    navController.personaInfoScreen(CreatePersonaRequestSource.DappRequest)
                }
            },
            onLoginFlowCancelled = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH, true)
            }
        )
    }
}
