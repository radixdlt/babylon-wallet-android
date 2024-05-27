package com.babylon.wallet.android.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.wallet.WalletScreen
import com.radixdlt.sargon.Account
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    mainUiState: StateFlow<MainUiState>,
    onMenuClick: () -> Unit,
    onAccountClick: (Account) -> Unit = { },
    onNavigateToSecurityCenter: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onNavigateToOnBoarding: () -> Unit,
    onNavigateToIncompatibleProfile: () -> Unit,
    showNPSSurvey: () -> Unit,
    onNavigateToRelinkConnectors: () -> Unit,
    onNavigateToConnectCloudBackup: () -> Unit
) {
    val state by mainUiState.collectAsStateWithLifecycle()
    when (state.initialAppState) {
        is AppState.Wallet -> {
            WalletScreen(
                modifier = modifier,
                viewModel = hiltViewModel(),
                onMenuClick = onMenuClick,
                onAccountClick = onAccountClick,
                onAccountCreationClick = onAccountCreationClick,
                onNavigateToSecurityCenter = onNavigateToSecurityCenter,
                showNPSSurvey = showNPSSurvey,
                onNavigateToRelinkConnectors = onNavigateToRelinkConnectors,
                onNavigateToConnectCloudBackup = onNavigateToConnectCloudBackup
            )
        }

        is AppState.IncompatibleProfile -> {
            LaunchedEffect(Unit) {
                onNavigateToIncompatibleProfile()
            }
        }

        is AppState.Loading -> {
            FullscreenCircularProgressContent()
        }

        is AppState.OnBoarding -> {
            LaunchedEffect(Unit) {
                onNavigateToOnBoarding()
            }
        }
    }
}
