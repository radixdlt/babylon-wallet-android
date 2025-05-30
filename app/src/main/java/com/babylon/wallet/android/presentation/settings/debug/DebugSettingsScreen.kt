package com.babylon.wallet.android.presentation.settings.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SwitchSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

@Composable
fun DebugSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: DebugSettingsViewModel,
    onBackClick: () -> Unit,
    onItemClick: (SettingsItem.DebugSettingsItem) -> Unit
) {
    val linkConnectionStatusIndicatorState by viewModel.linkConnectionStatusIndicatorState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.settings_debugSettings),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.Start
        ) {
            HorizontalDivider(color = RadixTheme.colors.divider)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                SettingsItem.DebugSettingsItem.values().forEach { debugSettingsItem ->
                    item {
                        when (debugSettingsItem) {
                            SettingsItem.DebugSettingsItem.LinkConnectionStatusIndicator -> {
                                SwitchSettingsItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(all = RadixTheme.dimensions.paddingDefault),
                                    titleRes = debugSettingsItem.titleRes(),
                                    iconResource = debugSettingsItem.getIcon(),
                                    checked = linkConnectionStatusIndicatorState.isEnabled,
                                    onCheckedChange = viewModel::onLinkConnectionStatusIndicatorToggled
                                )
                                HorizontalDivider(color = RadixTheme.colors.divider)
                            }

                            else -> {
                                DefaultSettingsItem(
                                    title = stringResource(id = debugSettingsItem.titleRes()),
                                    leadingIconRes = debugSettingsItem.getIcon(),
                                    onClick = {
                                        onItemClick(debugSettingsItem)
                                    }
                                )
                                HorizontalDivider(color = RadixTheme.colors.divider)
                            }
                        }
                    }
                }
            }
        }
    }
}
