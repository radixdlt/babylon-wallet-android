package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.recovery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.AddFactorButton
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.FactorsContainerView
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.ShieldBuilderTitleView
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.ShieldSetupStatusView
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.RemovableFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SetupRecoveryScreen(
    viewModel: SetupRecoveryViewModel,
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SetupRecoveryContent(
        state = state,
        onDismiss = onDismiss,
        onInfoClick = onInfoClick,
        onAddStartRecoveryFactorClick = viewModel::onAddStartRecoveryFactorClick,
        onRemoveStartRecoveryFactor = viewModel::onRemoveStartRecoveryFactor,
        onAddConfirmRecoveryFactorClick = viewModel::onAddConfirmRecoveryFactorClick,
        onRemoveConfirmRecoveryFactor = viewModel::onRemoveConfirmRecoveryFactor,
        onContinueClick = viewModel::onContinueClick
    )
}

@Composable
private fun SetupRecoveryContent(
    modifier: Modifier = Modifier,
    state: SetupRecoveryViewModel.State,
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onAddStartRecoveryFactorClick: () -> Unit,
    onRemoveStartRecoveryFactor: (FactorSourceCard) -> Unit,
    onAddConfirmRecoveryFactorClick: () -> Unit,
    onRemoveConfirmRecoveryFactor: (FactorSourceCard) -> Unit,
    onContinueClick: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = "Step 2 of 2", // TODO crowdin
                onBackClick = onDismiss,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                text = stringResource(R.string.common_continue),
                enabled = state.isButtonEnabled
            )
        },
        containerColor = RadixTheme.colors.white
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge)
            ) {
                ShieldBuilderTitleView(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    imageRes = DSR.ic_shield_recovery,
                    title = "Recovery", // TODO crowdin
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                SectionHeaderView(
                    title = "Starting a Recovery", // TODO crowdin
                    subtitle = "Factors you can use to start recovering, and temporarily lock, Accounts and Personas if you lose access.", // TODO crowdin
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

                state.status?.let {
                    ShieldSetupStatusView(
                        modifier = Modifier.padding(
                            start = RadixTheme.dimensions.paddingSemiLarge,
                            end = RadixTheme.dimensions.paddingSemiLarge,
                            top = RadixTheme.dimensions.paddingSemiLarge,
                            bottom = RadixTheme.dimensions.paddingXXLarge
                        ),
                        status = it,
                        onInfoClick = onInfoClick
                    )
                }

                FactorsView(
                    factors = state.startFactors,
                    onAddFactorClick = onAddStartRecoveryFactorClick,
                    onRemoveFactorClick = onRemoveStartRecoveryFactor
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

                SectionHeaderView(
                    title = "Confirming a Recovery", // TODO crowdin
                    subtitle = "Factors you can use to complete the recovery of your Accounts and Personas.", // TODO crowdin
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

                FactorsView(
                    factors = state.confirmFactors,
                    onAddFactorClick = onAddConfirmRecoveryFactorClick,
                    onRemoveFactorClick = onRemoveConfirmRecoveryFactor
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
            }
        }
    }
}

@Composable
private fun SectionHeaderView(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = RadixTheme.typography.header,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            text = subtitle,
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1
        )
    }
}

@Composable
private fun FactorsView(
    modifier: Modifier = Modifier,
    factors: PersistentList<FactorSourceCard>,
    onAddFactorClick: () -> Unit,
    onRemoveFactorClick: (FactorSourceCard) -> Unit
) {
    FactorsContainerView(
        modifier = modifier
    ) {
        Text(
            text = "You'll need to use the following:",
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

        if (factors.isNotEmpty()) {
            factors.forEach { factor ->
                RemovableFactorSourceCard(
                    item = factor,
                    onRemoveClick = onRemoveFactorClick
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            }
        }

        AddFactorButton(
            onClick = onAddFactorClick
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun SetupRecoveryPreview(
    @PreviewParameter(SetupRecoveryPreviewProvider::class) state: SetupRecoveryViewModel.State
) {
    RadixWalletPreviewTheme {
        SetupRecoveryContent(
            state = state,
            onDismiss = {},
            onInfoClick = {},
            onAddStartRecoveryFactorClick = {},
            onRemoveStartRecoveryFactor = {},
            onAddConfirmRecoveryFactorClick = {},
            onRemoveConfirmRecoveryFactor = {},
            onContinueClick = {}
        )
    }
}

@UsesSampleValues
class SetupRecoveryPreviewProvider : PreviewParameterProvider<SetupRecoveryViewModel.State> {

    override val values: Sequence<SetupRecoveryViewModel.State>
        get() = sequenceOf(
            SetupRecoveryViewModel.State(
                startFactors = persistentListOf(
                    FactorSourceCard(
                        id = FactorSourceId.Hash.init(
                            kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                            mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                        ),
                        name = "Ledger ABC",
                        includeDescription = true,
                        lastUsedOn = null,
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        messages = persistentListOf(),
                        accounts = persistentListOf(),
                        personas = persistentListOf(),
                        hasHiddenEntities = false
                    )
                ),
                confirmFactors = persistentListOf(
                    FactorSourceCard(
                        id = FactorSourceId.Hash.init(
                            kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                            mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                        ),
                        name = "Metal Jacket",
                        includeDescription = true,
                        lastUsedOn = null,
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        messages = persistentListOf(),
                        accounts = persistentListOf(),
                        personas = persistentListOf(),
                        hasHiddenEntities = false
                    )
                )
            )
        )
}
