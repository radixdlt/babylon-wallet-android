package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.accounts.ROUTE_CHOOSE_ACCOUNTS
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.accounts.chooseAccounts
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply.applyShield
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.personas.choosePersonas
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.extensions.init

private const val DESTINATION_APPLY_SHIELD_GRAPH = "apply_shield_graph"
const val ARG_SECURITY_STRUCTURE_ID = "arg_security_structure_id"
const val ARG_ENTITY_ADDRESS = "arg_entity_address"

const val ROUTE_APPLY_SHIELD_GRAPH = DESTINATION_APPLY_SHIELD_GRAPH +
    "?$ARG_SECURITY_STRUCTURE_ID={$ARG_SECURITY_STRUCTURE_ID}" +
    "&$ARG_ENTITY_ADDRESS={$ARG_ENTITY_ADDRESS}"

data class ApplyShieldArgs(
    val securityStructureId: SecurityStructureId,
    val address: AddressOfAccountOrPersona?
) {

    val isInEntityContext = address != null

    constructor(savedStateHandle: SavedStateHandle) : this(
        securityStructureId = SecurityStructureId.fromString(
            requireNotNull(
                savedStateHandle.get<String>(
                    ARG_SECURITY_STRUCTURE_ID
                )
            )
        ),
        address = savedStateHandle.get<String>(ARG_ENTITY_ADDRESS)?.let {
            AddressOfAccountOrPersona.init(it)
        }
    )

    constructor(bundle: Bundle) : this(
        securityStructureId = SecurityStructureId.fromString(
            requireNotNull(
                bundle.getString(ARG_SECURITY_STRUCTURE_ID)
            )
        ),
        address = bundle.getString(ARG_ENTITY_ADDRESS)?.let {
            AddressOfAccountOrPersona.init(it)
        }
    )
}

fun NavController.applyShieldNavGraph(
    id: SecurityStructureId,
    navOptionsBuilder: NavOptionsBuilder.() -> Unit = {}
) {
    navigate("$DESTINATION_APPLY_SHIELD_GRAPH?$ARG_SECURITY_STRUCTURE_ID=$id", navOptionsBuilder)
}

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
