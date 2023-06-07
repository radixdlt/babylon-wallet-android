package com.babylon.wallet.android.presentation.dapp.unauthorized

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.createpersona.createPersonaScreen
import com.babylon.wallet.android.presentation.createpersona.personaInfoScreen
import com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime.chooseAccountsOneTime
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.ROUTE_DAPP_LOGIN_UNAUTHORIZED_SCREEN
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.dAppLoginUnauthorized
import com.babylon.wallet.android.presentation.dapp.unauthorized.personaonetime.personaDataOnetimeUnauthorized
import com.babylon.wallet.android.presentation.settings.personaedit.personaEditScreen
import com.babylon.wallet.android.presentation.ui.composables.resultdialog.success.successBottomDialog
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
            }
        ) {
            navController.personaDataOnetimeUnauthorized(it)
        }
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
            onLoginFlowComplete = { requestId, dAppName ->
                navController.popBackStack(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH, true)
                navController.successBottomDialog(false, requestId, dAppName)
            },
            onPersonaOnetime = {
                navController.personaDataOnetimeUnauthorized(it)
            },
            navController = navController,
            onLoginFlowCancelled = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH, true)
            }
        )
        personaDataOnetimeUnauthorized(
            onEdit = {
                navController.personaEditScreen(it.personaAddress, it.requiredFieldsEncoded)
            },
            onBackClick = {
                navController.navigateUp()
            },
            onLoginFlowComplete = { requestId, dAppName ->
                navController.popBackStack(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH, true)
                navController.successBottomDialog(false, requestId, dAppName)
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
