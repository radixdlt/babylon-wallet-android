package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.applyShieldNavGraph
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.addfactor.addFactor
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.factorsready.factorsReady
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.onboarding.ROUTE_SECURITY_SHIELD_ONBOARDING
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.onboarding.securityShieldOnboarding
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.preparefactors.prepareFactors
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.recovery.setupRecoveryScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.regularaccess.regularAccess
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.selectfactors.selectFactors
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shieldcreated.shieldCreated
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shieldname.setupShieldName
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string

private const val DESTINATION_CREATE_SECURITY_SHIELD_GRAPH = "create_security_shield_graph"
private const val ADDRESS_ARG = "address_arg"
const val ROUTE_CREATE_SECURITY_SHIELD_GRAPH = "$DESTINATION_CREATE_SECURITY_SHIELD_GRAPH/{$ADDRESS_ARG}"

data class CreateSecurityShieldArgs(
    val address: AddressOfAccountOrPersona?
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<String>(ADDRESS_ARG)?.takeIf { it.isNotEmpty() }?.let {
            AddressOfAccountOrPersona.init(it)
        }
    )
}

fun NavController.createSecurityShield(address: AddressOfAccountOrPersona? = null) {
    navigate("$DESTINATION_CREATE_SECURITY_SHIELD_GRAPH/${address?.string.orEmpty()}")
}

fun NavGraphBuilder.createSecurityShieldNavGraph(
    navController: NavController
) {
    navigation(
        startDestination = ROUTE_SECURITY_SHIELD_ONBOARDING,
        route = ROUTE_CREATE_SECURITY_SHIELD_GRAPH
    ) {
        securityShieldOnboarding(navController)

        prepareFactors(navController)

        addFactor(navController)

        factorsReady(navController)

        selectFactors(navController)

        regularAccess(navController)

        setupRecoveryScreen(navController)

        setupShieldName(navController)

        shieldCreated(navController)

        applyShieldNavGraph(navController)
    }
}
