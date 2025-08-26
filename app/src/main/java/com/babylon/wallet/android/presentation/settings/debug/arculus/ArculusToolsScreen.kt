package com.babylon.wallet.android.presentation.settings.debug.arculus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

@Composable
fun ArculusToolsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: ArculusToolsViewModel
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
