package com.babylon.wallet.android.presentation.dapp.authorized

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.createpersona.createPersonaScreen
import com.babylon.wallet.android.presentation.createpersona.personaInfoScreen
import com.babylon.wallet.android.presentation.dapp.authorized.account.chooseAccounts
import com.babylon.wallet.android.presentation.dapp.authorized.login.ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH
import com.babylon.wallet.android.presentation.dapp.authorized.login.ROUTE_DAPP_LOGIN_AUTHORIZED_SCREEN
import com.babylon.wallet.android.presentation.dapp.authorized.login.dAppLoginAuthorized
import com.babylon.wallet.android.presentation.dapp.authorized.permission.loginPermission
import com.babylon.wallet.android.presentation.dapp.authorized.personaonetime.personaDataOnetimeAuthorized
import com.babylon.wallet.android.presentation.dapp.authorized.personaongoing.personaDataOngoing
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.selectPersona
import com.babylon.wallet.android.presentation.settings.personaedit.personaEditScreen
import com.babylon.wallet.android.presentation.ui.composables.resultdialog.success.successBottomDialog
import com.google.accompanist.navigation.animation.navigation

@Suppress("LongMethod")
@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.dappLoginAuthorizedNavGraph(navController: NavController) {
    navigation(
        startDestination = ROUTE_DAPP_LOGIN_AUTHORIZED_SCREEN,
        route = ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH
    ) {
        dAppLoginAuthorized(
            navController,
            onBackClick = {
                navController.popBackStack()
            },
            navigateToChooseAccount = { numberOfAccounts, isExactAccountsCount, oneTime, showBack ->
                navController.chooseAccounts(numberOfAccounts, isExactAccountsCount, oneTime, showBack)
            },
            navigateToPermissions = { numberOfAccounts, isExactAccountsCount, oneTime, showBack ->
                navController.loginPermission(numberOfAccounts, isExactAccountsCount, oneTime)
            },
            navigateToOneTimePersonaData = {
                navController.personaDataOnetimeAuthorized(it)
            },
            navigateToSelectPersona = { reqeustId ->
                navController.selectPersona(reqeustId)
            }
        ) { personaAddress, fieldsEncoded ->
            navController.personaDataOngoing(personaAddress, fieldsEncoded)
        }
        selectPersona(
            navController = navController,
            onBackClick = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
            },
            onChooseAccounts = { event ->
                navController.chooseAccounts(
                    event.numberOfAccounts,
                    event.isExactAccountsCount,
                    event.oneTime,
                    event.showBack
                )
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
                if (it.showSuccessDialog) {
                    navController.successBottomDialog(false, it.requestId, it.dAppName)
                }
            },
            createNewPersona = { isFirstPersonaCreated ->
                if (isFirstPersonaCreated) {
                    navController.createPersonaScreen()
                } else {
                    navController.personaInfoScreen()
                }
            },
            onDisplayPermission = { event ->
                navController.loginPermission(event.numberOfAccounts, event.isExactAccountsCount, event.oneTime)
            },
            onPersonaDataOngoing = {
                navController.personaDataOngoing(it.personaAddress, it.requiredFieldsEncoded)
            }
        ) {
            navController.personaDataOnetimeAuthorized(it.requiredFieldsEncoded)
        }
        loginPermission(
            onChooseAccounts = { event ->
                navController.chooseAccounts(
                    event.numberOfAccounts,
                    event.isExactAccountsCount,
                    event.oneTime,
                    event.showBack
                )
            },
            onCompleteFlow = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
            },
            navController = navController
        ) {
            navController.popBackStack()
        }
        chooseAccounts(
            dismissErrorDialog = {
                navController.navigateUp()
            },
            onAccountCreationClick = {
                navController.createAccountScreen(CreateAccountRequestSource.ChooseAccount)
            },
            onChooseAccounts = { event ->
                navController.chooseAccounts(
                    event.numberOfAccounts,
                    event.isExactAccountsCount,
                    event.oneTime,
                    event.showBack
                )
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
                if (it.showSuccessDialog) {
                    navController.successBottomDialog(false, it.requestId, it.dAppName)
                }
            },
            navController = navController,
            onBackClick = {
                navController.popBackStack()
            },
            onPersonaOngoingData = {
                navController.personaDataOngoing(it.personaAddress, it.requiredFieldsEncoded)
            },
            onPersonaDataOnetime = {
                navController.personaDataOnetimeAuthorized(it.requiredFieldsEncoded)
            }
        )
        personaDataOngoing(
            onEdit = {
                navController.personaEditScreen(it.personaAddress, it.requiredFieldsEncoded)
            },
            navController = navController,
            onBackClick = {
                navController.navigateUp()
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
                if (it.showSuccessDialog) {
                    navController.successBottomDialog(false, it.requestId, it.dAppName)
                }
            },
            onPersonaDataOnetime = {
                navController.personaDataOnetimeAuthorized(it.requiredFieldsEncoded)
            },
            onChooseAccounts = { event ->
                navController.chooseAccounts(
                    event.numberOfAccounts,
                    event.isExactAccountsCount,
                    event.oneTime,
                    event.showBack
                )
            }
        )
        personaDataOnetimeAuthorized(
            onEdit = {
                navController.personaEditScreen(it.personaAddress, it.requiredFieldsEncoded)
            },
            navController = navController,
            onBackClick = {
                navController.navigateUp()
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
                if (it.showSuccessDialog) {
                    navController.successBottomDialog(false, it.requestId, it.dAppName)
                }
            },
            onCreatePersona = { isFirstPersonaCreated ->
                if (isFirstPersonaCreated) {
                    navController.createPersonaScreen()
                } else {
                    navController.personaInfoScreen()
                }
            }
        )
    }
}
