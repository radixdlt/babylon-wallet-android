package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.selectfactors

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.ShieldSetupUnsafeCombinationStatusView
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.StatusMessageText
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableMultiChoiceFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.FactorSourceCategoryHeaderView
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.shared.StatusMessage
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.ArculusCardFactorSource
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.OffDeviceMnemonicFactorSource
import com.radixdlt.sargon.PasswordFactorSource
import com.radixdlt.sargon.SecurityShieldBuilderRuleViolation
import com.radixdlt.sargon.SelectedPrimaryThresholdFactorsStatus
import com.radixdlt.sargon.SelectedPrimaryThresholdFactorsStatusInvalidReason
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.samples.sample

@Composable
fun SelectFactorsScreen(
    modifier: Modifier = Modifier,
    viewModel: SelectFactorsViewModel,
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    toRegularAccess: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SelectFactorsContent(
        modifier = modifier,
        state = state,
        onDismiss = onDismiss,
        onFactorCheckedChange = viewModel::onFactorCheckedChange,
        onInfoClick = onInfoClick,
        onSkipClick = viewModel::onSkipClick,
        onBuildShieldClick = viewModel::onBuildShieldClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                SelectFactorsViewModel.Event.ToRegularAccess -> toRegularAccess()
            }
        }
    }
}

