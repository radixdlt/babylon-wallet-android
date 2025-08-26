package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin.BiometricsPinViewModel.State
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.composables.FactorSourcesList
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage.SecurityPrompt
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleStokenet
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricsPinScreen(
    viewModel: BiometricsPinViewModel,
    onBackClick: () -> Unit,
    onNavigateToWriteDownSeedPhrase: (factorSourceId: FactorSourceId) -> Unit,
    onNavigateToSeedPhraseRestore: () -> Unit,
    onNavigateToDeviceFactorSourceDetails: (factorSourceId: FactorSourceId) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is BiometricsPinViewModel.Event.NavigateToDeviceFactorSourceDetails -> {
                    onNavigateToDeviceFactorSourceDetails(event.factorSourceId)
                }

                is BiometricsPinViewModel.Event.NavigateToWriteDownSeedPhrase -> {
                    onNavigateToWriteDownSeedPhrase(event.factorSourceId)
                }

                BiometricsPinViewModel.Event.NavigateToSeedPhraseRestore -> {
                    onNavigateToSeedPhraseRestore()
                }
            }
        }
    }

    BiometricsPinContent(
        state = state,
        onBackClick = onBackClick,
        onDeviceFactorSourceClick = viewModel::onDeviceFactorSourceClick,
        onAddBiometricsPinClick = viewModel::onAddBiometricsPinClick
    )
}

@Composable
private fun BiometricsPinContent(
    modifier: Modifier = Modifier,
    state: State,
    onBackClick: () -> Unit,
    onDeviceFactorSourceClick: (FactorSourceId) -> Unit,
    onAddBiometricsPinClick: () -> Unit
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
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.Start
        ) {
            HorizontalDivider(color = RadixTheme.colors.divider)

            FactorSourcesList(
                factorSources = state.factorSources,
                factorSourceDescriptionText = R.string.factorSources_card_deviceDescription,
                addFactorSourceButtonTitle = R.string.factorSources_list_deviceAdd,
                onFactorSourceClick = onDeviceFactorSourceClick,
                onAddFactorSourceClick = onAddBiometricsPinClick
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
                factorSources = persistentListOf(
                    DeviceFactorSource.sample().asGeneral().toFactorSourceCard(
                        messages = persistentListOf(SecurityPrompt.LostFactorSource),
                        accounts = persistentListOf(Account.sampleMainnet()),
                        personas = persistentListOf(
                            Persona.sampleMainnet(),
                            Persona.sampleStokenet()
                        ),
                    ),
                    DeviceFactorSource.sample.other().asGeneral().toFactorSourceCard()
                )
            ),
            onBackClick = {},
            onDeviceFactorSourceClick = {},
            onAddBiometricsPinClick = {}
        )
    }
}
