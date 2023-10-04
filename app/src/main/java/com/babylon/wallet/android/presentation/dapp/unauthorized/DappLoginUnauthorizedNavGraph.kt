package com.babylon.wallet.android.presentation.dapp.unauthorized

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.account.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime.chooseAccountsOneTime
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.ROUTE_DAPP_LOGIN_UNAUTHORIZED_SCREEN
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.dAppLoginUnauthorized
import com.babylon.wallet.android.presentation.dapp.unauthorized.personaonetime.personaDataOnetimeUnauthorized
import com.babylon.wallet.android.presentation.settings.personas.createpersona.createPersonaScreen
import com.babylon.wallet.android.presentation.settings.personas.createpersona.personaInfoScreen
import com.babylon.wallet.android.presentation.settings.personas.personaedit.personaEditScreen
import com.google.accompanist.navigation.animation.navigation

@Suppress("LongMethod")
@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.dappLoginUnauthorizedNavGraph(navController: NavController) {
    navigation(
        startDestination = ROUTE_DAPP_LOGIN_UNAUTHORIZED_SCREEN,
        route = ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH
    ) {
        dAppLoginUnauthorized(
            navController,
            navigateToChooseAccount = { numberOfAccounts, isExactAccountsCount ->
                navController.chooseAccountsOneTime(numberOfAccounts, isExactAccountsCount)
            },
            navigateToOneTimePersonaData = {
                navController.personaDataOnetimeUnauthorized(it)
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH, true)
            }
        )
        chooseAccountsOneTime(
            exitRequestFlow = {
                navController.popBackStack()
            },
            dismissErrorDialog = {
                navController.popBackStack()
            },
            onAccountCreationClick = {
                navController.createAccountScreen(CreateAccountRequestSource.ChooseAccount)
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH, true)
            },
            onPersonaOnetime = {
                navController.personaDataOnetimeUnauthorized(it)
            },
            navController = navController
        )
        personaDataOnetimeUnauthorized(
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
                    navController.createPersonaScreen()
                } else {
                    navController.personaInfoScreen()
                }
            },
            navController = navController,
            onLoginFlowCancelled = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH, true)
            }
        )
    }
}
