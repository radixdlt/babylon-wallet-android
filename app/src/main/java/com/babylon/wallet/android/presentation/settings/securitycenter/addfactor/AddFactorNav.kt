package com.babylon.wallet.android.presentation.settings.securitycenter.addfactor

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.device.confirmseedphrase.confirmDeviceSeedPhrase
import com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.device.seedphrase.deviceSeedPhrase
import com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.intro.ROUTE_ADD_FACTOR_INTRO
import com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.intro.addFactorIntro
import com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.name.setFactorName
import com.radixdlt.sargon.FactorSourceKind

private const val DESTINATION_ADD_FACTOR_GRAPH = "add_factor_graph"
private const val ARG_FACTOR_SOURCE_KIND = "arg_factor_source_kind"

const val ROUTE_ADD_FACTOR_GRAPH = "$DESTINATION_ADD_FACTOR_GRAPH/{$ARG_FACTOR_SOURCE_KIND}"

internal class AddFactorArgs(
    val kind: FactorSourceKind
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        kind = FactorSourceKind.entries[requireNotNull(savedStateHandle.get<String>(ARG_FACTOR_SOURCE_KIND)?.toInt())]
    )
}

fun NavController.addFactor(
    kind: FactorSourceKind,
    navOptionsBuilder: NavOptionsBuilder.() -> Unit = {}
) {
    navigate("$DESTINATION_ADD_FACTOR_GRAPH/${kind.ordinal}", navOptionsBuilder)
}

fun NavGraphBuilder.addFactor(
    navController: NavController
) {
    navigation(
        startDestination = ROUTE_ADD_FACTOR_INTRO,
        route = ROUTE_ADD_FACTOR_GRAPH
    ) {
        addFactorIntro(navController)

        deviceSeedPhrase(navController)

        confirmDeviceSeedPhrase(navController)

        setFactorName(navController)
    }
}
