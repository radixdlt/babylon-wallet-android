package com.babylon.wallet.android.presentation.dapp

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.babylon.wallet.android.presentation.createaccount.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.createaccount.ROUTE_CREATE_ACCOUNT
import com.babylon.wallet.android.presentation.createaccount.createAccountConfirmationScreen
import com.babylon.wallet.android.presentation.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.createpersona.ROUTE_CREATE_PERSONA
import com.babylon.wallet.android.presentation.createpersona.createPersonaConfirmationScreen
import com.babylon.wallet.android.presentation.createpersona.createPersonaScreen
import com.babylon.wallet.android.presentation.dapp.account.ROUTE_CHOOSE_ACCOUNTS
import com.babylon.wallet.android.presentation.dapp.account.chooseAccounts
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginViewModel
import com.babylon.wallet.android.presentation.dapp.permission.ROUTE_DAPP_PERMISSION
import com.babylon.wallet.android.presentation.dapp.permission.dappPermission
import com.babylon.wallet.android.presentation.dapp.selectpersona.ROUTE_SELECT_PERSONA
import com.babylon.wallet.android.presentation.dapp.selectpersona.selectPersona
import com.babylon.wallet.android.presentation.navigation.Screen
import com.google.accompanist.navigation.animation.AnimatedNavHost

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DappLoginNavigationHost(
    initialRoute: InitialRoute,
    navController: NavHostController,
    finishDappLogin: () -> Unit,
    showSuccessDialog: (String) -> Unit,
    sharedViewModel: DAppLoginViewModel
) {
    AnimatedNavHost(
        navController = navController,
        startDestination = initialRoute.toRouteString()
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
            initialRoute = initialRoute as? InitialRoute.SelectPersona,
            sharedViewModel = sharedViewModel
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
        dappPermission(
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
            initialRoute = initialRoute as? InitialRoute.Permission,
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
            initialRoute = initialRoute as? InitialRoute.ChooseAccount,
            sharedViewModel = sharedViewModel
        ) {
            navController.popBackStack()
        }
    }
}

sealed interface InitialRoute {
    data class SelectPersona(val reqId: String) : InitialRoute
    data class Permission(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false
    ) : InitialRoute

    data class ChooseAccount(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false,
        val showBack: Boolean = false
    ) : InitialRoute

    fun toRouteString(): String {
        return when (this) {
            is ChooseAccount -> ROUTE_CHOOSE_ACCOUNTS
            is Permission -> ROUTE_DAPP_PERMISSION
            is SelectPersona -> ROUTE_SELECT_PERSONA
        }
    }
}
