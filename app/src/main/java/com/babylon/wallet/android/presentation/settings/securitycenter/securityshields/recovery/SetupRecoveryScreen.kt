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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.presentation.common.displayName
import com.babylon.wallet.android.presentation.common.title
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.AddFactorButton
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.FactorsContainerView
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.ShieldBuilderTitleView
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.ShieldSetupMissingFactorStatusView
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.ShieldSetupUnsafeCombinationStatusView
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.choosefactor.ChooseFactorSourceBottomSheet
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.ListItemPicker
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.RemovableFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.SecurityShieldBuilderRuleViolation
import com.radixdlt.sargon.SecurityShieldBuilderStatus
import com.radixdlt.sargon.SecurityShieldBuilderStatusInvalidReason
import com.radixdlt.sargon.TimePeriod
import com.radixdlt.sargon.TimePeriodUnit
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.values
import com.radixdlt.sargon.samples.sample
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun SetupRecoveryScreen(
    viewModel: SetupRecoveryViewModel,
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    toNameSetup: () -> Unit,
    onDismissFlow: () -> Unit
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
        onContinueClick = viewModel::onContinueClick,
        onUnsafeCombinationInfoDismiss = viewModel::onUnsafeCombinationInfoDismiss,
        onUnsafeCombinationInfoConfirm = viewModel::onUnsafeCombinationInfoConfirm,
        onDismissMessage = viewModel::onDismissMessage
    )

    state.selectFactor?.let { selectFactor ->
        ChooseFactorSourceBottomSheet(
            viewModel = hiltViewModel(),
            unusableFactorSourceKinds = selectFactor.unusableFactorSourceKinds,
            alreadySelectedFactorSources = selectFactor.alreadySelectedFactorSources,
            onContinueClick = viewModel::onFactorSelected,
            onInfoClick = onInfoClick,
            onDismissSheet = viewModel::onDismissSelectFactor
        )
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                SetupRecoveryViewModel.Event.ToNameSetup -> toNameSetup()
                SetupRecoveryViewModel.Event.DismissFlow -> onDismissFlow()
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
    onFallbackPeriodUnitChange: (TimePeriodUnit) -> Unit,
    onSetFallbackPeriodClick: () -> Unit,
    onDismissFallbackPeriod: () -> Unit,
    onContinueClick: () -> Unit,
    onUnsafeCombinationInfoDismiss: () -> Unit,
    onUnsafeCombinationInfoConfirm: () -> Unit,
    onDismissMessage: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.shieldWizardRecovery_step_title),
                onBackClick = onDismiss,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                text = if (state.isNewShield) {
                    stringResource(R.string.common_continue)
                } else {
                    stringResource(R.string.common_confirm)
                },
                enabled = state.isButtonEnabled
            )
        },
        containerColor = RadixTheme.colors.background
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
                    imageRes = if (RadixTheme.config.isDarkTheme) {
                        DSR.ic_shield_recovery_dark
                    } else {
                        DSR.ic_shield_recovery
                    },
                    title = stringResource(id = R.string.shieldWizardRecovery_title),
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                if (state.isCombinationUnsafe) {
                    ShieldSetupUnsafeCombinationStatusView(
                        modifier = Modifier.padding(
                            start = RadixTheme.dimensions.paddingSmall,
                            end = RadixTheme.dimensions.paddingSmall,
                            top = RadixTheme.dimensions.paddingDefault,
                            bottom = RadixTheme.dimensions.paddingXXLarge
                        ),
                        onInfoClick = onInfoClick
                    )
                }

                SectionHeaderView(
                    title = stringResource(id = R.string.shieldWizardRecovery_start_title),
                    subtitle = stringResource(id = R.string.shieldWizardRecovery_start_subtitle),
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

                if (state.isRecoveryListEmpty) {
                    ShieldSetupMissingFactorStatusView(
                        modifier = Modifier.padding(
                            start = RadixTheme.dimensions.paddingMedium,
                            end = RadixTheme.dimensions.paddingMedium,
                            top = RadixTheme.dimensions.paddingMedium,
                            bottom = RadixTheme.dimensions.paddingLarge
                        )
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
                    title = stringResource(id = R.string.shieldWizardRecovery_confirm_title),
                    subtitle = stringResource(id = R.string.shieldWizardRecovery_confirm_subtitle),
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

                if (state.isConfirmationListEmpty) {
                    ShieldSetupMissingFactorStatusView(
                        modifier = Modifier.padding(
                            start = RadixTheme.dimensions.paddingMedium,
                            end = RadixTheme.dimensions.paddingMedium,
                            top = RadixTheme.dimensions.paddingMedium,
                            bottom = RadixTheme.dimensions.paddingLarge
                        )
                    )
                }

                FactorsView(
                    factors = state.confirmationFactors,
                    onAddFactorClick = onAddConfirmRecoveryFactorClick,
                    onRemoveFactorClick = onRemoveConfirmRecoveryFactor
                )

                state.fallbackPeriod?.let { period ->
                    Text(
                        modifier = Modifier
                            .padding(vertical = RadixTheme.dimensions.paddingDefault)
                            .align(Alignment.CenterHorizontally),
                        text = stringResource(id = R.string.shieldWizardRecovery_combination_label),
                        style = RadixTheme.typography.body1Link,
                        color = RadixTheme.colors.text
                    )

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

    if (state.showUnsafeCombinationInfo) {
        UnsafeCombinationInfoDialog(
            onCancelClick = onUnsafeCombinationInfoDismiss,
            onContinueClick = onUnsafeCombinationInfoConfirm
        )
    }

    state.errorMessage?.let {
        ErrorAlertDialog(
            errorMessage = it,
            cancel = onDismissMessage
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
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            text = subtitle,
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.text
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
            text = stringResource(id = R.string.shieldWizardRecovery_factors_title),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

        if (factors.isNotEmpty()) {
            factors.forEachIndexed { index, factor ->
                RemovableFactorSourceCard(
                    item = factor,
                    onRemoveClick = onRemoveFactorClick
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                if (index < factors.size - 1) {
                    Text(
                        modifier = Modifier
                            .padding(end = RadixTheme.dimensions.paddingXXXLarge)
                            .align(Alignment.CenterHorizontally),
                        text = stringResource(id = R.string.shieldWizardRecovery_combination_label),
                        style = RadixTheme.typography.body1Link,
                        color = RadixTheme.colors.text
                    )

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
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
    period: TimePeriod,
    onInfoClick: (GlossaryItem) -> Unit,
    onNumberOfDaysClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.errorSecondary,
                shape = RadixTheme.shapes.roundedRectMedium
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = RadixTheme.colors.error,
                    shape = RadixTheme.shapes.roundedRectTopMedium
                )
                .clip(RadixTheme.shapes.roundedRectTopMedium)
                .clickable { onInfoClick(GlossaryItem.emergencyfallback) }
                .padding(RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.shieldWizardRecovery_fallback_title),
                style = RadixTheme.typography.body1Header,
                color = White
            )

            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(DSR.ic_info_outline),
                tint = White,
                contentDescription = "info"
            )
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.shieldWizardRecovery_fallback_subtitle)
                .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

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
                    color = RadixTheme.colors.card,
                    shape = RadixTheme.shapes.roundedRectSmall
                )
                .throttleClickable { onNumberOfDaysClick() }
                .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Icon(
                painter = painterResource(id = DSR.ic_calendar),
                contentDescription = null,
                tint = RadixTheme.colors.icon
            )

            Text(
                text = period.title(),
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.text
            )
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
            text = stringResource(id = R.string.shieldWizardRecovery_fallback_note),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.error
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
    }
}

@Composable
private fun SelectFallbackPeriodSheet(
    selectFallbackPeriod: SetupRecoveryViewModel.State.SelectFallbackPeriod,
    onValueChange: (Int) -> Unit,
    onUnitChange: (TimePeriodUnit) -> Unit,
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
                text = stringResource(id = R.string.shieldWizardRecovery_setFallback_title),
                style = RadixTheme.typography.header,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                text = stringResource(id = R.string.shieldWizardRecovery_setFallback_subtitle)
                    .formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.text,
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
                text = stringResource(id = R.string.shieldWizardRecovery_setFallback_button),
                enabled = true
            )
        }
    }
}