@Composable
private fun SelectFactorsContent(
    modifier: Modifier = Modifier,
    state: SelectFactorsViewModel.State,
    onDismiss: () -> Unit,
    onFactorCheckedChange: (FactorSourceCard, Boolean) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onSkipClick: () -> Unit,
    onBuildShieldClick: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onDismiss,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onBuildShieldClick,
                text = stringResource(R.string.shieldSetupSelectFactors_buildButtonTitle),
                enabled = state.isButtonEnabled
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = RadixTheme.dimensions.paddingDefault,
                end = RadixTheme.dimensions.paddingDefault,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + RadixTheme.dimensions.paddingSemiLarge
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Icon(
                    painter = painterResource(id = DSR.ic_select_factors),
                    contentDescription = null,
                    tint = RadixTheme.colors.icon
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    text = stringResource(id = R.string.shieldSetupSelectFactors_title),
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    text = stringResource(id = R.string.shieldSetupSelectFactors_subtitle)
                        .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

                state.status?.let {
                    StatusView(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        status = it,
                        onInfoClick = onInfoClick
                    )
                }
            }

            items(state.items) {
                when (val item = it) {
                    is SelectFactorsViewModel.State.UiItem.CategoryHeader -> FactorSourceCategoryHeaderView(
                        modifier = Modifier.padding(top = RadixTheme.dimensions.paddingXLarge),
                        kind = item.kind,
                        message = stringResource(id = R.string.shieldSetupStatus_factorCannotBeUsedByItself)
                            .takeIf { state.cannotBeUsedByItself(item) }
                    )

                    is SelectFactorsViewModel.State.UiItem.Factor -> SelectableMultiChoiceFactorSourceCard(
                        modifier = Modifier.padding(top = RadixTheme.dimensions.paddingMedium),
                        item = item.card,
                        onCheckedChange = onFactorCheckedChange
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                RadixTextButton(
                    text = stringResource(id = R.string.shieldSetupSelectFactors_skipButtonTitle),
                    onClick = onSkipClick,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatusView(
    modifier: Modifier = Modifier,
    status: SelectedPrimaryThresholdFactorsStatus,
    onInfoClick: (GlossaryItem) -> Unit
) {
    when (status) {
        SelectedPrimaryThresholdFactorsStatus.Suboptimal -> StatusMessageText(
            modifier = modifier,
            message = StatusMessage(
                message = stringResource(id = R.string.shieldSetupStatus_recommendedFactors),
                type = StatusMessage.Type.WARNING
            )
        )

        SelectedPrimaryThresholdFactorsStatus.Insufficient -> StatusMessageText(
            modifier = modifier,
            message = StatusMessage(
                message = stringResource(id = R.string.shieldSetupStatus_selectFactors_atLeastOneFactor),
                type = StatusMessage.Type.ERROR
            )
        )

        is SelectedPrimaryThresholdFactorsStatus.Invalid -> ShieldSetupUnsafeCombinationStatusView(
            modifier = modifier,
            onInfoClick = onInfoClick
        )

        SelectedPrimaryThresholdFactorsStatus.Optimal -> return
    }
}

@Composable
@Preview
@UsesSampleValues
private fun SelectFactorsLightPreview(
    @PreviewParameter(SelectFactorsPreviewProvider::class) state: SelectFactorsViewModel.State
) {
    RadixWalletPreviewTheme {
        SelectFactorsContent(
            state = state,
            onDismiss = {},
            onFactorCheckedChange = { _, _ -> },
            onInfoClick = {},
            onSkipClick = {},
            onBuildShieldClick = {},
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun SelectFactorsDarkPreview(
    @PreviewParameter(SelectFactorsPreviewProvider::class) state: SelectFactorsViewModel.State
) {
    RadixWalletPreviewTheme(
        enableDarkTheme = true
    ) {
        SelectFactorsContent(
            state = state,
            onDismiss = {},
            onFactorCheckedChange = { _, _ -> },
            onInfoClick = {},
            onSkipClick = {},
            onBuildShieldClick = {},
        )
    }
}

@UsesSampleValues
class SelectFactorsPreviewProvider : PreviewParameterProvider<SelectFactorsViewModel.State> {

    val items = listOf(
        SelectFactorsViewModel.State.UiItem.CategoryHeader(FactorSourceKind.DEVICE),
        SelectFactorsViewModel.State.UiItem.Factor(
            Selectable(
                data = DeviceFactorSource.sample().asGeneral().toFactorSourceCard(),
                selected = false
            )
        ),
        SelectFactorsViewModel.State.UiItem.CategoryHeader(FactorSourceKind.ARCULUS_CARD),
        SelectFactorsViewModel.State.UiItem.Factor(
            Selectable(
                data = ArculusCardFactorSource.sample().asGeneral().toFactorSourceCard(),
                selected = false
            )
        ),
        SelectFactorsViewModel.State.UiItem.CategoryHeader(FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET),
        SelectFactorsViewModel.State.UiItem.Factor(
            Selectable(
                data = LedgerHardwareWalletFactorSource.sample().asGeneral().toFactorSourceCard(),
                selected = false
            )
        ),
        SelectFactorsViewModel.State.UiItem.Factor(
            Selectable(
                data = LedgerHardwareWalletFactorSource.sample.other().asGeneral().toFactorSourceCard(),
                selected = false
            )
        ),
        SelectFactorsViewModel.State.UiItem.CategoryHeader(FactorSourceKind.PASSWORD),
        SelectFactorsViewModel.State.UiItem.Factor(
            Selectable(
                data = PasswordFactorSource.sample().asGeneral().toFactorSourceCard(),
                selected = true
            )
        ),
        SelectFactorsViewModel.State.UiItem.CategoryHeader(FactorSourceKind.OFF_DEVICE_MNEMONIC),
        SelectFactorsViewModel.State.UiItem.Factor(
            Selectable(
                data = OffDeviceMnemonicFactorSource.sample().asGeneral().toFactorSourceCard(),
                selected = false
            )
        )
    )

    override val values: Sequence<SelectFactorsViewModel.State>
        get() = sequenceOf(
            SelectFactorsViewModel.State(
                items = items,
                status = SelectedPrimaryThresholdFactorsStatus.Invalid(
                    reason = SelectedPrimaryThresholdFactorsStatusInvalidReason.Other(
                        underlying = SecurityShieldBuilderRuleViolation.PrimaryRoleMustHaveAtLeastOneFactor()
                    )
                )
            )
        )
}
