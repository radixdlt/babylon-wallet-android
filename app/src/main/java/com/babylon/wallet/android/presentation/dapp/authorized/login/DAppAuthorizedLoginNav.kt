package com.babylon.wallet.android.presentation.dapp.authorized.login

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.babylon.wallet.android.presentation.dapp.authorized.verifyentities.EntitiesForProofWithSignatures
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.IdentityAddress

@VisibleForTesting
internal const val ARG_INTERACTION_ID = "interaction_id"

const val ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH = "dapp_login_authorized/{$ARG_INTERACTION_ID}"
const val ROUTE_DAPP_LOGIN_AUTHORIZED_SCREEN = "dapp_login_screen/{$ARG_INTERACTION_ID}"

internal class DAppAuthorizedLoginArgs(val interactionId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(checkNotNull(savedStateHandle[ARG_INTERACTION_ID]) as String)
}

fun NavController.dAppLoginAuthorized(requestId: String, navOptionsBuilder: NavOptionsBuilder.() -> Unit = {}) {
    navigate("dapp_login_authorized/$requestId", navOptionsBuilder)
}

@Suppress("LongParameterList")
fun NavGraphBuilder.dAppLoginAuthorized(
    navController: NavController,
    onBackClick: () -> Unit,
    onNavigateToSelectPersona: (authorizedRequestInteractionId: String, dappDefinitionAddress: AccountAddress) -> Unit,
    onNavigateToOneTimeAccounts: (
        walletAuthorizedRequest: String,
        isOneTimeRequest: Boolean,
        isExactAccountsCount: Boolean,
        numberOfAccounts: Int,
        showBacK: Boolean
    ) -> Unit,
    onNavigateToOngoingAccounts: (
        isOneTimeRequest: Boolean,
        isExactAccountsCount: Boolean,
        numberOfAccounts: Int,
        showBacK: Boolean
    ) -> Unit,
    onNavigateToOneTimePersonaData: (RequiredPersonaFields) -> Unit,
    onNavigateToOngoingPersonaData: (IdentityAddress, RequiredPersonaFields) -> Unit,
    onNavigateToVerifyPersona: (interactionId: String, EntitiesForProofWithSignatures) -> Unit,
    onNavigateToVerifyAccounts: (interactionId: String, EntitiesForProofWithSignatures) -> Unit,
    onLoginFlowComplete: () -> Unit,
) {
    composable(
        route = ROUTE_DAPP_LOGIN_AUTHORIZED_SCREEN,
        arguments = listOf(
            navArgument(ARG_INTERACTION_ID) {
                type = NavType.StringType
            }
        )
    ) { entry ->
        val parentEntry = remember(entry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH)
        }
        val vm = hiltViewModel<DAppAuthorizedLoginViewModel>(parentEntry)
        DappAuthorizedLoginScreen(
            viewModel = vm,
            onBackClick = onBackClick,
            onNavigateToSelectPersona = onNavigateToSelectPersona,
            onNavigateToOneTimeAccounts = onNavigateToOneTimeAccounts,
            onNavigateToOngoingAccounts = onNavigateToOngoingAccounts,
            onNavigateToOneTimePersonaData = onNavigateToOneTimePersonaData,
            onNavigateToOngoingPersonaData = onNavigateToOngoingPersonaData,
            onNavigateToVerifyPersona = onNavigateToVerifyPersona,
            onNavigateToVerifyAccounts = onNavigateToVerifyAccounts,
            onLoginFlowComplete = onLoginFlowComplete
        )
    }
}
