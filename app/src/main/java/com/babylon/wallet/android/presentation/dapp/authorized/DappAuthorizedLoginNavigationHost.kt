package com.babylon.wallet.android.presentation.dapp.authorized

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.babylon.wallet.android.presentation.createaccount.ROUTE_CREATE_ACCOUNT
import com.babylon.wallet.android.presentation.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.createaccount.confirmation.createAccountConfirmationScreen
import com.babylon.wallet.android.presentation.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.createaccount.withledger.createAccountWithLedger
import com.babylon.wallet.android.presentation.createpersona.createPersonaConfirmationScreen
import com.babylon.wallet.android.presentation.createpersona.createPersonaScreen
import com.babylon.wallet.android.presentation.createpersona.personaInfoScreen
import com.babylon.wallet.android.presentation.createpersona.popPersonaCreation
import com.babylon.wallet.android.presentation.dapp.authorized.account.ROUTE_CHOOSE_ACCOUNTS
import com.babylon.wallet.android.presentation.dapp.authorized.account.chooseAccounts
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginEvent
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.permission.ROUTE_DAPP_PERMISSION
import com.babylon.wallet.android.presentation.dapp.authorized.permission.loginPermission
import com.babylon.wallet.android.presentation.dapp.authorized.personaonetime.ROUTE_PERSONA_DATA_ONETIME_AUTHORIZED
import com.babylon.wallet.android.presentation.dapp.authorized.personaonetime.personaDataOnetimeAuthorized
import com.babylon.wallet.android.presentation.dapp.authorized.personaongoing.ROUTE_PERSONA_DATA_ONGOING
import com.babylon.wallet.android.presentation.dapp.authorized.personaongoing.personaDataOngoing
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.ROUTE_SELECT_PERSONA
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.selectPersona
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.settings.personaedit.personaEditScreen
import com.babylon.wallet.android.utils.decodeUtf8
import com.google.accompanist.navigation.animation.AnimatedNavHost

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DappAuthorizedLoginNavigationHost(
    initialAuthorizedLoginRoute: InitialAuthorizedLoginRoute,
    navController: NavHostController,
    finishDappLogin: () -> Unit,
    showSuccessDialog: (requestId: String, dAppName: String) -> Unit,
    sharedViewModel: DAppAuthorizedLoginViewModel
) {
    val loginFlowCompletedCallback = { event: DAppAuthorizedLoginEvent.LoginFlowCompleted ->
        finishDappLogin()
        if (event.showSuccessDialog) {
            showSuccessDialog(
                event.requestId,
                event.dAppName.decodeUtf8()
            )
        }
    }
    AnimatedNavHost(
        navController = navController,
        startDestination = initialAuthorizedLoginRoute.toRouteString()
    ) {
        selectPersona(
            onBackClick = finishDappLogin,
            onChooseAccounts = { event ->
                navController.chooseAccounts(
                    event.numberOfAccounts,
                    event.isExactAccountsCount,
                    event.oneTime,
                    event.showBack
                )
            },
            onLoginFlowComplete = loginFlowCompletedCallback,
            createNewPersona = { isFirstPersonaCreated ->
                if (isFirstPersonaCreated) {
                    navController.createPersonaScreen()
                } else {
                    navController.personaInfoScreen()
                }
            },
            initialAuthorizedLoginRoute = initialAuthorizedLoginRoute as? InitialAuthorizedLoginRoute.SelectPersona,
            sharedViewModel = sharedViewModel,
            onDisplayPermission = { event ->
                navController.loginPermission(event.numberOfAccounts, event.isExactAccountsCount, event.oneTime)
            },
            onPersonaDataOngoing = {
                navController.personaDataOngoing(it.personaAddress, it.requiredFieldsEncoded)
            },
            onPersonaDataOnetime = {
                navController.personaDataOnetimeAuthorized(it.requiredFieldsEncoded)
            }
        )
        personaInfoScreen(
            onBackClick = { navController.navigateUp() },
            onContinueClick = { navController.createPersonaScreen() }
        )
        createPersonaScreen(
            onBackClick = { navController.navigateUp() },
            onContinueClick = { personaId ->
                navController.createPersonaConfirmationScreen(personaId = personaId)
            }
        )
        createPersonaConfirmationScreen(
            finishPersonaCreation = {
                navController.popPersonaCreation()
            }
        )
        createAccountScreen(
            "",
            onBackClick = {
                navController.navigateUp()
            },
            onContinueClick = { accountId, requestSource ->
                navController.createAccountConfirmationScreen(
                    accountId,
                    requestSource ?: CreateAccountRequestSource.FirstTime
                )
            },
            onCloseApp = {},
            onAddLedgerDevice = {
                navController.createAccountWithLedger()
            }
        )
        createAccountWithLedger(
            onBackClick = {
                navController.navigateUp()
            },
            goBackToCreateAccount = {
                navController.popBackStack(ROUTE_CREATE_ACCOUNT, false)
            }
        )
        createAccountConfirmationScreen(
            onNavigateToWallet = {
                navController.popBackStack(MAIN_ROUTE, inclusive = false)
            },
            onFinishAccountCreation = {
                navController.popBackStack(ROUTE_CREATE_ACCOUNT, inclusive = true)
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
                finishDappLogin()
            },
            initialAuthorizedLoginRoute = initialAuthorizedLoginRoute as? InitialAuthorizedLoginRoute.Permission,
            sharedViewModel = sharedViewModel
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
            onLoginFlowComplete = loginFlowCompletedCallback,
            initialAuthorizedLoginRoute = initialAuthorizedLoginRoute as? InitialAuthorizedLoginRoute.ChooseAccount,
            sharedViewModel = sharedViewModel,
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
        personaEditScreen(onBackClick = {
            navController.navigateUp()
        })
        personaDataOngoing(
            onEdit = {
                navController.personaEditScreen(it.personaAddress, it.requiredFieldsEncoded)
            },
            sharedViewModel = sharedViewModel,
            initialAuthorizedLoginRoute = initialAuthorizedLoginRoute as? InitialAuthorizedLoginRoute.OngoingPersonaData,
            onBackClick = {
                navController.navigateUp()
            },
            onLoginFlowComplete = loginFlowCompletedCallback,
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
            sharedViewModel = sharedViewModel,
            initialAuthorizedLoginRoute = initialAuthorizedLoginRoute as? InitialAuthorizedLoginRoute.OneTimePersonaData,
            onBackClick = {
                navController.navigateUp()
            },
            onLoginFlowComplete = loginFlowCompletedCallback,
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

sealed interface InitialAuthorizedLoginRoute {
    data class SelectPersona(val reqId: String) : InitialAuthorizedLoginRoute
    data class Permission(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val showBack: Boolean = false,
        val oneTime: Boolean = false
    ) : InitialAuthorizedLoginRoute

    data class OngoingPersonaData(
        val personaAddress: String,
        val requestedFieldsEncoded: String
    ) : InitialAuthorizedLoginRoute

    data class OneTimePersonaData(
        val requestedFieldsEncoded: String
    ) : InitialAuthorizedLoginRoute

    data class ChooseAccount(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false,
        val showBack: Boolean = false
    ) : InitialAuthorizedLoginRoute

    fun toRouteString(): String {
        return when (this) {
            is ChooseAccount -> ROUTE_CHOOSE_ACCOUNTS
            is Permission -> ROUTE_DAPP_PERMISSION
            is SelectPersona -> ROUTE_SELECT_PERSONA
            is OngoingPersonaData -> ROUTE_PERSONA_DATA_ONGOING
            is OneTimePersonaData -> ROUTE_PERSONA_DATA_ONETIME_AUTHORIZED
        }
    }
}
