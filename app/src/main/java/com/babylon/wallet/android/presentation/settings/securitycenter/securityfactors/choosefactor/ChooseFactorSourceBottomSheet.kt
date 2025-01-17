package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.choosefactor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem
import com.babylon.wallet.android.presentation.settings.debug.factors.SecurityFactorSamplesViewModel.Companion.availableFactorSources
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.choosefactor.ChooseFactorSourceViewModel.State
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.SecurityFactorTypesListView
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.SelectableFactorSourcesListView
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.currentSecurityFactorTypeItems
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.annotation.UsesSampleValues
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ChooseFactorSourceBottomSheet(
    modifier: Modifier = Modifier,
    excludeFactorSources: PersistentList<FactorSourceId> = persistentListOf(),
    viewModel: ChooseFactorSourceViewModel,
    onContinueClick: (factorSourceCard: FactorSourceCard) -> Unit,
    onDismissSheet: () -> Unit,
) {
    LaunchedEffect(excludeFactorSources) {
        viewModel.initData(excludeFactorSources)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(onBack = viewModel::onSheetBackClick)

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                ChooseFactorSourceViewModel.Event.DismissSheet -> onDismissSheet()
                is ChooseFactorSourceViewModel.Event.SelectedFactorSourceConfirm -> onContinueClick(event.factorSourceCard)
            }
        }
    }

    val pagerState = rememberPagerState(
        pageCount = { state.pages.count() }
    )

    LaunchedEffect(state.currentPagePosition) {
        if (state.currentPagePosition == State.Page.SelectFactorSourceType.ordinal) {
            // animation is abnormal when navigating back thus scrollToPage works as expected
            pagerState.scrollToPage(state.currentPagePosition)
        } else {
            pagerState.animateScrollToPage(state.currentPagePosition)
        }
    }

    ChooseFactorSourceContent(
        modifier = modifier,
        state = state,
        pagerState = pagerState,
        onSecurityFactorTypeClick = viewModel::onSecurityFactorTypeClick,
        onFactorSourceSelect = viewModel::onFactorSourceFromSheetSelect,
        onAddFactorSourceClick = viewModel::onAddFactorSourceClick,
        onContinueClick = viewModel::onSelectedFactorSourceConfirm,
        onSheetBackClick = viewModel::onSheetBackClick,
        onSheetCloseClick = viewModel::onSheetCloseClick
    )
}

