package com.babylon.wallet.android.presentation.settings.recovery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.MnemonicType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.utils.formattedSpans

@Composable
fun DeriveLegacyAccountScreen(
    viewModel: DeriveLegacyAccountViewModel,
    onBack: () -> Unit,
    onChooseSeedPhrase: (MnemonicType) -> Unit,
    onChooseLedger: (Boolean) -> Unit,
) {
    DeriveLegacyAccountContent(
        onBackClick = viewModel::onBackClick,
        onUseFactorSource = viewModel::onUseFactorSource
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                DeriveLegacyAccountViewModel.Event.OnDismiss -> onBack()
                is DeriveLegacyAccountViewModel.Event.ChooseLedger -> onChooseLedger(it.isOlympia)
                is DeriveLegacyAccountViewModel.Event.ChooseSeedPhrase -> onChooseSeedPhrase(it.mnemonicType)
            }
        }
    }
}

@Composable
private fun DeriveLegacyAccountContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onUseFactorSource: (RecoveryType) -> Unit,
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
            title = "Derive Legacy Account", // TODO crowdin
            onBackClick = {
                backCallback()
            }
        )
    }, containerColor = RadixTheme.colors.gray5) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(state = rememberScrollState())
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                text = "The Radix Wallet can scan for previously used accounts using a bare seed phrase or " +
                    "Ledger hardware wallet device.\n" +
                    "\n" +
                    "(If you have Olympia Accounts in the Radix Olympia Desktop Wallet, consider using Import from a" +
                    " Legacy Wallet instead.)", // TODO crowdin
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = "Babylon Accounts", // TODO crowdin
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.header,
                color = RadixTheme.colors.gray1
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingSmall
                    ),
                text = "Scan for Accounts originally created on the *Babylon* network:".formattedSpans(
                    RadixTheme.typography.body1Header.toSpanStyle()
                ), // TODO crowdin
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
            Column(
                modifier = Modifier
                    .background(RadixTheme.colors.defaultBackground)
                    .padding(RadixTheme.dimensions.paddingDefault)
            ) {
                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Use Seed Phrase", // TODO crowdin
                    onClick = {
                        onUseFactorSource(RecoveryType.DeviceBabylon)
                    }
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Use Ledger Hardware Wallet", // TODO crowdin
                    onClick = {
                        onUseFactorSource(RecoveryType.LedgerBabylon)
                    }
                )
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = "Olympia Accounts", // TODO crowdin
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.header,
                color = RadixTheme.colors.gray1
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingSmall
                    ),
                text = "Scan for Accounts originally created on the *Olympia* network:".formattedSpans(
                    RadixTheme.typography.body1Header.toSpanStyle()
                ), // TODO crowdin
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
            Column(
                modifier = Modifier
                    .background(RadixTheme.colors.defaultBackground)
                    .padding(RadixTheme.dimensions.paddingDefault)
            ) {
                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Use Seed Phrase", // TODO crowdin
                    onClick = {
                        onUseFactorSource(RecoveryType.DeviceOlympia)
                    }
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Use Ledger Hardware Wallet", // TODO crowdin
                    onClick = {
                        onUseFactorSource(RecoveryType.LedgerOlympia)
                    }
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = RadixTheme.dimensions.paddingSmall),
                    text = "Note: You will still use the new *Radix Babylon* app on your Ledger device.".formattedSpans(
                        RadixTheme.typography.body1HighImportance.toSpanStyle()
                    ), // TODO crowdin
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray2
                )
            }
        }
    }
}

@Preview
@Composable
fun RestoreWithoutBackupPreview() {
    RadixWalletTheme {
        DeriveLegacyAccountContent(
            onBackClick = {},
            onUseFactorSource = {}
        )
    }
}
