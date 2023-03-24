package com.babylon.wallet.android.presentation.dapp.authorized

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.babylon.wallet.android.data.dapp.model.PersonaDataField
import com.babylon.wallet.android.presentation.createaccount.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.createaccount.ROUTE_CREATE_ACCOUNT
import com.babylon.wallet.android.presentation.createaccount.createAccountConfirmationScreen
import com.babylon.wallet.android.presentation.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.createpersona.ROUTE_CREATE_PERSONA
import com.babylon.wallet.android.presentation.createpersona.createPersonaConfirmationScreen
import com.babylon.wallet.android.presentation.createpersona.createPersonaScreen
import com.babylon.wallet.android.presentation.createpersona.personaInfoScreen
import com.babylon.wallet.android.presentation.dapp.authorized.account.ROUTE_CHOOSE_ACCOUNTS
import com.babylon.wallet.android.presentation.dapp.authorized.account.chooseAccounts
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.permission.ROUTE_DAPP_PERMISSION
import com.babylon.wallet.android.presentation.dapp.authorized.permission.loginPermission
import com.babylon.wallet.android.presentation.dapp.authorized.personaonetime.personaDataOnetimeAuthorized
import com.babylon.wallet.android.presentation.dapp.authorized.personaongoing.ROUTE_PERSONA_DATA_ONGOING
import com.babylon.wallet.android.presentation.dapp.authorized.personaongoing.personaDataOngoing
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.ROUTE_SELECT_PERSONA
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.selectPersona
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.personaedit.personaEditScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DappAuthorizedLoginNavigationHost(
    initialAuthorizedLoginRoute: InitialAuthorizedLoginRoute,
    navController: NavHostController,
    finishDappLogin: () -> Unit,
    showSuccessDialog: (String) -> Unit,
    sharedViewModel: DAppAuthorizedLoginViewModel
) {
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
            onLoginFlowComplete = { dappName ->
                finishDappLogin()
                showSuccessDialog(dappName)
            },
            createNewPersona = {
                navController.createPersonaScreen()
            },
            initialAuthorizedLoginRoute = initialAuthorizedLoginRoute as? InitialAuthorizedLoginRoute.SelectPersona,
            sharedViewModel = sharedViewModel,
            onDisplayPermission = { event ->
                navController.loginPermission(event.numberOfAccounts, event.isExactAccountsCount, event.oneTime)
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
                navController.popBackStack(ROUTE_CREATE_PERSONA, inclusive = true)
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
            }
        )
        createAccountConfirmationScreen(
            onNavigateToWallet = {
                navController.popBackStack(Screen.WalletDestination.route, inclusive = false)
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
            onLoginFlowComplete = { dappName ->
                finishDappLogin()
                dappName?.let { showSuccessDialog(it) }
            },
            initialAuthorizedLoginRoute = initialAuthorizedLoginRoute as? InitialAuthorizedLoginRoute.ChooseAccount,
            sharedViewModel = sharedViewModel,
            onPersonaOngoingData = {
                navController.personaDataOngoing(it.personaAddress, it.requiredFieldsEncoded)
            },
            onBackClick = {
                navController.popBackStack()
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
            onLoginFlowComplete = { dappName ->
                finishDappLogin()
                showSuccessDialog(dappName)
            },
        )
        personaDataOnetimeAuthorized(
            onEdit = {
                navController.personaEditScreen(it.personaAddress, it.requiredFieldsEncoded)
            },
            sharedViewModel = sharedViewModel,
            initialAuthorizedLoginRoute = initialAuthorizedLoginRoute as? InitialAuthorizedLoginRoute.OngoingPersonaData,
            onBackClick = {
                navController.navigateUp()
            },
            onLoginFlowComplete = { dappName ->
                finishDappLogin()
                showSuccessDialog(dappName)
            },
            onCreatePersona = {
                navController.createPersonaScreen()
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
        val requestedFields: List<PersonaDataField>
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
        }
    }
}