@Composable
private fun ChooseFactorSourceContent(
    modifier: Modifier = Modifier,
    state: State,
    pagerState: PagerState,
    onSecurityFactorTypeClick: (SecurityFactorsSettingsItem) -> Unit,
    onFactorSourceSelect: (FactorSourceCard) -> Unit,
    onAddFactorSourceClick: (FactorSourceKind) -> Unit,
    onContinueClick: () -> Unit,
    onSheetBackClick: () -> Unit,
    onSheetCloseClick: () -> Unit
) {
    BottomSheetDialogWrapper(
        modifier = modifier,
        addScrim = true,
        sheetBackgroundColor = if (pagerState.currentPage == State.Page.SelectFactorSourceType.ordinal) {
            RadixTheme.colors.defaultBackground
        } else {
            RadixTheme.colors.gray5
        },
        headerBackIcon = if (pagerState.currentPage == State.Page.SelectFactorSourceType.ordinal) {
            Icons.Filled.Clear
        } else {
            Icons.AutoMirrored.Filled.ArrowBack
        },
        title = when (state.pages[pagerState.currentPage]) {
            State.Page.SelectFactorSourceType -> stringResource(R.string.securityFactors_selectFactor_title)
            State.Page.BiometricsPin -> stringResource(R.string.factorSources_card_deviceTitle)
            State.Page.LedgerNano -> stringResource(R.string.factorSources_card_ledgerTitle)
            State.Page.ArculusCard -> stringResource(R.string.factorSources_card_arculusCardTitle)
            State.Page.Password -> stringResource(R.string.factorSources_card_passwordTitle)
            State.Page.Passphrase -> stringResource(R.string.factorSources_card_passphraseTitle)
        },
        isDismissible = false,
        onHeaderBackIconClick = onSheetBackClick,
        onDismiss = onSheetCloseClick
    ) {
        HorizontalPager(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .imePadding(),
            state = pagerState,
            userScrollEnabled = false
        ) { pageIndex ->
            val currentPage = state.pages[pageIndex]
            when (currentPage) {
                State.Page.SelectFactorSourceType -> {
                    SecurityFactorTypesListView(
                        securityFactorSettingItems = state.securityFactorTypeItems,
                        onSecurityFactorTypeItemClick = onSecurityFactorTypeClick
                    )
                }

                State.Page.BiometricsPin -> {
                    SelectableFactorSourcesListView(
                        factorSources = state.selectableFactorSources[FactorSourceKind.DEVICE] ?: persistentListOf(),
                        factorSourceDescriptionText = R.string.factorSources_card_deviceDescription,
                        addFactorSourceButtonTitle = R.string.factorSources_list_deviceAdd,
                        onFactorSourceSelect = onFactorSourceSelect,
                        onAddFactorSourceClick = { onAddFactorSourceClick(FactorSourceKind.DEVICE) },
                        onContinueClick = onContinueClick
                    )
                }

                State.Page.LedgerNano -> {
                    SelectableFactorSourcesListView(
                        factorSources = state.selectableFactorSources[FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET]
                            ?: persistentListOf(),
                        factorSourceDescriptionText = R.string.factorSources_card_ledgerDescription,
                        addFactorSourceButtonTitle = R.string.factorSources_list_ledgerAdd,
                        onFactorSourceSelect = onFactorSourceSelect,
                        onAddFactorSourceClick = { onAddFactorSourceClick(FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET) },
                        onContinueClick = onContinueClick
                    )
                }

                State.Page.ArculusCard -> {
                    SelectableFactorSourcesListView(
                        factorSources = state.selectableFactorSources[FactorSourceKind.ARCULUS_CARD] ?: persistentListOf(),
                        factorSourceDescriptionText = R.string.factorSources_card_arculusCardDescription,
                        addFactorSourceButtonTitle = R.string.factorSources_list_arculusCardAdd,
                        onFactorSourceSelect = onFactorSourceSelect,
                        onAddFactorSourceClick = { onAddFactorSourceClick(FactorSourceKind.ARCULUS_CARD) },
                        onContinueClick = onContinueClick
                    )
                }

                State.Page.Password -> {
                    SelectableFactorSourcesListView(
                        factorSources = state.selectableFactorSources[FactorSourceKind.PASSWORD] ?: persistentListOf(),
                        factorSourceDescriptionText = R.string.factorSources_card_passwordDescription,
                        addFactorSourceButtonTitle = R.string.factorSources_list_passwordAdd,
                        onFactorSourceSelect = onFactorSourceSelect,
                        onAddFactorSourceClick = { onAddFactorSourceClick(FactorSourceKind.PASSWORD) },
                        onContinueClick = onContinueClick
                    )
                }

                State.Page.Passphrase -> {
                    SelectableFactorSourcesListView(
                        factorSources = state.selectableFactorSources[FactorSourceKind.OFF_DEVICE_MNEMONIC] ?: persistentListOf(),
                        factorSourceDescriptionText = R.string.factorSources_card_offDeviceMnemonicDescription,
                        addFactorSourceButtonTitle = R.string.factorSources_list_offDeviceMnemonicAdd,
                        onFactorSourceSelect = onFactorSourceSelect,
                        onAddFactorSourceClick = { onAddFactorSourceClick(FactorSourceKind.OFF_DEVICE_MNEMONIC) },
                        onContinueClick = onContinueClick
                    )
                }
            }
        }
    }
}

@Preview
@Composable
@UsesSampleValues
private fun ChooseFactorSourceBottomSheetPreview() {
    RadixWalletPreviewTheme {
        ChooseFactorSourceContent(
            modifier = Modifier,
            state = State(
                currentPagePosition = State.Page.SelectFactorSourceType.ordinal,
                securityFactorTypeItems = currentSecurityFactorTypeItems,

            ),
            pagerState = rememberPagerState {
                State.Page.entries.count()
            },
            onSecurityFactorTypeClick = {},
            onFactorSourceSelect = {},
            onAddFactorSourceClick = {},
            onContinueClick = {},
            onSheetBackClick = {},
            onSheetCloseClick = {}
        )
    }
}

@Preview
@Composable
@UsesSampleValues
private fun DeviceFactorsBottomSheetPreview() {
    RadixWalletPreviewTheme {
        ChooseFactorSourceContent(
            modifier = Modifier,
            state = State(
                currentPagePosition = State.Page.BiometricsPin.ordinal,
                securityFactorTypeItems = currentSecurityFactorTypeItems,
                selectableFactorSources = availableFactorSources
            ),
            pagerState = rememberPagerState(initialPage = State.Page.BiometricsPin.ordinal) {
                State.Page.entries.count()
            },
            onSecurityFactorTypeClick = {},
            onFactorSourceSelect = {},
            onAddFactorSourceClick = {},
            onContinueClick = {},
            onSheetBackClick = {},
            onSheetCloseClick = {}
        )
    }
}
