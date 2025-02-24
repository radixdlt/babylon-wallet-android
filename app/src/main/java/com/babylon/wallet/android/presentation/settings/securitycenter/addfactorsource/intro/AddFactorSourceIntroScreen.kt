package com.babylon.wallet.android.presentation.settings.securitycenter.addfactorsource.intro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.securitycenter.common.utils.infoButtonTitle
import com.babylon.wallet.android.presentation.settings.securitycenter.common.utils.infoGlossaryItem
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.iconRes
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.FactorSourceKind

@Composable
fun AddFactorSourceIntroScreen(
    modifier: Modifier = Modifier,
    viewModel: AddFactorSourceIntroViewModel,
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onContinueClick: (FactorSourceKind) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AddFactorSourceIntroContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onInfoClick = onInfoClick,
        onContinueClick = { onContinueClick(state.factorSourceKind) }
    )
}

@Composable
private fun AddFactorSourceIntroContent(
    modifier: Modifier = Modifier,
    state: AddFactorSourceIntroViewModel.State,
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onContinueClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = { onDismiss() },
                backIconType = BackIconType.Close,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.defaultBackground,
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                text = stringResource(id = R.string.common_continue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                modifier = Modifier.size(80.dp),
                painter = painterResource(id = state.factorSourceKind.iconRes()),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

            Text(
                text = state.factorSourceKind.addTitle(),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Text(
                text = state.factorSourceKind.addSubtitle(),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            InfoButton(
                text = state.factorSourceKind.infoButtonTitle(),
                onClick = { onInfoClick(state.factorSourceKind.infoGlossaryItem()) }
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
        }
    }
}

@Composable
private fun FactorSourceKind.addTitle() = when (this) {
    FactorSourceKind.DEVICE -> "Add a New Biometrics/PIN Seed Phrase"
    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> "Add a New Ledger Nano"
    FactorSourceKind.OFF_DEVICE_MNEMONIC -> "Add a New Mnemonic Seed Phrase"
    FactorSourceKind.ARCULUS_CARD -> "Add a New Arculus Card"
    FactorSourceKind.PASSWORD -> "Add a New Password"
}

@Suppress("MaxLineLength")
@Composable
private fun FactorSourceKind.addSubtitle() = when (this) {
    FactorSourceKind.DEVICE -> "This factor is a seed phrase held by your phone and unlocked by your biometrics/PIN."
    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> "Ledger Nanos are hardware signing devices you can connect to your Radix Wallet with a USB cable and computer."
    FactorSourceKind.OFF_DEVICE_MNEMONIC -> "Mnemonics are 12 to 24-word BIP39 seed phrases that you’ll need to enter in full every time you use this factor."
    FactorSourceKind.ARCULUS_CARD -> "Arculus Cards are hardware signing devices you tap to your phone to sign a transaction."
    FactorSourceKind.PASSWORD -> "Passwords on Radix are decentralized and aren’t known or stored by anyone but you."
}

@Composable
@Preview
private fun AddFactorSourceIntroPreview(
    @PreviewParameter(AddFactoSourcerIntroPreviewProvider::class) state: AddFactorSourceIntroViewModel.State
) {
    RadixWalletPreviewTheme {
        AddFactorSourceIntroContent(
            state = state,
            onDismiss = {},
            onInfoClick = {},
            onContinueClick = {}
        )
    }
}

class AddFactoSourcerIntroPreviewProvider : PreviewParameterProvider<AddFactorSourceIntroViewModel.State> {

    override val values: Sequence<AddFactorSourceIntroViewModel.State>
        get() = FactorSourceKind.entries.map { kind ->
            AddFactorSourceIntroViewModel.State(
                factorSourceKind = kind
            )
        }.asSequence()
}
