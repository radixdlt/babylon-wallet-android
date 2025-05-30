package com.babylon.wallet.android.presentation.walletclaimed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.resetwallet.ResetWalletDialog
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.WarningButton

@Composable
fun ClaimedByAnotherDeviceScreen(
    viewModel: ClaimedByAnotherDeviceViewModel,
    modifier: Modifier = Modifier,
    onNavigateToOnboarding: () -> Unit,
    onReclaimedBack: () -> Unit
) {
    BackHandler(enabled = true) { }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                ClaimedByAnotherDeviceViewModel.Event.ResetToOnboarding -> onNavigateToOnboarding()
                ClaimedByAnotherDeviceViewModel.Event.Reclaimed -> onReclaimedBack()
            }
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isReclaiming) {
        FullscreenCircularProgressContent()
    } else {
        Scaffold(
            modifier = modifier.imePadding(),
            containerColor = RadixTheme.colors.background
        ) { padding ->
            ClaimedByAnotherDeviceContent(
                modifier = Modifier
                    .padding(padding)
                    .padding(vertical = RadixTheme.dimensions.paddingLarge)
                    .fillMaxSize(),
                state = state,
                onResetWalletClick = viewModel::onResetWalletClick,
                onResetWalletConfirmClick = viewModel::onResetWalletConfirm,
                onResetWalletDenyClick = viewModel::onResetWalletDeny,
                onTransferControlBackClick = viewModel::onTransferWalletBackClick
            )
        }
    }
}

@Composable
fun ClaimedByAnotherDeviceContent(
    modifier: Modifier = Modifier,
    state: ClaimedByAnotherDeviceViewModel.State,
    onResetWalletClick: () -> Unit,
    onResetWalletConfirmClick: () -> Unit,
    onResetWalletDenyClick: () -> Unit,
    onTransferControlBackClick: () -> Unit
) {
    if (state.isResetWalletConfirmationVisible) {
        ResetWalletDialog(
            onConfirm = onResetWalletConfirmClick,
            onDeny = onResetWalletDenyClick
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.3f))
        Image(
            modifier = Modifier.size(100.dp),
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_wallet_app),
            contentDescription = null
        )
        Spacer(modifier = Modifier.weight(0.2f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
        ) {
            Text(
                text = stringResource(id = R.string.configurationBackup_automated_walletTransferredTitle),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            Text(
                text = stringResource(id = R.string.configurationBackup_automated_walletTransferredSubtitle),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            Text(
                text = stringResource(id = R.string.configurationBackup_automated_walletTransferredExplanation1),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            Text(
                text = stringResource(id = R.string.configurationBackup_automated_walletTransferredExplanation2),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text
            )
        }

        Spacer(Modifier.weight(0.5f))
        WarningButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.factoryReset_resetWallet),
            onClick = onResetWalletClick
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        RadixTextButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.configurationBackup_automated_walletTransferredTransferBackButton),
            onClick = onTransferControlBackClick
        )
    }
}

@Preview
@Composable
fun ClaimedByAnotherDeviceScreenPreviewLight() {
    RadixWalletPreviewTheme {
        ClaimedByAnotherDeviceContent(
            state = ClaimedByAnotherDeviceViewModel.State(),
            onResetWalletClick = {},
            onResetWalletConfirmClick = {},
            onResetWalletDenyClick = {},
            onTransferControlBackClick = {}
        )
    }
}

@Preview
@Composable
fun ClaimedByAnotherDeviceScreenPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        ClaimedByAnotherDeviceContent(
            state = ClaimedByAnotherDeviceViewModel.State(),
            onResetWalletClick = {},
            onResetWalletConfirmClick = {},
            onResetWalletDenyClick = {},
            onTransferControlBackClick = {}
        )
    }
}
