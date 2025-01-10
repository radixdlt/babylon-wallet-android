package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin

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
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin.BiometricsPinViewModel.State
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.composables.FactorSourcesList
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
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
fun BiometricsPinScreen(
    viewModel: BiometricsPinViewModel,
    onBackClick: () -> Unit,
    onNavigateToDeviceFactorSourceDetails: (factorSourceId: FactorSourceId) -> Unit,
    onNavigateToAddBiometricPin: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is BiometricsPinViewModel.Event.NavigateToDeviceFactorSourceDetails -> {
                    onNavigateToDeviceFactorSourceDetails(event.factorSourceId)
                }
            }
        }
    }

    BiometricsPinContent(
        state = state,
        onBackClick = onBackClick,
        onDeviceFactorSourceClick = viewModel::onDeviceFactorSourceClick,
        onAddBiometricsPinClick = onNavigateToAddBiometricPin,
        onInfoClick = onInfoClick
    )
}

@Composable
private fun BiometricsPinContent(
    modifier: Modifier = Modifier,
    state: State,
    onBackClick: () -> Unit,
    onDeviceFactorSourceClick: (FactorSourceId) -> Unit,
    onAddBiometricsPinClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.factorSources_card_deviceTitle),
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
                mainFactorSource = state.mainDeviceFactorSource,
                factorSources = state.deviceFactorSources,
                factorSourceDescriptionText = R.string.factorSources_card_deviceDescription,
                addFactorSourceButtonTitle = R.string.factorSources_list_deviceAdd,
                glossaryItem = GlossaryItem.biometricsPIN,
                onFactorSourceClick = onDeviceFactorSourceClick,
                onAddFactorSourceClick = onAddBiometricsPinClick,
                onInfoClick = onInfoClick
            )
        }
    }
}

@Composable
@Preview
@UsesSampleValues
private fun BiometricsPinPreview() {
    RadixWalletPreviewTheme {
        BiometricsPinContent(
            state = State(
                mainDeviceFactorSource = FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.DEVICE,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "My cool phone",
                    includeDescription = false,
                    lastUsedOn = "Today",
                    kind = FactorSourceKind.DEVICE,
                    messages = persistentListOf(FactorSourceStatusMessage.NoSecurityIssues),
                    accounts = persistentListOf(
                        Account.sampleMainnet()
                    ),
                    personas = persistentListOf(
                        Persona.sampleMainnet(),
                        Persona.sampleStokenet()
                    ),
                    hasHiddenEntities = false
                ),
                deviceFactorSources = persistentListOf(
                    FactorSourceCard(
                        id = FactorSourceId.Hash.init(
                            kind = FactorSourceKind.DEVICE,
                            mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                        ),
                        name = "My Phone",
                        includeDescription = false,
                        lastUsedOn = "Today",
                        kind = FactorSourceKind.DEVICE,
                        messages = persistentListOf(FactorSourceStatusMessage.SecurityPrompt.RecoveryRequired),
                        accounts = persistentListOf(
                            Account.sampleMainnet()
                        ),
                        personas = persistentListOf(
                            Persona.sampleMainnet(),
                            Persona.sampleStokenet()
                        ),
                        hasHiddenEntities = true
                    )
                )
            ),
            onBackClick = {},
            onDeviceFactorSourceClick = {},
            onAddBiometricsPinClick = {},
            onInfoClick = {}
        )
    }
}
