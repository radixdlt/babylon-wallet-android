package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.ledgerdevice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
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
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.composables.FactorSourcesList
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleStokenet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun LedgerDevicesScreen(
    modifier: Modifier = Modifier,
    viewModel: LedgerDevicesViewModel,
    toFactorSourceDetails: (factorSourceId: FactorSourceId) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is LedgerDevicesViewModel.Event.NavigateToLedgerFactorSourceDetails -> {
                    toFactorSourceDetails(event.factorSourceId)
                }
            }
        }
    }

    LedgerDevicesContent(
        modifier = modifier,
        ledgerFactorSources = state.ledgerFactorSources,
        onLedgerFactorSourceClick = viewModel::onLedgerFactorSourceClick,
        onAddLedgerDeviceClick = viewModel::onAddLedgerDeviceClick,
        onBackClick = onBackClick
    )
}

@Composable
private fun LedgerDevicesContent(
    modifier: Modifier = Modifier,
    ledgerFactorSources: PersistentList<FactorSourceCard>,
    onLedgerFactorSourceClick: (FactorSourceId) -> Unit,
    onAddLedgerDeviceClick: () -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.factorSources_card_ledgerTitle),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(color = RadixTheme.colors.divider)

            FactorSourcesList(
                factorSources = ledgerFactorSources,
                factorSourceDescriptionText = R.string.factorSources_card_ledgerDescription,
                addFactorSourceButtonContent = {
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(align = Alignment.CenterHorizontally),
                        text = stringResource(id = R.string.factorSources_list_ledgerAdd),
                        onClick = onAddLedgerDeviceClick,
                        throttleClicks = true
                    )
                },
                onFactorSourceClick = onLedgerFactorSourceClick
            )
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun LedgerDevicesScreenPreview() {
    RadixWalletTheme {
        LedgerDevicesContent(
            ledgerFactorSources = persistentListOf(
                LedgerHardwareWalletFactorSource.sample().asGeneral().toFactorSourceCard(
                    personas = persistentListOf(
                        Persona.sampleMainnet(),
                        Persona.sampleStokenet()
                    ),
                    includeLastUsedOn = true,
                    hasHiddenEntities = true
                ),
                LedgerHardwareWalletFactorSource.sample.other().asGeneral().toFactorSourceCard(
                    includeLastUsedOn = true,
                    hasHiddenEntities = false
                ),
                FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "Gate13",
                    includeDescription = false,
                    lastUsedOn = "Last year",
                    kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    messages = persistentListOf(),
                    accounts = persistentListOf(Account.sampleMainnet()),
                    personas = persistentListOf(),
                    hasHiddenEntities = false,
                    supportsBabylon = true,
                    isEnabled = true
                )
            ),
            onLedgerFactorSourceClick = {},
            onAddLedgerDeviceClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LedgerDevicesEmptyScreenPreview() {
    RadixWalletTheme {
        LedgerDevicesContent(
            ledgerFactorSources = persistentListOf(),
            onLedgerFactorSourceClick = {},
            onAddLedgerDeviceClick = {},
            onBackClick = {}
        )
    }
}
