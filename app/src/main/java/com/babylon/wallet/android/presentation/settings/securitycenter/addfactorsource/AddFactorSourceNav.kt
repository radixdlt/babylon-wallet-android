package com.babylon.wallet.android.presentation.settings.securitycenter.addfactorsource

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.settings.securitycenter.addfactorsource.device.confirmseedphrase.confirmDeviceSeedPhrase
import com.babylon.wallet.android.presentation.settings.securitycenter.addfactorsource.device.seedphrase.deviceSeedPhrase
import com.babylon.wallet.android.presentation.settings.securitycenter.addfactorsource.intro.ROUTE_ADD_FACTOR_INTRO
import com.babylon.wallet.android.presentation.settings.securitycenter.addfactorsource.intro.addFactorIntro
import com.babylon.wallet.android.presentation.settings.securitycenter.addfactorsource.name.setFactorName

const val ROUTE_ADD_FACTOR_SOURCE_GRAPH = "add_factor_source_graph"

fun NavController.addFactorSource(
    navOptionsBuilder: NavOptionsBuilder.() -> Unit = {}
) {
    navigate(ROUTE_ADD_FACTOR_SOURCE_GRAPH, navOptionsBuilder)
}

fun NavGraphBuilder.addFactorSource(
    navController: NavController
) {
    navigation(
        startDestination = ROUTE_ADD_FACTOR_INTRO,
        route = ROUTE_ADD_FACTOR_SOURCE_GRAPH
    ) {
        addFactorIntro(navController)

        deviceSeedPhrase(navController)

        confirmDeviceSeedPhrase(navController)

        setFactorName(navController)
    }
}