@Composable
private fun UnsafeCombinationInfoDialog(
    modifier: Modifier = Modifier,
    onCancelClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    BasicPromptAlertDialog(
        modifier = modifier,
        finish = { accepted ->
            if (accepted) {
                onContinueClick()
            } else {
                onCancelClick()
            }
        },
        message = {
            Text(
                text = stringResource(id = R.string.shieldSetupStatus_unsafeCombination_message),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.text
            )
        },
        confirmText = stringResource(id = R.string.shieldSetupStatus_unsafeCombination_confirm),
        dismissText = stringResource(id = R.string.shieldSetupStatus_unsafeCombination_cancel),
        confirmTextColor = RadixTheme.colors.error
    )
}

@Composable
@Preview
@UsesSampleValues
private fun SetupRecoveryLightPreview(
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
            onContinueClick = {},
            onUnsafeCombinationInfoDismiss = {},
            onUnsafeCombinationInfoConfirm = {},
            onDismissMessage = {}
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun SetupRecoveryDarkPreview(
    @PreviewParameter(SetupRecoveryPreviewProvider::class) state: SetupRecoveryViewModel.State
) {
    RadixWalletPreviewTheme(
        enableDarkTheme = true
    ) {
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
            onContinueClick = {},
            onUnsafeCombinationInfoDismiss = {},
            onUnsafeCombinationInfoConfirm = {},
            onDismissMessage = {}
        )
    }
}

