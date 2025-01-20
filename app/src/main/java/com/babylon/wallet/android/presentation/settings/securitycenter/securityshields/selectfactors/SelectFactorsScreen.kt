package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.selectfactors

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.StatusMessageText
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableMultiChoiceFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.card.subtitle
import com.babylon.wallet.android.presentation.ui.composables.card.title
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.StatusMessage
import com.babylon.wallet.android.presentation.ui.modifier.noIndicationClickable
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.SecurityShieldBuilderInvalidReason
import com.radixdlt.sargon.SelectedPrimaryThresholdFactorsStatus
import com.radixdlt.sargon.SelectedPrimaryThresholdFactorsStatusInvalidReason
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
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
        containerColor = RadixTheme.colors.white
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
                Image(
                    painter = painterResource(id = DSR.ic_select_factors),
                    contentDescription = null
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    text = stringResource(id = R.string.shieldSetupSelectFactors_title),
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    text = stringResource(id = R.string.shieldSetupSelectFactors_subtitle)
                        .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1,
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
                    is SelectFactorsViewModel.State.UiItem.CategoryHeader -> CategoryHeaderView(
                        modifier = Modifier.padding(top = RadixTheme.dimensions.paddingXLarge),
                        item = item,
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
        }
    }
}

@Composable
private fun CategoryHeaderView(
    item: SelectFactorsViewModel.State.UiItem.CategoryHeader,
    message: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = item.kind.title(),
            style = RadixTheme.typography.body1Link,
            color = RadixTheme.colors.gray2
        )

        Text(
            text = item.kind.subtitle(),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray2
        )

        message?.let {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))

            Text(
                text = it,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.orange1
            )
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
        is SelectedPrimaryThresholdFactorsStatus.Invalid -> StatusMessageText(
            modifier = modifier.noIndicationClickable { onInfoClick(GlossaryItem.buildingshield) },
            message = StatusMessage(
                message = stringResource(id = R.string.shieldSetupStatus_invalidCombination).formattedSpans(
                    boldStyle = SpanStyle(
                        color = RadixTheme.colors.blue2,
                        fontWeight = RadixTheme.typography.body1StandaloneLink.fontWeight,
                        fontSize = RadixTheme.typography.body2Link.fontSize
                    )
                ),
                type = StatusMessage.Type.ERROR
            )
        )
        SelectedPrimaryThresholdFactorsStatus.Optimal -> return
    }
}

@Composable
@Preview
@UsesSampleValues
private fun SelectFactorsPreview(
    @PreviewParameter(SelectFactorsPreviewProvider::class) state: SelectFactorsViewModel.State
) {
    RadixWalletPreviewTheme {
        SelectFactorsContent(
            state = state,
            onDismiss = {},
            onFactorCheckedChange = { _, _ -> },
            onInfoClick = {},
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
                data = FactorSourceCard.compact(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.DEVICE,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "My Phone",
                    kind = FactorSourceKind.DEVICE,
                ),
                selected = false
            )
        ),
        SelectFactorsViewModel.State.UiItem.CategoryHeader(FactorSourceKind.ARCULUS_CARD),
        SelectFactorsViewModel.State.UiItem.Factor(
            Selectable(
                data = FactorSourceCard.compact(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.ARCULUS_CARD,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "SecretSecret123",
                    kind = FactorSourceKind.ARCULUS_CARD
                ),
                selected = false
            )
        ),
        SelectFactorsViewModel.State.UiItem.CategoryHeader(FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET),
        SelectFactorsViewModel.State.UiItem.Factor(
            Selectable(
                data = FactorSourceCard.compact(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "Highly Secretive Stick",
                    kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET
                ),
                selected = false
            )
        ),
        SelectFactorsViewModel.State.UiItem.Factor(
            Selectable(
                data = FactorSourceCard.compact(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "My Arc",
                    kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET
                ),
                selected = false
            )
        ),
        SelectFactorsViewModel.State.UiItem.CategoryHeader(FactorSourceKind.PASSWORD),
        SelectFactorsViewModel.State.UiItem.Factor(
            Selectable(
                data = FactorSourceCard.compact(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.PASSWORD,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "My Password",
                    kind = FactorSourceKind.PASSWORD
                ),
                selected = true
            )
        ),
        SelectFactorsViewModel.State.UiItem.CategoryHeader(FactorSourceKind.OFF_DEVICE_MNEMONIC),
        SelectFactorsViewModel.State.UiItem.Factor(
            Selectable(
                data = FactorSourceCard.compact(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.OFF_DEVICE_MNEMONIC,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "ShizzleWords",
                    kind = FactorSourceKind.OFF_DEVICE_MNEMONIC
                ),
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
                        underlying = SecurityShieldBuilderInvalidReason.PrimaryRoleMustHaveAtLeastOneFactor()
                    )
                )
            )
        )
}
