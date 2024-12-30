package com.babylon.wallet.android.presentation.ui.composables.securityfactors

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem.SecurityFactorCategory
import com.babylon.wallet.android.presentation.settings.debug.factors.SecurityFactorSamplesViewModel
import com.babylon.wallet.android.presentation.settings.debug.factors.SecurityFactorSamplesViewModel.Companion.availableFactorSources
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.annotation.UsesSampleValues
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChooseFactorSourceBottomSheet(
    modifier: Modifier = Modifier,
    securityFactorTypeItems: ImmutableMap<SecurityFactorCategory, ImmutableSet<SecurityFactorsSettingsItem>>,
    pages: PersistentList<SecurityFactorSamplesViewModel.State.Page>,
    currentPagePosition: Int,
    factorSources: PersistentMap<FactorSourceKind, PersistentList<Selectable<FactorSourceCard>>>,
    onSecurityFactorTypeClick: (SecurityFactorsSettingsItem) -> Unit,
    onFactorSourceSelect: (FactorSourceCard) -> Unit,
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit,
    onDismissSheet: () -> Unit,
) {
    BackHandler(onBack = onBackClick)

    val pagerState = rememberPagerState(
        pageCount = { pages.count() }
    )

    LaunchedEffect(currentPagePosition) {
        if (currentPagePosition == SecurityFactorSamplesViewModel.State.Page.SelectFactorSourceType.ordinal) {
            // animation is abnormal when navigating back thus scrollToPage works as expected
            pagerState.scrollToPage(currentPagePosition)
        } else {
            pagerState.animateScrollToPage(currentPagePosition)
        }
    }

    BottomSheetDialogWrapper(
        modifier = modifier,
        addScrim = true,
        sheetBackgroundColor = if (pagerState.currentPage == SecurityFactorSamplesViewModel.State.Page.SelectFactorSourceType.ordinal) {
            RadixTheme.colors.defaultBackground
        } else {
            RadixTheme.colors.gray5
        },
        headerBackIcon = if (pagerState.currentPage == SecurityFactorSamplesViewModel.State.Page.SelectFactorSourceType.ordinal) {
            Icons.Filled.Clear
        } else {
            Icons.AutoMirrored.Filled.ArrowBack
        },
        title = when (pages[pagerState.currentPage]) { // TODO strings
            SecurityFactorSamplesViewModel.State.Page.SelectFactorSourceType -> "Select Factor Type"
            SecurityFactorSamplesViewModel.State.Page.BiometricsPin -> "Biometrics"
            SecurityFactorSamplesViewModel.State.Page.LedgerNano -> "Ledger nano"
            SecurityFactorSamplesViewModel.State.Page.ArculusCard -> "Arculus card"
            SecurityFactorSamplesViewModel.State.Page.Password -> "Password"
            SecurityFactorSamplesViewModel.State.Page.Passphrase -> "Passphrase"
        },
        isDismissible = false,
        onHeaderBackIconClick = onBackClick,
        onDismiss = onDismissSheet
    ) {
        HorizontalPager(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .imePadding(),
            state = pagerState,
            userScrollEnabled = false
        ) { pageIndex ->
            val currentPage = pages[pageIndex]
            when (currentPage) {
                SecurityFactorSamplesViewModel.State.Page.SelectFactorSourceType -> {
                    SecurityFactorTypes(
                        securityFactorSettingItems = securityFactorTypeItems,
                        onSecurityFactorSettingItemClick = onSecurityFactorTypeClick
                    )
                }
                SecurityFactorSamplesViewModel.State.Page.BiometricsPin -> {
                    SelectableFactorSourcesList(
                        factorSources = factorSources[FactorSourceKind.DEVICE] ?: persistentListOf(),
                        factorSourceDescriptionText = R.string.factorSources_card_deviceDescription,
                        addFactorSourceButtonTitle = R.string.factorSources_list_deviceAdd,
                        onFactorSourceSelect = onFactorSourceSelect,
                        onAddFactorSourceClick = {},
                        onContinueClick = onContinueClick
                    )
                }
                SecurityFactorSamplesViewModel.State.Page.LedgerNano -> {
                    SelectableFactorSourcesList(
                        factorSources = factorSources[FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET] ?: persistentListOf(),
                        factorSourceDescriptionText = R.string.factorSources_card_ledgerDescription,
                        addFactorSourceButtonTitle = R.string.factorSources_list_ledgerAdd,
                        onFactorSourceSelect = onFactorSourceSelect,
                        onAddFactorSourceClick = {},
                        onContinueClick = onContinueClick
                    )
                }
                SecurityFactorSamplesViewModel.State.Page.ArculusCard -> {
                    Column {
                        Text("Arculus")
                    }
                }
                SecurityFactorSamplesViewModel.State.Page.Password -> {
                    Column {
                        Text("Password")
                    }
                }
                SecurityFactorSamplesViewModel.State.Page.Passphrase -> {
                    Column {
                        Text("Passhprase")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
@UsesSampleValues
private fun SelectFactorTypeBottomSheetPreview() {
    RadixWalletPreviewTheme {
        ChooseFactorSourceBottomSheet(
            modifier = Modifier,
            pages = SecurityFactorSamplesViewModel.State.Page.entries.toPersistentList(),
            currentPagePosition = SecurityFactorSamplesViewModel.State.Page.SelectFactorSourceType.ordinal,
            factorSources = persistentMapOf(),
            securityFactorTypeItems = currentSecurityFactorTypeItems,
            onSecurityFactorTypeClick = {},
            onFactorSourceSelect = {},
            onContinueClick = {},
            onBackClick = {},
            onDismissSheet = {}
        )
    }
}

@Preview
@Composable
@UsesSampleValues
private fun SelectDeviceFactorBottomSheetPreview() {
    RadixWalletPreviewTheme {
        ChooseFactorSourceBottomSheet(
            modifier = Modifier,
            pages = SecurityFactorSamplesViewModel.State.Page.entries.toPersistentList(),
            currentPagePosition = SecurityFactorSamplesViewModel.State.Page.BiometricsPin.ordinal,
            factorSources = availableFactorSources,
            securityFactorTypeItems = currentSecurityFactorTypeItems,
            onSecurityFactorTypeClick = {},
            onFactorSourceSelect = {},
            onContinueClick = {},
            onBackClick = {},
            onDismissSheet = {}
        )
    }
}
