package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.choosefactor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.choosefactor.ChooseFactorSourceViewModel.State
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.card.title
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.SecurityFactorTypesListPreviewProvider
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.SecurityFactorTypesListView
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.SelectableFactorSourcesListView
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.presentation.ui.model.factors.SecurityFactorTypeUiItem
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleStokenet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

@Composable
fun ChooseFactorSourceBottomSheet(
    modifier: Modifier = Modifier,
    unusableFactorSourceKinds: PersistentList<FactorSourceKind> = persistentListOf(),
    alreadySelectedFactorSources: PersistentList<FactorSourceId> = persistentListOf(),
    unusableFactorSources: PersistentList<FactorSourceId> = persistentListOf(),
    viewModel: ChooseFactorSourceViewModel,
    onContinueClick: (factorSourceCard: FactorSourceCard) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onDismissSheet: () -> Unit,
) {
    LaunchedEffect(unusableFactorSources) {
        viewModel.initData(
            unusableFactorSourceKinds = unusableFactorSourceKinds,
            alreadySelectedFactorSources = alreadySelectedFactorSources,
            unusableFactorSources = unusableFactorSources
        )
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

    ChooseFactorSourceContent(
        modifier = modifier,
        state = state,
        onSecurityFactorTypeClick = viewModel::onSecurityFactorTypeClick,
        onFactorSourceSelect = viewModel::onFactorSourceSelect,
        onAddFactorSourceClick = viewModel::onAddFactorSourceClick,
        onContinueClick = viewModel::onSelectedFactorSourceConfirm,
        onInfoClick = onInfoClick,
        onSheetBackClick = viewModel::onSheetBackClick,
        onSheetCloseClick = viewModel::onSheetCloseClick
    )
}

@Composable
private fun ChooseFactorSourceContent(
    modifier: Modifier = Modifier,
    state: State,
    onSecurityFactorTypeClick: (SecurityFactorTypeUiItem.Item) -> Unit,
    onFactorSourceSelect: (FactorSourceCard) -> Unit,
    onAddFactorSourceClick: (FactorSourceKind) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onContinueClick: () -> Unit,
    onSheetBackClick: () -> Unit,
    onSheetCloseClick: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = state.currentPagePosition,
        pageCount = { state.pages.size }
    )

    LaunchedEffect(state.currentPagePosition) {
        if (state.currentPagePosition == state.selectTypePagePosition) {
            // animation is abnormal when navigating back thus scrollToPage works as expected
            pagerState.scrollToPage(state.currentPagePosition)
        } else {
            pagerState.animateScrollToPage(state.currentPagePosition)
        }
    }

    // TODO this will be replaced with the DefaultModalSheetLayout only when this bug is really fixed.
    // https://issuetracker.google.com/issues/278216859
    BottomSheetDialogWrapper(
        modifier = modifier,
        addScrim = true,
        sheetBackgroundColor = if (pagerState.currentPage == state.selectTypePagePosition) {
            RadixTheme.colors.background
        } else {
            RadixTheme.colors.backgroundSecondary
        },
        headerBackIcon = if (pagerState.currentPage == state.selectTypePagePosition) {
            Icons.Filled.Clear
        } else {
            Icons.AutoMirrored.Filled.ArrowBack
        },
        title = when (val page = state.pages[pagerState.currentPage]) {
            is State.Page.SelectFactorSourceType -> stringResource(R.string.securityFactors_selectFactor_title)
            is State.Page.SelectFactorSource -> page.kind.title()
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
            userScrollEnabled = false,
            verticalAlignment = Alignment.Top
        ) { pageIndex ->
            when (val currentPage = state.pages[pageIndex]) {
                is State.Page.SelectFactorSourceType -> SecurityFactorTypesListView(
                    items = currentPage.items,
                    onSecurityFactorTypeItemClick = onSecurityFactorTypeClick,
                    onInfoClick = onInfoClick
                )
                is State.Page.SelectFactorSource -> SelectableFactorSourcesListView(
                    factorSourceKind = currentPage.kind,
                    factorSources = currentPage.items,
                    isButtonEnabled = state.isButtonEnabled,
                    onFactorSourceSelect = onFactorSourceSelect,
                    onAddFactorSourceClick = { onAddFactorSourceClick(currentPage.kind) },
                    onContinueClick = onContinueClick
                )
            }
        }
    }
}

@Preview
@Composable
@UsesSampleValues
private fun ChooseFactorSourceBottomSheetPreviewLight(
    @PreviewParameter(ChooseFactorSourcePreviewProvider::class) state: State
) {
    RadixWalletPreviewTheme {
        ChooseFactorSourceContent(
            modifier = Modifier,
            state = state,
            onSecurityFactorTypeClick = {},
            onFactorSourceSelect = {},
            onAddFactorSourceClick = {},
            onInfoClick = {},
            onContinueClick = {},
            onSheetBackClick = {},
            onSheetCloseClick = {}
        )
    }
}

