package com.babylon.wallet.android.presentation.settings.recovery.chooseseed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DeviceFactorSourceData
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.MnemonicType
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import rdx.works.profile.data.model.factorsources.FactorSource

@Composable
fun ChooseSeedPhraseScreen(
    viewModel: ChooseSeedPhraseViewModel,
    onBack: () -> Unit,
    onAddSeedPhrase: (MnemonicType) -> Unit,
    onRecoveryScanWithFactorSource: (FactorSource, Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    ChooseSeedPhraseContent(
        state = state,
        onBackClick = onBack,
        onUseFactorSource = viewModel::onUseFactorSource,
        onAddSeedPhrase = onAddSeedPhrase,
        onSelectionChanged = viewModel::onSelectionChanged,
        recoveryType = state.mnemonicType
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is ChooseSeedPhraseViewModel.Event.UseFactorSource -> {
                    onRecoveryScanWithFactorSource(event.factorSource, event.isOlympia)
                }
            }
        }
    }
}

@Composable
private fun ChooseSeedPhraseContent(
    modifier: Modifier = Modifier,
    state: ChooseSeedPhraseViewModel.State,
    onBackClick: () -> Unit,
    onUseFactorSource: () -> Unit,
    onAddSeedPhrase: (MnemonicType) -> Unit,
    onSelectionChanged: (FactorSource.FactorSourceID.FromHash) -> Unit,
    recoveryType: MnemonicType
) {
    val backCallback = {
        onBackClick()
    }
    BackHandler {
        backCallback()
    }
    Scaffold(modifier = modifier, topBar = {
        RadixCenteredTopAppBar(
            windowInsets = WindowInsets.statusBars,
            title = stringResource(id = R.string.empty),
            onBackClick = {
                backCallback()
            },
            backIconType = BackIconType.Close
        )
    }, containerColor = RadixTheme.colors.defaultBackground, bottomBar = {
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault).navigationBarsPadding(),
            text = stringResource(id = R.string.common_continue),
            onClick = onUseFactorSource
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXLarge, vertical = RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.accountRecoveryScan_chooseSeedPhrase_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXLarge, vertical = RadixTheme.dimensions.paddingDefault),
                text = when (recoveryType) {
                    MnemonicType.Olympia -> stringResource(id = R.string.accountRecoveryScan_chooseSeedPhrase_subtitleOlympia)
                    else -> stringResource(id = R.string.accountRecoveryScan_chooseSeedPhrase_subtitleBabylon)
                },
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
            ) {
                items(state.factorSources) { factorSource ->
                    SeedPhraseCard(
                        modifier = Modifier
                            .shadow(elevation = 4.dp, shape = RadixTheme.shapes.roundedRectMedium)
                            .fillMaxWidth()
                            .background(RadixTheme.colors.gray5, RadixTheme.shapes.roundedRectMedium)
                            .padding(RadixTheme.dimensions.paddingDefault),
                        data = factorSource.data,
                        selected = factorSource.selected,
                        onSelectionChanged = {
                            onSelectionChanged(factorSource.data.deviceFactorSource.id)
                        }
                    )
                }
                item {
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = when (recoveryType) {
                            MnemonicType.BabylonMain,
                            MnemonicType.Babylon -> stringResource(id = R.string.accountRecoveryScan_chooseSeedPhrase_addButtonBabylon)
                            MnemonicType.Olympia -> stringResource(id = R.string.accountRecoveryScan_chooseSeedPhrase_addButtonOlympia)
                        },
                        onClick = {
                            onAddSeedPhrase(recoveryType)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SeedPhraseCard(
    modifier: Modifier,
    data: DeviceFactorSourceData,
    selected: Boolean,
    onSelectionChanged: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Icon(
                painter = painterResource(id = DSR.ic_seed_phrases),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(
                    text = "Seed phrase",
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                androidx.compose.material3.Text(
                    text = stringResource(
                        id = if (data.accounts.size == 1) {
                            R.string.displayMnemonics_connectedAccountsLabel_one
                        } else {
                            R.string.displayMnemonics_connectedAccountsLabel_many
                        },
                        data.accounts.size
                    ),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            RadioButton(
                selected = selected,
                colors = RadioButtonDefaults.colors(
                    selectedColor = RadixTheme.colors.gray1,
                    unselectedColor = RadixTheme.colors.gray3,
                    disabledSelectedColor = Color.White
                ),
                onClick = onSelectionChanged,
            )
        }
        if (data.accounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))
            data.accounts.forEach { account ->
                SimpleAccountCard(
                    modifier = Modifier.fillMaxWidth(),
                    account = account
                )
            }
        }
    }
}

@Preview
@Composable
fun RestoreWithoutBackupPreview() {
    RadixWalletTheme {
        ChooseSeedPhraseContent(
            state = ChooseSeedPhraseViewModel.State(),
            onBackClick = {},
            onUseFactorSource = {},
            onAddSeedPhrase = {},
            onSelectionChanged = {},
            recoveryType = MnemonicType.Olympia
        )
    }
}
