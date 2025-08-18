package com.babylon.wallet.android.presentation.addfactorsource

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.addfactorsource.arculus.createpin.createArculusPin
import com.babylon.wallet.android.presentation.addfactorsource.confirmseedphrase.confirmSeedPhrase
import com.babylon.wallet.android.presentation.addfactorsource.intro.ROUTE_ADD_FACTOR_SOURCE_INTRO
import com.babylon.wallet.android.presentation.addfactorsource.intro.addFactorSourceIntro
import com.babylon.wallet.android.presentation.addfactorsource.name.setFactorSourceName
import com.babylon.wallet.android.presentation.addfactorsource.seedphrase.seedPhrase

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
        startDestination = ROUTE_ADD_FACTOR_SOURCE_INTRO,
        route = ROUTE_ADD_FACTOR_SOURCE_GRAPH
    ) {
        addFactorSourceIntro(navController)

        seedPhrase(navController)

        confirmSeedPhrase(navController)

        createArculusPin(
            onDismiss = { navController.popBackStack() },
            onConfirmed = { navController.setFactorSourceName() }
        )

        setFactorSourceName(navController)
    }
}
