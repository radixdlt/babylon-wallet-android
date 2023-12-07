package com.babylon.wallet.android.presentation.settings.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar

@Composable
fun DebugSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onItemClick: (SettingsItem.DebugSettingsItem) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.settings_debugSettings),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.Start
        ) {
            HorizontalDivider(color = RadixTheme.colors.gray5)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                SettingsItem.DebugSettingsItem.values().forEachIndexed { index, appSettingsItem ->
                    item {
                        DefaultSettingsItem(
                            title = stringResource(id = appSettingsItem.descriptionRes()),
                            icon = appSettingsItem.getIcon(),
                            onClick = {
                                onItemClick(appSettingsItem)
                            }
                        )
                        HorizontalDivider(color = RadixTheme.colors.gray5)
                    }
                }
            }
        }
    }
}
