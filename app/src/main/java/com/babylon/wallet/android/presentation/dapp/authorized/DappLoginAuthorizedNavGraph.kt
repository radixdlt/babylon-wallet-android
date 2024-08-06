package com.babylon.wallet.android.presentation.dapp.authorized

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.account.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.dapp.authorized.account.chooseAccounts
import com.babylon.wallet.android.presentation.dapp.authorized.login.ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH
import com.babylon.wallet.android.presentation.dapp.authorized.login.ROUTE_DAPP_LOGIN_AUTHORIZED_SCREEN
import com.babylon.wallet.android.presentation.dapp.authorized.login.dAppLoginAuthorized
import com.babylon.wallet.android.presentation.dapp.authorized.permission.loginPermission
import com.babylon.wallet.android.presentation.dapp.authorized.personaonetime.personaDataOnetimeAuthorized
import com.babylon.wallet.android.presentation.dapp.authorized.personaongoing.personaDataOngoing
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.selectPersona
import com.babylon.wallet.android.presentation.settings.personas.createpersona.createPersonaScreen
import com.babylon.wallet.android.presentation.settings.personas.createpersona.personaInfoScreen
import com.babylon.wallet.android.presentation.settings.personas.personaedit.personaEditScreen

@Suppress("LongMethod")
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
                navController.loginPermission(numberOfAccounts, isExactAccountsCount, oneTime, showBack)
            },
            navigateToOneTimePersonaData = {
                navController.personaDataOnetimeAuthorized(it, false)
            },
            navigateToSelectPersona = { dappDefinitionAddress ->
                navController.selectPersona(dappDefinitionAddress)
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
            },
            navigateToOngoingPersonaData = { personaAddress, requiredFields ->
                navController.personaDataOngoing(personaAddress, requiredFields, false)
            }
        )
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
            },
            createNewPersona = { isFirstPersonaCreated ->
                if (isFirstPersonaCreated) {
                    navController.createPersonaScreen()
                } else {
                    navController.personaInfoScreen()
                }
            },
            onDisplayPermission = { event ->
                navController.loginPermission(event.numberOfAccounts, event.isExactAccountsCount, event.oneTime, true)
            },
            onPersonaDataOngoing = {
                navController.personaDataOngoing(it.personaAddress, it.requiredPersonaFields, true)
            },
            onPersonaDataOnetime = {
                navController.personaDataOnetimeAuthorized(it.requiredPersonaFields, true)
            }
        )
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
            },
            navController = navController,
            onBackClick = {
                navController.popBackStack()
            },
            onPersonaOngoingData = {
                navController.personaDataOngoing(it.personaAddress, it.requiredPersonaFields, true)
            },
            onPersonaDataOnetime = {
                navController.personaDataOnetimeAuthorized(it.requiredPersonaFields, true)
            }
        )
        personaDataOngoing(
            onEdit = {
                navController.personaEditScreen(it.personaAddress, it.requiredPersonaFields)
            },
            navController = navController,
            onBackClick = {
                navController.navigateUp()
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
            },
            onPersonaDataOnetime = {
                navController.personaDataOnetimeAuthorized(it.requiredPersonaFields, true)
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
                navController.personaEditScreen(it.persona.address, it.requiredPersonaFields)
            },
            navController = navController,
            onBackClick = {
                navController.navigateUp()
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
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
