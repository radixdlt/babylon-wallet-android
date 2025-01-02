package com.babylon.wallet.android.presentation.ui.composables.securityfactors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem.SecurityFactorCategory
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.getSecurityWarnings
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
fun SecurityFactorTypesListView(
    modifier: Modifier = Modifier,
    isDescriptionVisible: Boolean = false,
    securityFactorSettingItems: ImmutableMap<SecurityFactorCategory, ImmutableSet<SecurityFactorsSettingsItem>>,
    onSecurityFactorSettingItemClick: (SecurityFactorsSettingsItem) -> Unit
) {
    LazyColumn(
        modifier = modifier.background(color = RadixTheme.colors.gray5)
    ) {
        if (isDescriptionVisible) {
            item {
                Text(
                    text = stringResource(id = R.string.securityFactors_subtitle),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray2,
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
                )
            }
        } else {
            item {
                HorizontalDivider(color = RadixTheme.colors.gray4)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXXLarge))
            }
        }
        securityFactorSettingItems.forEach { (category, securityFactorsItems) ->
            // Add a header for the security factor category if any
            val categoryTitleRes = category.titleRes()
            categoryTitleRes?.let {
                item {
                    Text(
                        text = stringResource(id = categoryTitleRes),
                        style = RadixTheme.typography.body1Header,
                        color = RadixTheme.colors.gray2,
                        modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
                    )
                }
            }
            // Add security factor items
            items(securityFactorsItems.toList()) { securityFactorsItem ->
                val isLastItem = securityFactorsItem == securityFactorsItems.last()
                DefaultSettingsItem(
                    title = stringResource(id = securityFactorsItem.titleRes()),
                    subtitle = stringResource(id = securityFactorsItem.subtitleRes()),
                    leadingIconRes = securityFactorsItem.getIcon(),
                    onClick = {
                        onSecurityFactorSettingItemClick(securityFactorsItem)
                    },
                    warnings = if (securityFactorsItem is SecurityFactorsSettingsItem.BiometricsPin) {
                        getSecurityWarnings(securityFactorsSettingsItem = securityFactorsItem)
                    } else {
                        null
                    }
                )
                if (isLastItem.not() || isDescriptionVisible.not()) {
                    HorizontalDivider(
                        modifier = Modifier
                            .background(color = RadixTheme.colors.defaultBackground)
                            .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                        color = RadixTheme.colors.gray4
                    )
                } else {
                    HorizontalDivider(color = RadixTheme.colors.gray4)
                }
            }
        }
        item {
            Spacer(modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault))
        }
    }
}

val currentSecurityFactorTypeItems = if (BuildConfig.EXPERIMENTAL_FEATURES_ENABLED) {
    persistentMapOf(
        SecurityFactorCategory.Own to persistentSetOf(SecurityFactorsSettingsItem.BiometricsPin(persistentSetOf())),
        SecurityFactorCategory.Hardware to persistentSetOf(
            SecurityFactorsSettingsItem.ArculusCard,
            SecurityFactorsSettingsItem.LedgerNano
        ),
        SecurityFactorCategory.Information to persistentSetOf(
            SecurityFactorsSettingsItem.Password,
            SecurityFactorsSettingsItem.Passphrase
        )
    )
} else {
    persistentMapOf(
        SecurityFactorCategory.Own to persistentSetOf(SecurityFactorsSettingsItem.BiometricsPin(persistentSetOf())),
        SecurityFactorCategory.Hardware to persistentSetOf(
            SecurityFactorsSettingsItem.LedgerNano
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun SecurityFactorTypesListPreview() {
    RadixWalletTheme {
        SecurityFactorTypesListView(
            securityFactorSettingItems = currentSecurityFactorTypeItems,
            onSecurityFactorSettingItemClick = {}
        )
    }
}
