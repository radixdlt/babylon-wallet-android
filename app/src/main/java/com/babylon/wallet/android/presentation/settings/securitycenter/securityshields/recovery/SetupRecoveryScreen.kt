package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.AddFactorButton
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.FactorsContainerView
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.ShieldBuilderTitleView
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.ShieldSetupStatusView
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.choosefactor.ChooseFactorSourceBottomSheet
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.ListItemPicker
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.RemovableFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun SetupRecoveryScreen(
    viewModel: SetupRecoveryViewModel,
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onShieldCreated: () -> Unit
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
        onFallbackPeriodClick = viewModel::onFallbackPeriodClick,
        onFallbackPeriodValueChange = viewModel::onFallbackPeriodValueChange,
        onFallbackPeriodUnitChange = viewModel::onFallbackPeriodUnitChange,
        onSetFallbackPeriodClick = viewModel::onSetFallbackPeriodClick,
        onDismissFallbackPeriod = viewModel::onDismissFallbackPeriod,
        onShieldNameChange = viewModel::onShieldNameChange,
        onDismissSetShieldName = viewModel::onDismissSetShieldName,
        onConfirmShieldNameClick = viewModel::onConfirmShieldNameClick,
        onContinueClick = viewModel::onContinueClick
    )

    state.selectFactor?.let { selectFactor ->
        ChooseFactorSourceBottomSheet(
            viewModel = hiltViewModel(),
            excludeFactorSources = selectFactor.excludeFactorSources,
            onContinueClick = viewModel::onFactorSelected,
            onDismissSheet = viewModel::onDismissSelectFactor
        )
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                SetupRecoveryViewModel.Event.ShieldCreated -> onShieldCreated()
            }
        }
    }
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
    onFallbackPeriodClick: () -> Unit,
    onFallbackPeriodValueChange: (Int) -> Unit,
    onFallbackPeriodUnitChange: (SetupRecoveryViewModel.State.FallbackPeriod.Unit) -> Unit,
    onSetFallbackPeriodClick: () -> Unit,
    onDismissFallbackPeriod: () -> Unit,
    onShieldNameChange: (String) -> Unit,
    onDismissSetShieldName: () -> Unit,
    onConfirmShieldNameClick: () -> Unit,
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
                    factors = state.startRecoveryFactors,
                    onAddFactorClick = onAddStartRecoveryFactorClick,
                    onRemoveFactorClick = onRemoveStartRecoveryFactor
                )
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge)
            ) {
                SectionHeaderView(
                    title = "Confirming a Recovery", // TODO crowdin
                    subtitle = "Factors you can use to complete the recovery of your Accounts and Personas.", // TODO crowdin
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

                FactorsView(
                    factors = state.confirmationFactors,
                    onAddFactorClick = onAddConfirmRecoveryFactorClick,
                    onRemoveFactorClick = onRemoveConfirmRecoveryFactor
                )

                Text(
                    modifier = Modifier
                        .padding(vertical = RadixTheme.dimensions.paddingDefault)
                        .align(Alignment.CenterHorizontally),
                    text = "OR", // TODO crowdin
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray1
                )

                state.fallbackPeriod?.let { period ->
                    EmergencyFallbackView(
                        period = period,
                        onInfoClick = onInfoClick,
                        onNumberOfDaysClick = onFallbackPeriodClick
                    )
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
            }
        }
    }

    if (state.selectFallbackPeriod != null) {
        SelectFallbackPeriodSheet(
            selectFallbackPeriod = state.selectFallbackPeriod,
            onValueChange = onFallbackPeriodValueChange,
            onUnitChange = onFallbackPeriodUnitChange,
            onSetClick = onSetFallbackPeriodClick,
            onDismiss = onDismissFallbackPeriod
        )
    }

    if (state.setShieldName != null) {
        SetShieldNameSheet(
            input = state.setShieldName,
            onNameChange = onShieldNameChange,
            onConfirmClick = onConfirmShieldNameClick,
            onDismiss = onDismissSetShieldName
        )
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
private fun EmergencyFallbackView(
    modifier: Modifier = Modifier,
    period: SetupRecoveryViewModel.State.FallbackPeriod,
    onInfoClick: (GlossaryItem) -> Unit,
    onNumberOfDaysClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.lightRed,
                shape = RadixTheme.shapes.roundedRectMedium
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = RadixTheme.colors.red1,
                    shape = RadixTheme.shapes.roundedRectTopMedium
                )
                .clip(RadixTheme.shapes.roundedRectTopMedium)
                .clickable { onInfoClick(GlossaryItem.emergencyFallback) }
                .padding(RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "Emergency Fallback", // TODO crowdin
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.white
            )

            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(DSR.ic_info_outline),
                tint = RadixTheme.colors.white,
                contentDescription = "info"
            )
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = "Set a time period to automatically confirm recovery **WITHOUT** presenting any of the above confirmation factors." // TODO crowdin
                .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))

        Row(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                .fillMaxWidth()
                .height(RadixTheme.dimensions.buttonDefaultHeight)
                .shadow(
                    elevation = 2.dp,
                    shape = RadixTheme.shapes.roundedRectSmall
                )
                .background(
                    color = RadixTheme.colors.white,
                    shape = RadixTheme.shapes.roundedRectSmall
                )
                .throttleClickable { onNumberOfDaysClick() }
                .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Icon(
                painter = painterResource(id = DSR.ic_calendar),
                contentDescription = null
            )

            Text(
                text = period.title(),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
            text = "We recommend setting this for an extended period, so you have time to notice and cancel a recovery you don’t want.", // TODO crowdin
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.red1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
    }
}

