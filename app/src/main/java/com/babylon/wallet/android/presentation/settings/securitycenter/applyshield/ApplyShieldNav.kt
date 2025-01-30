package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.accounts.ROUTE_CHOOSE_ACCOUNTS
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.accounts.chooseAccounts
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply.applyShield
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.personas.choosePersonas
import com.radixdlt.sargon.SecurityStructureId

private const val DESTINATION_APPLY_SHIELD_GRAPH = "apply_shield_graph"
private const val ARG_SECURITY_STRUCTURE_ID = "arg_security_structure_id"

const val ROUTE_APPLY_SHIELD_GRAPH = "$DESTINATION_APPLY_SHIELD_GRAPH/{$ARG_SECURITY_STRUCTURE_ID}"

internal class ApplyShieldArgs(
    val securityStructureId: SecurityStructureId
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        securityStructureId = SecurityStructureId.fromString(requireNotNull(savedStateHandle.get<String>(ARG_SECURITY_STRUCTURE_ID)))
    )
}

fun NavController.applyShieldNavGraph(
    id: SecurityStructureId,
    navOptionsBuilder: NavOptionsBuilder.() -> Unit = {}
) {
    navigate("$DESTINATION_APPLY_SHIELD_GRAPH/$id", navOptionsBuilder)
}

fun NavGraphBuilder.applyShieldNavGraph(
    navController: NavController
) {
    navigation(
        startDestination = ROUTE_CHOOSE_ACCOUNTS,
        route = ROUTE_APPLY_SHIELD_GRAPH,
    ) {
        chooseAccounts(navController)

        choosePersonas(navController)

        applyShield(navController)
    }
}
