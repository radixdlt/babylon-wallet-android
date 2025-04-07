package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.applyShieldNavGraph
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.addfactor.addFactor
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.factorsready.factorsReady
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.onboarding.securityShieldOnboarding
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.preparefactors.prepareFactors
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.recovery.setupRecoveryScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.regularaccess.regularAccess
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.selectfactors.selectFactors
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shieldcreated.shieldCreated
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shielddetails.securityShieldDetails
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shieldname.setupShieldName

const val ROUTE_SECURITY_SHIELDS = "security_shields"
const val ROUTE_SECURITY_SHIELDS_GRAPH = "security_shields_graph"

fun NavGraphBuilder.securityShieldsNavGraph(
    navController: NavController
) {
    navigation(
        startDestination = ROUTE_SECURITY_SHIELDS,
        route = ROUTE_SECURITY_SHIELDS_GRAPH
    ) {
        securityShieldsScreen(navController)

        securityShieldDetails(navController)

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

fun NavController.securityShieldsScreen(navOptionsBuilder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(ROUTE_SECURITY_SHIELDS, navOptionsBuilder)
}

fun NavGraphBuilder.securityShieldsScreen(
    navController: NavController
) {
    composable(
        route = ROUTE_SECURITY_SHIELDS,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        SecurityShieldsScreen(
            viewModel = hiltViewModel(),
            onNavigateToSecurityShieldDetails = { securityShieldId, securityShieldName ->
                navController.securityShieldDetails(securityShieldId, securityShieldName)
            },
            onCreateNewSecurityShieldClick = { navController.securityShieldOnboarding() },
            onInfoClick = { glossaryItem -> navController.infoDialog(glossaryItem) },
            onBackClick = { navController.navigateUp() },
        )
    }
}