@UsesSampleValues
class SetupRecoveryPreviewProvider : PreviewParameterProvider<SetupRecoveryViewModel.State> {

    override val values: Sequence<SetupRecoveryViewModel.State>
        get() = sequenceOf(
            SetupRecoveryViewModel.State(
                startRecoveryFactors = persistentListOf(
                    LedgerHardwareWalletFactorSource.sample().asGeneral().toFactorSourceCard(),
                    LedgerHardwareWalletFactorSource.sample.other().asGeneral().toFactorSourceCard()
                ),
                confirmationFactors = persistentListOf(
                    LedgerHardwareWalletFactorSource.sample().asGeneral().toFactorSourceCard(),
                    LedgerHardwareWalletFactorSource.sample.other().asGeneral().toFactorSourceCard()
                ),
                fallbackPeriod = TimePeriod.sample()
            ),
            SetupRecoveryViewModel.State(
                startRecoveryFactors = persistentListOf(
                    LedgerHardwareWalletFactorSource.sample().asGeneral().toFactorSourceCard(),
                ),
                confirmationFactors = persistentListOf(
                    LedgerHardwareWalletFactorSource.sample.other().asGeneral().toFactorSourceCard()
                ),
                status = SecurityShieldBuilderStatus.Weak(
                    reason = SecurityShieldBuilderRuleViolation.RecoveryAndConfirmationFactorsOverlap()
                ),
                fallbackPeriod = TimePeriod.sample()
            ),
            SetupRecoveryViewModel.State(
                status = SecurityShieldBuilderStatus.Invalid(
                    reason = SecurityShieldBuilderStatusInvalidReason(
                        isPrimaryRoleFactorListEmpty = true,
                        isAuthSigningFactorMissing = false,
                        isRecoveryRoleFactorListEmpty = false,
                        isConfirmationRoleFactorListEmpty = false
                    )
                ),
                fallbackPeriod = TimePeriod.sample()
            ),
            SetupRecoveryViewModel.State(
                status = SecurityShieldBuilderStatus.Invalid(
                    reason = SecurityShieldBuilderStatusInvalidReason(
                        isPrimaryRoleFactorListEmpty = false,
                        isAuthSigningFactorMissing = false,
                        isRecoveryRoleFactorListEmpty = true,
                        isConfirmationRoleFactorListEmpty = true
                    )
                ),
                fallbackPeriod = TimePeriod.sample()
            ),
            SetupRecoveryViewModel.State(
                selectFallbackPeriod = SetupRecoveryViewModel.State.SelectFallbackPeriod(
                    currentValue = 10,
                    currentUnit = TimePeriodUnit.DAYS,
                    values = TimePeriodUnit.DAYS.values.toPersistentList(),
                    units = TimePeriodUnit.entries.toPersistentList()
                ),
                fallbackPeriod = TimePeriod.sample()
            ),
            SetupRecoveryViewModel.State(
                showUnsafeCombinationInfo = true,
                fallbackPeriod = TimePeriod.sample()
            )
        )
}
