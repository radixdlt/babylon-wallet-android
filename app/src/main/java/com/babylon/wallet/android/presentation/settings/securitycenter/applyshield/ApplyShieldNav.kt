package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.dapp.authorized.account.ROUTE_CHOOSE_ACCOUNTS
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.accounts.chooseAccounts
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply.applyShield
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.personas.choosePersonas

private const val ROUTE_APPLY_SHIELD_GRAPH = "apply_shield_graph"

fun NavGraphBuilder.applyShieldNavGraph(
    navController: NavController
) {
    navigation(
        startDestination = ROUTE_CHOOSE_ACCOUNTS,
        route = ROUTE_APPLY_SHIELD_GRAPH
    ) {
        chooseAccounts(navController)

        choosePersonas(navController)

        applyShield(navController)
    }
}
