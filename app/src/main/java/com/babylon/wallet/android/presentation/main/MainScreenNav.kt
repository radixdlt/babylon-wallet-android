package com.babylon.wallet.android.presentation.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.radixdlt.sargon.Account
import kotlinx.coroutines.flow.StateFlow

const val MAIN_ROUTE = "main"

@Suppress("LongParameterList")
fun NavGraphBuilder.main(
    mainUiState: StateFlow<MainViewModel.State>,
    onMenuClick: () -> Unit,
    onAccountClick: (Account) -> Unit,
    onNavigateToSecurityCenter: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onNavigateToOnBoarding: () -> Unit,
    onNavigateToIncompatibleProfile: () -> Unit,
    showNPSSurvey: () -> Unit,
    onNavigateToRelinkConnectors: () -> Unit,
    onNavigateToConnectCloudBackup: () -> Unit,
    onNavigateToLinkConnector: () -> Unit
) {
    composable(
        route = MAIN_ROUTE,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        MainScreen(
            mainUiState = mainUiState,
            onMenuClick = onMenuClick,
            onAccountClick = onAccountClick,
            onAccountCreationClick = onAccountCreationClick,
            onNavigateToOnBoarding = onNavigateToOnBoarding,
            onNavigateToIncompatibleProfile = onNavigateToIncompatibleProfile,
            onNavigateToSecurityCenter = onNavigateToSecurityCenter,
            showNPSSurvey = showNPSSurvey,
            onNavigateToRelinkConnectors = onNavigateToRelinkConnectors,
            onNavigateToConnectCloudBackup = onNavigateToConnectCloudBackup,
            onNavigateToLinkConnector = onNavigateToLinkConnector
        )
    }
}
