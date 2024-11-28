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
import com.babylon.wallet.android.presentation.dapp.authorized.ongoingaccounts.ongoingAccounts
import com.babylon.wallet.android.presentation.dapp.authorized.personaonetime.personaDataOnetimeAuthorized
import com.babylon.wallet.android.presentation.dapp.authorized.personaongoing.personaDataOngoing
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.selectPersona
import com.babylon.wallet.android.presentation.dapp.authorized.verifyentities.accounts.verifyAccounts
import com.babylon.wallet.android.presentation.dapp.authorized.verifyentities.persona.verifyPersona
import com.babylon.wallet.android.presentation.settings.personas.createpersona.CreatePersonaRequestSource
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
            onNavigateToSelectPersona = { authorizedRequestInteractionId, dappDefinitionAddress ->
                navController.selectPersona(authorizedRequestInteractionId, dappDefinitionAddress)
            },
            onNavigateToOneTimeAccounts = { interactionId, isOneTimeRequest, isExactAccountsCount, numberOfAccounts, showBack ->
                navController.chooseAccounts(
                    authorizedRequestInteractionId = interactionId,
                    isOneTimeRequest = isOneTimeRequest,
                    isExactAccountsCount = isExactAccountsCount,
                    numberOfAccounts = numberOfAccounts,
                    showBack = showBack
                )
            },
            onNavigateToOngoingAccounts = { isOneTimeRequest, isExactAccountsCount, numberOfAccounts, showBack ->
                navController.ongoingAccounts(
                    isOneTimeRequest = isOneTimeRequest,
                    isExactAccountsCount = isExactAccountsCount,
                    numberOfAccounts = numberOfAccounts,
                    showBack = showBack
                )
            },
            onNavigateToOneTimePersonaData = {
                navController.personaDataOnetimeAuthorized(it, false)
            },
            onNavigateToVerifyPersona = { walletUnauthorizedRequestInteractionId, entitiesForProofWithSignatures ->
                navController.verifyPersona(
                    walletAuthorizedRequestInteractionId = walletUnauthorizedRequestInteractionId,
                    entitiesForProofWithSignatures = entitiesForProofWithSignatures,
                    canNavigateBack = false
                )
            },
            onNavigateToVerifyAccounts = { walletUnauthorizedRequestInteractionId, entitiesForProofWithSignatures ->
                navController.verifyAccounts(
                    walletAuthorizedRequestInteractionId = walletUnauthorizedRequestInteractionId,
                    entitiesForProofWithSignatures = entitiesForProofWithSignatures,
                    canNavigateBack = false
                )
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
            },
            onNavigateToOngoingPersonaData = { personaAddress, requiredFields ->
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
                    authorizedRequestInteractionId = event.authorizedRequestInteractionId,
                    isOneTimeRequest = event.isOneTimeRequest,
                    isExactAccountsCount = event.isExactAccountsCount,
                    numberOfAccounts = event.numberOfAccounts,
                    showBack = event.showBack
                )
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
            },
            createNewPersona = { isFirstPersonaCreated ->
                if (isFirstPersonaCreated) {
                    navController.createPersonaScreen(CreatePersonaRequestSource.DappRequest)
                } else {
                    navController.personaInfoScreen(CreatePersonaRequestSource.DappRequest)
                }
            },
            onNavigateToOngoingAccounts = { event ->
                navController.ongoingAccounts(
                    isOneTimeRequest = event.isOneTimeRequest,
                    isExactAccountsCount = event.isExactAccountsCount,
                    numberOfAccounts = event.numberOfAccounts,
                    showBack = true
                )
            },
            onNavigateToOngoingPersonaData = {
                navController.personaDataOngoing(it.personaAddress, it.requiredPersonaFields, true)
            },
            onNavigateToOneTimePersonaData = {
                navController.personaDataOnetimeAuthorized(it.requiredPersonaFields, true)
            }
        )

        ongoingAccounts(
            onChooseAccounts = { event ->
                navController.chooseAccounts(
                    authorizedRequestInteractionId = event.authorizedRequestInteractionId,
                    isOneTimeRequest = event.isOneTimeRequest,
                    isExactAccountsCount = event.isExactAccountsCount,
                    numberOfAccounts = event.numberOfAccounts,
                    showBack = event.showBack
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
            onNavigateToChooseAccounts = { event ->
                navController.chooseAccounts(
                    authorizedRequestInteractionId = event.authorizedRequestInteractionId,
                    isOneTimeRequest = event.isOneTimeRequest,
                    isExactAccountsCount = event.isExactAccountsCount,
                    numberOfAccounts = event.numberOfAccounts,
                    showBack = event.showBack
                )
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
            },
            navController = navController,
            onBackClick = {
                navController.popBackStack()
            },
            onNavigateToOngoingPersonaData = {
                navController.personaDataOngoing(it.personaAddress, it.requiredPersonaFields, true)
            },
            onNavigateToOneTimePersonaData = {
                navController.personaDataOnetimeAuthorized(it.requiredPersonaFields, true)
            },
            onNavigateToVerifyPersona = { walletUnauthorizedRequestInteractionId, entitiesForProofWithSignatures ->
                navController.verifyPersona(
                    walletAuthorizedRequestInteractionId = walletUnauthorizedRequestInteractionId,
                    entitiesForProofWithSignatures = entitiesForProofWithSignatures,
                    canNavigateBack = true
                )
            },
            onNavigateToVerifyAccounts = { walletUnauthorizedRequestInteractionId, entitiesForProofWithSignatures ->
                navController.verifyAccounts(
                    walletAuthorizedRequestInteractionId = walletUnauthorizedRequestInteractionId,
                    entitiesForProofWithSignatures = entitiesForProofWithSignatures,
                    canNavigateBack = true
                )
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
                    authorizedRequestInteractionId = event.authorizedRequestInteractionId,
                    isOneTimeRequest = event.isOneTimeRequest,
                    isExactAccountsCount = event.isExactAccountsCount,
                    numberOfAccounts = event.numberOfAccounts,
                    showBack = event.showBack
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
            onCreatePersona = { isFirstPersonaCreated ->
                if (isFirstPersonaCreated) {
                    navController.createPersonaScreen(CreatePersonaRequestSource.DappRequest)
                } else {
                    navController.personaInfoScreen(CreatePersonaRequestSource.DappRequest)
                }
            },
            onNavigateToVerifyPersona = { walletUnauthorizedRequestInteractionId, entitiesForProofWithSignatures ->
                navController.verifyPersona(
                    walletAuthorizedRequestInteractionId = walletUnauthorizedRequestInteractionId,
                    entitiesForProofWithSignatures = entitiesForProofWithSignatures,
                    canNavigateBack = true
                )
            },
            onNavigateToVerifyAccounts = { walletUnauthorizedRequestInteractionId, entitiesForProofWithSignatures ->
                navController.verifyAccounts(
                    walletAuthorizedRequestInteractionId = walletUnauthorizedRequestInteractionId,
                    entitiesForProofWithSignatures = entitiesForProofWithSignatures,
                    canNavigateBack = true
                )
            },
            onLoginFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
            }
        )

        verifyPersona(
            navController = navController,
            onNavigateToVerifyAccounts = { walletUnauthorizedRequestInteractionId, entitiesForProofWithSignatures ->
                navController.verifyAccounts(
                    walletAuthorizedRequestInteractionId = walletUnauthorizedRequestInteractionId,
                    entitiesForProofWithSignatures = entitiesForProofWithSignatures,
                    canNavigateBack = true
                )
            },
            onVerificationFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
            },
            onBackClick = {
                navController.navigateUp()
            }
        )

        verifyAccounts(
            navController = navController,
            onVerificationFlowComplete = {
                navController.popBackStack(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH, true)
            },
            onBackClick = {
                navController.navigateUp()
            }
        )
    }
}
