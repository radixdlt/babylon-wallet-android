package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.SecurityProblem
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem.SecurityFactorCategory
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.SecurityFactorTypes
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.currentSecurityFactorTypeItems
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet

@Composable
fun SecurityFactorsScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityFactorsViewModel,
    onSecurityFactorSettingItemClick: (SecurityFactorsSettingsItem) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SecurityFactorsContent(
        modifier = modifier.fillMaxSize(),
        securityFactorSettingItems = state.securityFactorSettingItems,
        onSecurityFactorSettingItemClick = onSecurityFactorSettingItemClick,
        onBackClick = onBackClick,
    )
}

@Composable
private fun SecurityFactorsContent(
    modifier: Modifier = Modifier,
    securityFactorSettingItems: ImmutableMap<SecurityFactorCategory, ImmutableSet<SecurityFactorsSettingsItem>>,
    onSecurityFactorSettingItemClick: (SecurityFactorsSettingsItem) -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(id = R.string.securityFactors_title),
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )
                HorizontalDivider(color = RadixTheme.colors.gray4)
            }
        }
    ) { padding ->
        SecurityFactorTypes(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            isDescriptionVisible = true,
            securityFactorSettingItems = securityFactorSettingItems,
            onSecurityFactorSettingItemClick = onSecurityFactorSettingItemClick
        )
    }
}

@Composable
fun getSecurityWarnings(securityFactorsSettingsItem: SecurityFactorsSettingsItem.BiometricsPin): PersistentList<String> {
    return mutableListOf<String>().apply {
        securityFactorsSettingsItem.securityProblems.forEach { problem ->
            when (problem) {
                is SecurityProblem.EntitiesNotRecoverable -> {
                    add(stringResource(id = R.string.securityProblems_no3_securityFactors))
                }

                is SecurityProblem.SeedPhraseNeedRecovery -> {
                    add(stringResource(id = R.string.securityProblems_no9_securityFactors))
                }

                else -> {}
            }
        }
    }.toPersistentList()
}

@Preview(showBackground = true)
@Composable
private fun SecurityFactorsPreview() {
    RadixWalletTheme {
        SecurityFactorsContent(
            modifier = Modifier,
            securityFactorSettingItems = currentSecurityFactorTypeItems,
            onSecurityFactorSettingItemClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SecurityFactorsWithSecurityProblemsPreview() {
    RadixWalletTheme {
        SecurityFactorsContent(
            modifier = Modifier,
            securityFactorSettingItems = persistentMapOf(
                SecurityFactorCategory.Own to persistentSetOf(
                    SecurityFactorsSettingsItem.BiometricsPin(
                        securityProblems = setOf(
                            SecurityProblem.SeedPhraseNeedRecovery(isAnyActivePersonaAffected = true),
                            SecurityProblem.EntitiesNotRecoverable(
                                accountsNeedBackup = 7,
                                personasNeedBackup = 2,
                                hiddenAccountsNeedBackup = 1,
                                hiddenPersonasNeedBackup = 3
                            )
                        ).toPersistentSet()
                    )
                ),
                SecurityFactorCategory.Hardware to persistentSetOf(
                    SecurityFactorsSettingsItem.LedgerNano
                )
            ),
            onSecurityFactorSettingItemClick = {},
            onBackClick = {}
        )
    }
}