@Preview
@Composable
@UsesSampleValues
private fun ChooseFactorSourceBottomSheetPreviewDark(
    @PreviewParameter(ChooseFactorSourcePreviewProvider::class) state: State
) {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        ChooseFactorSourceContent(
            modifier = Modifier,
            state = state,
            onSecurityFactorTypeClick = {},
            onFactorSourceSelect = {},
            onAddFactorSourceClick = {},
            onInfoClick = {},
            onContinueClick = {},
            onSheetBackClick = {},
            onSheetCloseClick = {}
        )
    }
}

@Preview
@Composable
@UsesSampleValues
private fun DeviceFactorsBottomSheetPreview(
    @PreviewParameter(ChooseFactorSourcePreviewProvider::class) state: State
) {
    RadixWalletPreviewTheme {
        ChooseFactorSourceContent(
            modifier = Modifier,
            state = state,
            onSecurityFactorTypeClick = {},
            onFactorSourceSelect = {},
            onAddFactorSourceClick = {},
            onInfoClick = {},
            onContinueClick = {},
            onSheetBackClick = {},
            onSheetCloseClick = {}
        )
    }
}

@Preview
@Composable
@UsesSampleValues
private fun DeviceFactorsBottomSheetPreviewDark(
    @PreviewParameter(ChooseFactorSourcePreviewProvider::class) state: State
) {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        ChooseFactorSourceContent(
            modifier = Modifier,
            state = state,
            onSecurityFactorTypeClick = {},
            onFactorSourceSelect = {},
            onAddFactorSourceClick = {},
            onInfoClick = {},
            onContinueClick = {},
            onSheetBackClick = {},
            onSheetCloseClick = {}
        )
    }
}

@UsesSampleValues
class ChooseFactorSourcePreviewProvider : PreviewParameterProvider<State> {

    private val securityFactorTypeItems = SecurityFactorTypesListPreviewProvider().value
    private val availableFactorSources = mapOf(
        FactorSourceKind.DEVICE to persistentListOf(
            Selectable(
                DeviceFactorSource.sample().asGeneral().toFactorSourceCard(
                    messages = persistentListOf(FactorSourceStatusMessage.NoSecurityIssues),
                    accounts = persistentListOf(
                        Account.sampleMainnet()
                    ),
                    personas = persistentListOf(
                        Persona.sampleMainnet(),
                        Persona.sampleStokenet()
                    )
                )
            ),
            Selectable(
                DeviceFactorSource.sample.other().asGeneral().toFactorSourceCard(
                    messages = persistentListOf(FactorSourceStatusMessage.SecurityPrompt.LostFactorSource),
                    accounts = persistentListOf(
                        Account.sampleMainnet()
                    ),
                    personas = persistentListOf(
                        Persona.sampleMainnet(),
                        Persona.sampleStokenet()
                    )
                )
            ),
            Selectable(
                DeviceFactorSource.sample().asGeneral().toFactorSourceCard(
                    messages = persistentListOf(FactorSourceStatusMessage.SecurityPrompt.WriteDownSeedPhrase),
                )
            ),
            Selectable(
                DeviceFactorSource.sample.other().asGeneral().toFactorSourceCard(
                    accounts = persistentListOf(
                        Account.sampleMainnet()
                    ),
                    personas = persistentListOf(
                        Persona.sampleMainnet(),
                    )
                )
            )
        ),
        FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET to persistentListOf(
            Selectable(
                LedgerHardwareWalletFactorSource.sample().asGeneral().toFactorSourceCard(
                    messages = persistentListOf(FactorSourceStatusMessage.NoSecurityIssues),
                    accounts = persistentListOf(
                        Account.sampleMainnet()
                    ),
                    personas = persistentListOf(
                        Persona.sampleMainnet()
                    )
                )
            ),
            Selectable(
                LedgerHardwareWalletFactorSource.sample.other().asGeneral().toFactorSourceCard(
                    messages = persistentListOf(FactorSourceStatusMessage.SecurityPrompt.LostFactorSource)
                )
            )
        )
    ).toPersistentMap()

    private val pages = listOf(
        State.Page.SelectFactorSourceType(
            items = securityFactorTypeItems
        )
    ) + FactorSourceKind.entries.map {
        State.Page.SelectFactorSource(
            kind = it,
            items = availableFactorSources[it] ?: persistentListOf()
        )
    }

    override val values: Sequence<State>
        get() = sequenceOf(
            State(
                pages = pages.toPersistentList(),
                currentPagePosition = 0
            ),
            State(
                pages = pages.toPersistentList(),
                currentPagePosition = 1
            )
        )
}
