package com.babylon.wallet.android.presentation.settings.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

private const val ROUTE_ARCULUS_TOOLS = "debug_arculus_tools"

fun NavController.arculusTools() {
    navigate(ROUTE_ARCULUS_TOOLS)
}

fun NavGraphBuilder.arculusTools(
    onBackClick: () -> Unit
) {
    composable(route = ROUTE_ARCULUS_TOOLS) {
        ArculusToolsScreen(onBackClick = onBackClick)
    }
}

@Composable
private fun ArculusToolsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: ArculusToolsViewModel = hiltViewModel()
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = "Arculus Tools",
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            DefaultSettingsItem(
                title = "Validate min firmware version",
                onClick = viewModel::onValidateMinFirmwareVersionClick
            )
        }
    }
}