@Composable
private fun SelectFallbackPeriodSheet(
    selectFallbackPeriod: SetupRecoveryViewModel.State.SelectFallbackPeriod,
    onValueChange: (Int) -> Unit,
    onUnitChange: (SetupRecoveryViewModel.State.FallbackPeriod.Unit) -> Unit,
    onSetClick: () -> Unit,
    onDismiss: () -> Unit
) {
    BottomSheetDialogWrapper(
        addScrim = true,
        showDragHandle = true,
        dragToDismissEnabled = false,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                text = "Emergency Fallback", // TODO crowdin
                style = RadixTheme.typography.header,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                text = "Set a time period to automatically confirm recovery **WITHOUT** presenting any confirmation factors." // TODO crowdin
                    .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Row(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXXLarge)
            ) {
                ListItemPicker(
                    modifier = Modifier.weight(1f),
                    displayMode = ListItemPicker.DisplayMode.Normal,
                    items = selectFallbackPeriod.values,
                    selectedValue = selectFallbackPeriod.currentValue,
                    onValueChange = onValueChange,
                    label = { item -> item.toString() },
                    contentAlignment = Alignment.CenterEnd,
                    contentPadding = PaddingValues(end = RadixTheme.dimensions.paddingSemiLarge)
                )

                ListItemPicker(
                    modifier = Modifier.weight(1f),
                    displayMode = ListItemPicker.DisplayMode.Normal,
                    items = selectFallbackPeriod.units,
                    selectedValue = selectFallbackPeriod.currentUnit,
                    onValueChange = onUnitChange,
                    label = { item -> item.displayName() },
                    contentAlignment = Alignment.CenterStart,
                    contentPadding = PaddingValues(start = RadixTheme.dimensions.paddingSemiLarge)
                )
            }

            RadixBottomBar(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSetClick,
                text = "Set", // TODO crowdin
                enabled = true
            )
        }
    }
}

@Composable
private fun SetShieldNameSheet(
    input: SetupRecoveryViewModel.State.SetShieldName,
    onNameChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val inputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        inputFocusRequester.requestFocus()
    }

    BottomSheetDialogWrapper(
        addScrim = true,
        showDragHandle = true,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge),
                text = "Name your Security Shield", // TODO crowdin
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
                text = "Give this Security Shield a name, so you can identify it later.", // TODO crowdin
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge)
                    .focusRequester(focusRequester = inputFocusRequester),
                onValueChanged = onNameChange,
                keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words),
                value = input.name,
                singleLine = true,
                hintColor = RadixTheme.colors.gray2
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
        }

        RadixBottomBar(
            onClick = onConfirmClick,
            text = stringResource(R.string.common_confirm),
            insets = WindowInsets.navigationBars.union(WindowInsets.ime),
            enabled = input.isButtonEnabled
        )
    }
}

@Composable
private fun SetupRecoveryViewModel.State.FallbackPeriod.title(): String = "$value ${unit.displayName()}"

@Composable
private fun SetupRecoveryViewModel.State.FallbackPeriod.Unit.displayName(): String = when (this) {
    SetupRecoveryViewModel.State.FallbackPeriod.Unit.DAYS -> "Days" // TODO crowdin
    SetupRecoveryViewModel.State.FallbackPeriod.Unit.WEEKS -> "Weeks" // TODO crowdin
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
            onFallbackPeriodClick = {},
            onFallbackPeriodValueChange = {},
            onFallbackPeriodUnitChange = {},
            onSetFallbackPeriodClick = {},
            onDismissFallbackPeriod = {},
            onShieldNameChange = {},
            onDismissSetShieldName = {},
            onConfirmShieldNameClick = {},
            onContinueClick = {}
        )
    }
}

@UsesSampleValues
class SetupRecoveryPreviewProvider : PreviewParameterProvider<SetupRecoveryViewModel.State> {

    override val values: Sequence<SetupRecoveryViewModel.State>
        get() = sequenceOf(
            SetupRecoveryViewModel.State(
                startRecoveryFactors = persistentListOf(
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
                confirmationFactors = persistentListOf(
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
                ),
                fallbackPeriod = SetupRecoveryViewModel.State.FallbackPeriod(
                    value = 10,
                    unit = SetupRecoveryViewModel.State.FallbackPeriod.Unit.DAYS
                )
            ),
            SetupRecoveryViewModel.State(
                selectFallbackPeriod = SetupRecoveryViewModel.State.SelectFallbackPeriod(
                    currentValue = 10,
                    currentUnit = SetupRecoveryViewModel.State.FallbackPeriod.Unit.DAYS,
                    values = SetupRecoveryViewModel.State.FallbackPeriod.Unit.DAYS.possibleValues,
                    units = SetupRecoveryViewModel.State.FallbackPeriod.Unit.entries.toPersistentList()
                )
            ),
            SetupRecoveryViewModel.State(
                setShieldName = SetupRecoveryViewModel.State.SetShieldName(
                    name = ""
                )
            )
        )
}
