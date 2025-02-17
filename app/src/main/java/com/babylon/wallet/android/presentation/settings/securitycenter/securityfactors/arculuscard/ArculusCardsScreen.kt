package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.composables.FactorSourcesList
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ArculusCardsScreen(
    viewModel: ArculusCardsViewModel,
    onNavigateToArculusFactorSourceDetails: (factorSourceId: FactorSourceId) -> Unit,
    onNavigateToAddArculusCard: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is ArculusCardsViewModel.Event.NavigateToArculusFactorSourceDetails -> {
                    onNavigateToArculusFactorSourceDetails(event.factorSourceId)
                }
            }
        }
    }

    ArculusCardsContent(
        arculusFactorSources = state.arculusFactorSources,
        onArculusFactorSourceClick = viewModel::onArculusFactorSourceClick,
        onAddArculusCardClick = onNavigateToAddArculusCard,
        onInfoClick = onInfoClick,
        onBackClick = onBackClick
    )
}

@Composable
private fun ArculusCardsContent(
    modifier: Modifier = Modifier,
    arculusFactorSources: PersistentList<FactorSourceCard>,
    onArculusFactorSourceClick: (FactorSourceId) -> Unit,
    onAddArculusCardClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.factorSources_card_arculusCardTitle),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.Start
        ) {
            HorizontalDivider(color = RadixTheme.colors.gray4)

            FactorSourcesList(
                mainFactorSource = null,
                factorSources = arculusFactorSources,
                factorSourceDescriptionText = R.string.factorSources_card_arculusCardDescription,
                addFactorSourceButtonTitle = R.string.factorSources_list_arculusCardAdd,
                glossaryItem = GlossaryItem.arculus,
                onFactorSourceClick = onArculusFactorSourceClick,
                onAddFactorSourceClick = onAddArculusCardClick,
                onInfoClick = onInfoClick
            )
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun ArculusCardsScreenPreview() {
    RadixWalletTheme {
        ArculusCardsContent(
            arculusFactorSources = persistentListOf(
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.ARCULUS_CARD,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "Panathinaikos",
                    includeDescription = false,
                    lastUsedOn = "Today",
                    kind = FactorSourceKind.ARCULUS_CARD,
                    messages = persistentListOf(),
                    accounts = persistentListOf(Account.sampleMainnet()),
                    personas = persistentListOf(),
                    hasHiddenEntities = false,
                    isEnabled = true
                ),
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.ARCULUS_CARD,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "Gate 13",
                    includeDescription = false,
                    lastUsedOn = "Last year",
                    kind = FactorSourceKind.ARCULUS_CARD,
                    messages = persistentListOf(),
                    accounts = persistentListOf(),
                    personas = persistentListOf(),
                    hasHiddenEntities = false,
                    isEnabled = true
                ),
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.ARCULUS_CARD,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "Gate13",
                    includeDescription = false,
                    lastUsedOn = "Last year",
                    kind = FactorSourceKind.ARCULUS_CARD,
                    messages = persistentListOf(),
                    accounts = persistentListOf(Account.sampleMainnet()),
                    personas = persistentListOf(),
                    hasHiddenEntities = false,
                    isEnabled = true
                )
            ),
            onArculusFactorSourceClick = {},
            onAddArculusCardClick = {},
            onBackClick = {},
            onInfoClick = {}
        )
    }
}
