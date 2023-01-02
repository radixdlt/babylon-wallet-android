package com.babylon.wallet.android.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onSettingClick: (SettingSectionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state
    SettingsContent(
        onBackClick = onBackClick,
        appSettings = state.settings,
        onSettingClick = onSettingClick,
        modifier = modifier
            .systemBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
    )
}

@Composable
private fun SettingsContent(
    onBackClick: () -> Unit,
    appSettings: ImmutableList<SettingSection>,
    onSettingClick: (SettingSectionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.settings),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1
        )
        Column(
            Modifier
                .fillMaxSize()
                .background(RadixTheme.colors.gray5)

        ) {
            LazyColumn {
                appSettings.forEach { section ->
                    val description = section.type.descriptionRes()
                    item {
                        if (description != null) {
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                            Text(
                                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                text = stringResource(id = description),
                                style = RadixTheme.typography.body2Regular,
                                color = RadixTheme.colors.gray2
                            )
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                        } else {
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                        }
                    }
                    section.items.forEach { settingItem ->
                        item {
                            val textColor =
                                if (settingItem is SettingSectionItem.DeleteAll) {
                                    RadixTheme.colors.red1
                                } else {
                                    RadixTheme.colors.gray1
                                }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .background(RadixTheme.colors.defaultBackground)
                                    .clickable { onSettingClick(settingItem) }
                                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                                verticalAlignment = CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = settingItem.descriptionRes()),
                                    style = RadixTheme.typography.body2Header,
                                    color = textColor
                                )
                            }
                            Divider(color = RadixTheme.colors.gray5)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenWithoutActiveConnectionPreview() {
    RadixWalletTheme {
        SettingsContent(
            onBackClick = {},
            appSettings = defaultAppSettings,
            onSettingClick = {}
        )
    }
}
