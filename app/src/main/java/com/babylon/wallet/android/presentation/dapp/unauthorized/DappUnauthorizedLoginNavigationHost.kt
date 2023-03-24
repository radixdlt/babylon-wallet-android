package com.babylon.wallet.android.presentation.dapp.unauthorized

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
import com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime.ROUTE_CHOOSE_ACCOUNTS_ONETIME
import com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime.chooseAccountsOneTime
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.DAppUnauthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.personaonetime.ROUTE_PERSONA_DATA_ONETIME_UNAUTHORIZED
import com.babylon.wallet.android.presentation.dapp.unauthorized.personaonetime.personaDataOnetimeUnauthorized
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.personaedit.personaEditScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DappUnauthorizedLoginNavigationHost(
    initialUnauthorizedLoginRoute: InitialUnauthorizedLoginRoute,
    navController: NavHostController,
    finishDappLogin: () -> Unit,
    showSuccessDialog: (String) -> Unit,
    sharedViewModel: DAppUnauthorizedLoginViewModel
) {
    AnimatedNavHost(
        navController = navController,
        startDestination = initialUnauthorizedLoginRoute.toRouteString()
    ) {
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
        personaEditScreen(onBackClick = {
            navController.navigateUp()
        })
        chooseAccountsOneTime(
            exitRequestFlow = {
                navController.popBackStack()
            },
            dismissErrorDialog = {
                navController.popBackStack()
            },
            initialUnauthorizedLoginRoute = initialUnauthorizedLoginRoute as? InitialUnauthorizedLoginRoute.ChooseAccount,
            sharedViewModel = sharedViewModel,
            onPersonaOnetime = {
                navController.personaDataOnetimeUnauthorized(it)
            },
            onLoginFlowComplete = {
                finishDappLogin()
                showSuccessDialog(it)
            },
            onAccountCreationClick = {
                navController.createAccountScreen(CreateAccountRequestSource.ChooseAccount)
            }
        )
        personaDataOnetimeUnauthorized(
            onEdit = {
                navController.personaEditScreen(it.personaAddress, it.requiredFieldsEncoded)
            },
            sharedViewModel = sharedViewModel,
            initialDappLoginRoute = initialUnauthorizedLoginRoute as? InitialUnauthorizedLoginRoute.OnetimePersonaData,
            onBackClick = {
                navController.navigateUp()
            },
            onLoginFlowComplete = { dappName ->
                finishDappLogin()
                showSuccessDialog(dappName)
            }
        ) {
            navController.createPersonaScreen()
        }
    }
}

sealed interface InitialUnauthorizedLoginRoute {
    data class OnetimePersonaData(
        val requestedFields: List<PersonaDataField>
    ) : InitialUnauthorizedLoginRoute

    data class ChooseAccount(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean
    ) : InitialUnauthorizedLoginRoute

    fun toRouteString(): String {
        return when (this) {
            is ChooseAccount -> ROUTE_CHOOSE_ACCOUNTS_ONETIME
            is OnetimePersonaData -> ROUTE_PERSONA_DATA_ONETIME_UNAUTHORIZED
        }
    }
}
