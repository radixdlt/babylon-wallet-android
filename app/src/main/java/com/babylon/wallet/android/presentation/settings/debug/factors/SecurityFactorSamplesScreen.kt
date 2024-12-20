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
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceInstanceCardView
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceKindCardView
import com.babylon.wallet.android.presentation.ui.composables.card.RemovableFactorSourceInstanceCard
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableMultiChoiceFactorSourceInstanceCard
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableSingleChoiceFactorSourceKindCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceInstanceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceKindCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.presentation.ui.model.factors.StatusMessage
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
    onBackClick: () -> Unit
) {
    val state = viewModel.state.collectAsState().value

    SecurityFactorSamplesContent(
        state = state,
        onBackClick = onBackClick,
        onSelect = viewModel::onSelect,
        onCheckedChange = viewModel::onCheckedChange,
        onRemoveClick = viewModel::onRemoveClick
    )
}

@Composable
private fun SecurityFactorSamplesContent(
    modifier: Modifier = Modifier,
    state: SecurityFactorSamplesViewModel.State,
    onBackClick: () -> Unit,
    onSelect: (FactorSourceKindCard) -> Unit,
    onCheckedChange: (FactorSourceInstanceCard, Boolean) -> Unit,
    onRemoveClick: (FactorSourceInstanceCard) -> Unit
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
            items(state.displayOnlyInstanceItems) {
                FactorSourceInstanceCardView(
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
                    onSelect = onSelect
                )
            }

            items(state.multiChoiceItems) {
                SelectableMultiChoiceFactorSourceInstanceCard(
                    item = it,
                    onCheckedChange = onCheckedChange
                )
            }

            items(state.removableItems) {
                RemovableFactorSourceInstanceCard(
                    item = it,
                    onRemoveClick = onRemoveClick
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
                displayOnlyInstanceItems = persistentListOf(
                    FactorSourceInstanceCard(
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
                        hasHiddenEntities = true
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
                multiChoiceItems = persistentListOf(
                    Selectable(
                        data = FactorSourceInstanceCard(
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
                            hasHiddenEntities = false
                        )
                    )
                )
            ),
            onBackClick = {},
            onSelect = {},
            onCheckedChange = { _, _ -> },
            onRemoveClick = {}
        )
    }
}
