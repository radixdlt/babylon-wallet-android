package com.babylon.wallet.android.presentation.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.coroutines.flow.StateFlow
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.model.pernetwork.Network

const val MAIN_ROUTE = "main"

@Suppress("LongParameterList")
fun NavGraphBuilder.main(
    mainUiState: StateFlow<MainUiState>,
    onMenuClick: () -> Unit,
    onAccountClick: (Network.Account) -> Unit,
    onNavigateToMnemonicBackup: (FactorSourceID.FromHash) -> Unit,
    onNavigateToMnemonicRestore: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onNavigateToOnBoarding: () -> Unit,
    onNavigateToIncompatibleProfile: () -> Unit,
    showNPSSurvey: () -> Unit
) {
    composable(route = MAIN_ROUTE) { entry ->
        MainScreen(
            mainUiState = mainUiState,
            onMenuClick = onMenuClick,
            onAccountClick = onAccountClick,
            onAccountCreationClick = onAccountCreationClick,
            onNavigateToOnBoarding = onNavigateToOnBoarding,
            onNavigateToIncompatibleProfile = onNavigateToIncompatibleProfile,
            onNavigateToMnemonicBackup = onNavigateToMnemonicBackup,
            onNavigateToMnemonicRestore = onNavigateToMnemonicRestore,
            showNPSSurvey = showNPSSurvey
        )
    }
}
