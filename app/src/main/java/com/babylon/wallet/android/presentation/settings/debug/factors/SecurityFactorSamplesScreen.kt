package com.babylon.wallet.android.presentation.settings.debug.factors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.choosefactor.ChooseFactorSourceBottomSheet
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceKindCardView
import com.babylon.wallet.android.presentation.ui.composables.card.RemovableFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableMultiChoiceFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableSingleChoiceFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableSingleChoiceFactorSourceKindCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceKindCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.presentation.ui.model.shared.StatusMessage
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleStokenet
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SecurityFactorSamplesScreen(
    viewModel: SecurityFactorSamplesViewModel,
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit
) {
    val state = viewModel.state.collectAsState().value

    SecurityFactorSamplesContent(
        state = state,
        onBackClick = onBackClick,
        onSelectFactorSourceKind = viewModel::onSelectFactorSourceKind,
        onSelectFactorSource = viewModel::onSelectFactorSource,
        onCheckedChange = viewModel::onCheckedChange,
        onRemoveClick = viewModel::onRemoveClick,
        onChooseFactorSourceClick = viewModel::onChooseFactorSourceClick
    )

    if (state.isBottomSheetVisible) {
        ChooseFactorSourceBottomSheet(
            viewModel = hiltViewModel(),
            onContinueClick = viewModel::onSelectedFactorSourceConfirm,
            onInfoClick = onInfoClick,
            onDismissSheet = viewModel::onSheetClosed
        )
    }
}

@Composable
private fun SecurityFactorSamplesContent(
    modifier: Modifier = Modifier,
    state: SecurityFactorSamplesViewModel.State,
    onBackClick: () -> Unit,
    onSelectFactorSourceKind: (FactorSourceKindCard) -> Unit,
    onSelectFactorSource: (FactorSourceCard) -> Unit,
    onCheckedChange: (FactorSourceCard, Boolean) -> Unit,
    onRemoveClick: (FactorSourceCard) -> Unit,
    onChooseFactorSourceClick: () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.securityFactors_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            items(state.displayOnlyFactorSourceItems) {
                FactorSourceCardView(
                    item = it
                )
            }

            items(state.displayOnlyFactorSourceKindItems) {
                FactorSourceKindCardView(
                    item = it
                )
            }

            items(state.singleChoiceFactorSourceKindItems) {
                SelectableSingleChoiceFactorSourceKindCard(
                    item = it,
                    onSelect = onSelectFactorSourceKind
                )
            }

            items(state.singleChoiceFactorSourceItems) {
                SelectableSingleChoiceFactorSourceCard(
                    item = it,
                    onSelect = onSelectFactorSource
                )
            }

            items(state.multiChoiceItems) {
                SelectableMultiChoiceFactorSourceCard(
                    item = it,
                    onCheckedChange = onCheckedChange
                )
            }

            items(state.removableItems) {
                RemovableFactorSourceCard(
                    item = it,
                    onRemoveClick = onRemoveClick
                )
            }

            item {
                RadixPrimaryButton(
                    text = "Choose Factor Source",
                    onClick = onChooseFactorSourceClick
                )
            }
        }
    }
}

@Composable
@Preview
@UsesSampleValues
private fun SecurityFactorSamplesPreview() {
    RadixWalletPreviewTheme {
        SecurityFactorSamplesContent(
            state = SecurityFactorSamplesViewModel.State(
                displayOnlyFactorSourceItems = persistentListOf(
                    FactorSourceCard(
                        id = FactorSourceId.Hash.init(
                            kind = FactorSourceKind.DEVICE,
                            mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                        ),
                        name = "My Phone",
                        includeDescription = false,
                        lastUsedOn = "Today",
                        kind = FactorSourceKind.DEVICE,
                        messages = persistentListOf(
                            FactorSourceStatusMessage.PassphraseHint,
                            FactorSourceStatusMessage.Dynamic(
                                message = StatusMessage(
                                    message = "Warning text",
                                    type = StatusMessage.Type.WARNING
                                )
                            )
                        ),
                        accounts = persistentListOf(
                            Account.sampleMainnet()
                        ),
                        personas = persistentListOf(
                            Persona.sampleMainnet(),
                            Persona.sampleStokenet()
                        ),
                        hasHiddenEntities = true,
                        supportsBabylon = true,
                        isEnabled = true
                    )
                ),
                displayOnlyFactorSourceKindItems = persistentListOf(
                    FactorSourceKindCard(
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        messages = persistentListOf()
                    ),
                    FactorSourceKindCard(
                        kind = FactorSourceKind.ARCULUS_CARD,
                        messages = persistentListOf()
                    )
                ),
                singleChoiceFactorSourceKindItems = persistentListOf(
                    Selectable(
                        data = FactorSourceKindCard(
                            kind = FactorSourceKind.DEVICE,
                            messages = persistentListOf()
                        )
                    ),
                    Selectable(
                        data = FactorSourceKindCard(
                            kind = FactorSourceKind.ARCULUS_CARD,
                            messages = persistentListOf()
                        )
                    )
                ),
                singleChoiceFactorSourceItems = persistentListOf(
                    Selectable(
                        data = FactorSourceCard(
                            id = FactorSourceId.Hash.init(
                                kind = FactorSourceKind.DEVICE,
                                mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                            ),
                            name = "My Phone 666",
                            includeDescription = false,
                            lastUsedOn = "Today",
                            kind = FactorSourceKind.DEVICE,
                            messages = persistentListOf(),
                            accounts = persistentListOf(),
                            personas = persistentListOf(),
                            hasHiddenEntities = false,
                            supportsBabylon = true,
                            isEnabled = true
                        )
                    )
                ),
                multiChoiceItems = persistentListOf(
                    Selectable(
                        data = FactorSourceCard(
                            id = FactorSourceId.Hash.init(
                                kind = FactorSourceKind.DEVICE,
                                mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                            ),
                            name = "My Phone",
                            includeDescription = false,
                            lastUsedOn = "Today",
                            kind = FactorSourceKind.DEVICE,
                            messages = persistentListOf(),
                            accounts = persistentListOf(),
                            personas = persistentListOf(),
                            hasHiddenEntities = false,
                            supportsBabylon = true,
                            isEnabled = true
                        )
                    )
                )
            ),
            onBackClick = {},
            onSelectFactorSourceKind = {},
            onSelectFactorSource = {},
            onCheckedChange = { _, _ -> },
            onRemoveClick = {},
            onChooseFactorSourceClick = {}
        )
    }
}
