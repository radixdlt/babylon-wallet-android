package com.babylon.wallet.android.presentation.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSourceId
import kotlinx.coroutines.flow.StateFlow

const val MAIN_ROUTE = "main"

@Suppress("LongParameterList")
fun NavGraphBuilder.main(
    mainUiState: StateFlow<MainUiState>,
    onMenuClick: () -> Unit,
    onAccountClick: (Account) -> Unit,
    onNavigateToMnemonicBackup: (FactorSourceId.Hash) -> Unit,
    onNavigateToMnemonicRestore: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onNavigateToOnBoarding: () -> Unit,
    onNavigateToIncompatibleProfile: () -> Unit,
    showNPSSurvey: () -> Unit
) {
    composable(route = MAIN_ROUTE) {
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
