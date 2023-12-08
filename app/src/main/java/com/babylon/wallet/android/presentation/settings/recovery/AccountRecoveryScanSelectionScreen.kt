package com.babylon.wallet.android.presentation.settings.recovery

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.MnemonicType
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.utils.formattedSpans

@Composable
fun AccountRecoveryScanSelectionScreen(
    viewModel: AccountRecoveryScanSelectionViewModel,
    onBack: () -> Unit,
    onChooseSeedPhrase: (MnemonicType) -> Unit,
    onChooseLedger: (Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AccountRecoveryScanSelectionContent(
        onBackClick = viewModel::onBackClick, onUseFactorSource = viewModel::onUseFactorSource, isMainnet = state.isMainnet
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                AccountRecoveryScanSelectionViewModel.Event.OnDismiss -> onBack()
                is AccountRecoveryScanSelectionViewModel.Event.ChooseLedger -> onChooseLedger(it.isOlympia)
                is AccountRecoveryScanSelectionViewModel.Event.ChooseSeedPhrase -> onChooseSeedPhrase(it.mnemonicType)
            }
        }
    }
}

@Composable
private fun AccountRecoveryScanSelectionContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onUseFactorSource: (RecoveryType) -> Unit,
    isMainnet: Boolean,
) {
    val backCallback = {
        onBackClick()
    }
    BackHandler {
        backCallback()
    }
    Scaffold(modifier = modifier, topBar = {
        RadixCenteredTopAppBar(
            windowInsets = WindowInsets.statusBars, title = stringResource(id = R.string.empty), onBackClick = {
                backCallback()
            }, backIconType = BackIconType.Close
        )
    }, containerColor = RadixTheme.colors.defaultBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .verticalScroll(state = rememberScrollState())
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = RadixTheme.dimensions.paddingDefault),
                text = "Account Recovery Scan", // TODO crowdin
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "The Radix Wallet can scan for previously used accounts using a bare seed phrase or " + "Ledger hardware wallet device.", // TODO crowdin
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge), color = RadixTheme.colors.gray4)
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Babylon Accounts", // TODO crowdin
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.header,
                color = RadixTheme.colors.gray1
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = RadixTheme.dimensions.paddingDefault
                    ), text = "Scan for Accounts originally created on the *Babylon* network:".formattedSpans(
                    RadixTheme.typography.body1Header.toSpanStyle()
                ), // TODO crowdin
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            RadixSecondaryButton(modifier = Modifier.fillMaxWidth(), text = "Use Seed Phrase", // TODO crowdin
                onClick = {
                    onUseFactorSource(RecoveryType.DeviceBabylon)
                })
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
            RadixSecondaryButton(modifier = Modifier.fillMaxWidth(), text = "Use Ledger Hardware Wallet", // TODO crowdin
                onClick = {
                    onUseFactorSource(RecoveryType.LedgerBabylon)
                })
            if (isMainnet) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge),
                    color = RadixTheme.colors.gray4
                )
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
                        .padding(vertical = RadixTheme.dimensions.paddingDefault),
                    text = "Scan for Accounts originally created on the previous *Olympia* network.\n\n" +
                            "(If you have Olympia Accounts in the Radix Olympia Desktop Wallet, consider using *Import from a Legacy Wallet* instead.)".formattedSpans(
                                RadixTheme.typography.body1Header.toSpanStyle()
                            ), // TODO crowdin
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )
                RadixSecondaryButton(modifier = Modifier.fillMaxWidth(), text = "Use Seed Phrase", // TODO crowdin
                    onClick = {
                        onUseFactorSource(RecoveryType.DeviceOlympia)
                    })
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                RadixSecondaryButton(modifier = Modifier.fillMaxWidth(), text = "Use Ledger Hardware Wallet", // TODO crowdin
                    onClick = {
                        onUseFactorSource(RecoveryType.LedgerOlympia)
                    })
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = RadixTheme.dimensions.paddingSmall, horizontal = RadixTheme.dimensions.paddingDefault),
                    text = "Note: You must still use the new *Radix Babylon* app on your Ledger device, not the old Radix Ledger app.".formattedSpans(
                        RadixTheme.typography.body1HighImportance.toSpanStyle()
                    ), // TODO crowdin
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1
                )
            }
        }
    }
}

@Preview
@Composable
fun AccountRecoveryScanSelectionPreview() {
    RadixWalletTheme {
        AccountRecoveryScanSelectionContent(onBackClick = {}, onUseFactorSource = {}, isMainnet = true)
    }
}
