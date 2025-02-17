package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.regularaccess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.AddFactorButton
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.FactorsContainerView
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.ShieldBuilderTitleView
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.ShieldSetupMissingFactorStatusView
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.ShieldSetupNotEnoughFactorsStatusView
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.ShieldSetupUnsafeCombinationStatusView
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.choosefactor.ChooseFactorSourceBottomSheet
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.ListItemPicker
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.RemovableFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.MeasureViewSize
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.modifier.noIndicationClickable
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.SecurityShieldBuilderRuleViolation
import com.radixdlt.sargon.SecurityShieldBuilderStatus
import com.radixdlt.sargon.SecurityShieldBuilderStatusInvalidReason
import com.radixdlt.sargon.Threshold
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SetupRegularAccessScreen(
    viewModel: SetupRegularAccessViewModel,
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onContinue: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SetupRegularAccessContent(
        state = state,
        onDismiss = onDismiss,
        onInfoClick = onInfoClick,
        onNumberOfFactorsClick = viewModel::onThresholdClick,
        onNumberOfFactorsSelect = viewModel::onThresholdSelect,
        onNumberOfFactorsDismiss = viewModel::onThresholdSelectionDismiss,
        onAddThresholdFactorClick = viewModel::onAddThresholdFactorClick,
        onRemoveThresholdFactorClick = viewModel::onRemoveThresholdFactorClick,
        onAddOverrideClick = viewModel::onAddOverrideClick,
        onAddOverrideFactorClick = viewModel::onAddOverrideFactorClick,
        onRemoveOverrideFactorClick = viewModel::onRemoveOverrideFactorClick,
        onRemoveAllOverrideFactorsClick = viewModel::onRemoveAllOverrideFactorsClick,
        onAddAuthenticationFactorClick = viewModel::onAddAuthenticationFactorClick,
        onRemoveAuthenticationFactorClick = viewModel::onRemoveAuthenticationFactorClick,
        onContinueClick = onContinue
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
}

@Composable
private fun SetupRegularAccessContent(
    modifier: Modifier = Modifier,
    state: SetupRegularAccessViewModel.State,
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onNumberOfFactorsClick: () -> Unit,
    onNumberOfFactorsSelect: (Threshold) -> Unit,
    onNumberOfFactorsDismiss: () -> Unit,
    onAddThresholdFactorClick: () -> Unit,
    onRemoveThresholdFactorClick: (FactorSourceCard) -> Unit,
    onAddOverrideClick: () -> Unit,
    onAddOverrideFactorClick: () -> Unit,
    onRemoveOverrideFactorClick: (FactorSourceCard) -> Unit,
    onRemoveAllOverrideFactorsClick: () -> Unit,
    onAddAuthenticationFactorClick: () -> Unit,
    onRemoveAuthenticationFactorClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.shieldWizardRegularAccess_step_title),
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
                    imageRes = DSR.ic_regular_access,
                    title = stringResource(id = R.string.shieldWizardRegularAccess_title)
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                Text(
                    text = stringResource(id = R.string.shieldWizardRegularAccess_subtitle),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

                FactorListStatusView(
                    modifier = Modifier.padding(
                        start = RadixTheme.dimensions.paddingMedium,
                        end = RadixTheme.dimensions.paddingMedium,
                        top = RadixTheme.dimensions.paddingSemiLarge,
                        bottom = RadixTheme.dimensions.paddingSemiLarge
                    ),
                    status = state.factorListStatus,
                    onInfoClick = onInfoClick
                )

                ThresholdFactorsView(
                    numberOfFactors = state.threshold,
                    factors = state.thresholdFactors,
                    onNumberOfFactorsClick = onNumberOfFactorsClick,
                    onAddFactorClick = onAddThresholdFactorClick,
                    onRemoveFactorClick = onRemoveThresholdFactorClick
                )

                OverrideFactorsView(
                    isFactorsSectionVisible = state.isOverrideSectionVisible,
                    overrideFactors = state.overrideFactors,
                    onAddOverrideClick = onAddOverrideClick,
                    onAddFactorClick = onAddOverrideFactorClick,
                    onRemoveClick = onRemoveOverrideFactorClick,
                    onRemoveAllClick = onRemoveAllOverrideFactorsClick
                )
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

            HorizontalDivider(color = RadixTheme.colors.gray3)

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

            AuthenticationFactorView(
                factor = state.authenticationFactor,
                isMissing = state.isAuthSigningFactorMissing,
                onRemoveClick = onRemoveAuthenticationFactorClick,
                onAddClick = onAddAuthenticationFactorClick
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
        }
    }

    state.selectThreshold?.let {
        SelectNumberOfFactorsSheet(
            selectThreshold = it,
            onSelect = onNumberOfFactorsSelect,
            onDismiss = onNumberOfFactorsDismiss
        )
    }
}

@Composable
private fun FactorListStatusView(
    modifier: Modifier,
    status: SetupRegularAccessViewModel.State.FactorListStatus,
    onInfoClick: (GlossaryItem) -> Unit
) {
    when (status) {
        SetupRegularAccessViewModel.State.FactorListStatus.PrimaryEmpty -> ShieldSetupMissingFactorStatusView(
            modifier = modifier
        )
        SetupRegularAccessViewModel.State.FactorListStatus.NotEnoughFactors -> ShieldSetupNotEnoughFactorsStatusView(
            modifier = modifier,
            onInfoClick = onInfoClick
        )
        SetupRegularAccessViewModel.State.FactorListStatus.Unsafe -> ShieldSetupUnsafeCombinationStatusView(
            modifier = modifier,
            onInfoClick = onInfoClick
        )
        SetupRegularAccessViewModel.State.FactorListStatus.Ok -> {}
    }
}

@Composable
private fun OverrideFactorsView(
    isFactorsSectionVisible: Boolean,
    overrideFactors: PersistentList<FactorSourceCard>,
    onAddOverrideClick: () -> Unit,
    onAddFactorClick: () -> Unit,
    onRemoveClick: (FactorSourceCard) -> Unit,
    onRemoveAllClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isFactorsSectionVisible) {
            RadixTextButton(
                modifier = Modifier
                    .padding(top = RadixTheme.dimensions.paddingXXSmall)
                    .align(Alignment.End),
                text = stringResource(id = R.string.shieldWizardRegularAccess_override_button),
                leadingIcon = {
                    Icon(
                        modifier = Modifier.padding(end = RadixTheme.dimensions.paddingXXSmall),
                        painter = painterResource(id = DSR.ic_add_override),
                        contentDescription = null,
                        tint = RadixTheme.colors.blue2
                    )
                },
                isWithoutPadding = true,
                contentColor = RadixTheme.colors.blue2,
                throttleClicks = true,
                onClick = onAddOverrideClick
            )
        } else {
            Text(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingDefault)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(id = R.string.shieldWizardRegularAccess_combination_label),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray1
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = RadixTheme.colors.gray1,
                        shape = RadixTheme.shapes.roundedRectTopMedium
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.shieldWizardRegularAccess_override_title),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.white,
                )

                IconButton(
                    onClick = onRemoveAllClick
                ) {
                    Icon(
                        painter = painterResource(id = DSR.ic_close),
                        contentDescription = null,
                        tint = RadixTheme.colors.gray2
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = RadixTheme.colors.gray5,
                        shape = RadixTheme.shapes.roundedRectBottomMedium
                    )
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingMedium,
                        vertical = RadixTheme.dimensions.paddingDefault
                    )
            ) {
                Text(
                    modifier = Modifier.padding(
                        top = RadixTheme.dimensions.paddingSmall,
                        bottom = RadixTheme.dimensions.paddingMedium
                    ),
                    text = stringResource(id = R.string.shieldWizardRegularAccess_override_description),
                    color = RadixTheme.colors.gray1,
                    style = RadixTheme.typography.body2Regular
                )

                overrideFactors.forEachIndexed { index, item ->
                    RemovableFactorSourceCard(
                        item = item,
                        onRemoveClick = onRemoveClick
                    )

                    if (index < overrideFactors.size - 1) {
                        Text(
                            modifier = Modifier
                                .padding(end = RadixTheme.dimensions.paddingXXXLarge)
                                .padding(vertical = RadixTheme.dimensions.paddingDefault)
                                .align(Alignment.CenterHorizontally),
                            text = stringResource(id = R.string.shieldWizardRegularAccess_overrideCombination_label),
                            style = RadixTheme.typography.body1Link,
                            color = RadixTheme.colors.gray1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

                AddFactorButton(
                    onClick = onAddFactorClick
                )
            }
        }
    }
}

@Composable
private fun ThresholdFactorsView(
    modifier: Modifier = Modifier,
    numberOfFactors: Threshold,
    factors: PersistentList<FactorSourceCard>,
    onNumberOfFactorsClick: () -> Unit,
    onAddFactorClick: () -> Unit,
    onRemoveFactorClick: (FactorSourceCard) -> Unit
) {
    FactorsContainerView(
        modifier = modifier
    ) {
        val density = LocalDensity.current

        MeasureViewSize(
            modifier = Modifier.noIndicationClickable { onNumberOfFactorsClick() },
            viewToMeasure = { NumberOfFactorsView(numberOfFactors) },
        ) { measuredSize ->
            val inlineContentKey = "button"

            Text(
                text = buildAnnotatedString {
                    val annotatedPart = stringResource(id = R.string.shieldWizardRegularAccess_thresholdDescription_selection)
                    val text = stringResource(id = R.string.shieldWizardRegularAccess_thresholdDescription_title, annotatedPart)
                    val parts = text.split(annotatedPart)
                    append(parts.getOrNull(0).orEmpty())
                    appendInlineContent(id = inlineContentKey)
                    append(parts.getOrNull(1).orEmpty())
                },
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1,
                inlineContent = mapOf(
                    inlineContentKey to InlineTextContent(
                        with(density) {
                            Placeholder(
                                width = measuredSize.width.toSp(),
                                height = measuredSize.height.toSp(),
                                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                            )
                        }
                    ) {
                        NumberOfFactorsView(numberOfFactors)
                    }
                )
            )
        }

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
private fun NumberOfFactorsView(
    threshold: Threshold
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (threshold) {
                Threshold.All -> stringResource(id = R.string.shieldWizardRegularAccess_thresholdDescription_all)
                is Threshold.Specific -> threshold.v1.toString()
            },
            style = RadixTheme.typography.body2Link,
            color = RadixTheme.colors.blue2
        )

        Icon(
            painter = painterResource(id = DSR.ic_chevron_down),
            contentDescription = null,
            tint = RadixTheme.colors.blue2
        )
    }
}

@Composable
private fun AuthenticationFactorView(
    modifier: Modifier = Modifier,
    factor: FactorSourceCard?,
    isMissing: Boolean,
    onRemoveClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
    ) {
        Text(
            text = stringResource(id = R.string.shieldWizardRegularAccess_authentication_title),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

        if (isMissing) {
            ShieldSetupMissingFactorStatusView(
                modifier = Modifier.padding(
                    top = RadixTheme.dimensions.paddingSmall,
                    bottom = RadixTheme.dimensions.paddingSemiLarge
                )
            )
        }

        FactorsContainerView {
            if (factor != null) {
                RemovableFactorSourceCard(
                    item = factor,
                    onRemoveClick = { onRemoveClick() }
                )
            } else {
                AddFactorButton(
                    onClick = onAddClick
                )
            }
        }
    }
}

@Composable
private fun SelectNumberOfFactorsSheet(
    selectThreshold: SetupRegularAccessViewModel.State.SelectThreshold,
    onSelect: (Threshold) -> Unit,
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
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge),
                text = stringResource(id = R.string.shieldWizardRegularAccess_setThreshold_title),
                style = RadixTheme.typography.header,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

            var selectedItem by remember(selectThreshold.current) { mutableStateOf(selectThreshold.current) }

            ListItemPicker(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                items = selectThreshold.items,
                selectedValue = selectedItem,
                onValueChange = {
                    selectedItem = it
                },
                label = { item ->
                    when (item) {
                        Threshold.All -> stringResource(id = R.string.shieldWizardRegularAccess_setThreshold_all)
                        is Threshold.Specific -> item.v1.toString()
                    }
                }
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

            RadixBottomBar(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelect(selectedItem) },
                text = stringResource(id = R.string.shieldWizardRegularAccess_setThreshold_button),
                enabled = true
            )
        }
    }
}

@Composable
@Preview
@UsesSampleValues
private fun RegularAccessPreview(
    @PreviewParameter(RegularAccessPreviewProvider::class) state: SetupRegularAccessViewModel.State
) {
    RadixWalletPreviewTheme {
        SetupRegularAccessContent(
            state = state,
            onDismiss = {},
            onInfoClick = {},
            onNumberOfFactorsClick = {},
            onNumberOfFactorsSelect = {},
            onNumberOfFactorsDismiss = {},
            onAddThresholdFactorClick = {},
            onRemoveThresholdFactorClick = {},
            onAddOverrideClick = {},
            onAddOverrideFactorClick = {},
            onRemoveOverrideFactorClick = {},
            onRemoveAllOverrideFactorsClick = {},
            onAddAuthenticationFactorClick = {},
            onRemoveAuthenticationFactorClick = {},
            onContinueClick = {}
        )
    }
}

@UsesSampleValues
class RegularAccessPreviewProvider : PreviewParameterProvider<SetupRegularAccessViewModel.State> {

    override val values: Sequence<SetupRegularAccessViewModel.State>
        get() = sequenceOf(
            SetupRegularAccessViewModel.State(
                thresholdFactors = persistentListOf(
                    FactorSourceCard(
                        id = FactorSourceId.Hash.init(
                            kind = FactorSourceKind.DEVICE,
                            mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                        ),
                        name = "My Phone",
                        includeDescription = true,
                        lastUsedOn = null,
                        kind = FactorSourceKind.DEVICE,
                        messages = persistentListOf(),
                        accounts = persistentListOf(),
                        personas = persistentListOf(),
                        hasHiddenEntities = false,
                        isEnabled = true
                    ),
                    FactorSourceCard(
                        id = FactorSourceId.Hash.init(
                            kind = FactorSourceKind.ARCULUS_CARD,
                            mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                        ),
                        name = "Arculus Card Secret",
                        includeDescription = true,
                        lastUsedOn = null,
                        kind = FactorSourceKind.ARCULUS_CARD,
                        messages = persistentListOf(),
                        accounts = persistentListOf(),
                        personas = persistentListOf(),
                        hasHiddenEntities = false,
                        isEnabled = true
                    )
                ),
                overrideFactors = persistentListOf(
                    FactorSourceCard(
                        id = FactorSourceId.Hash.init(
                            kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                            mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                        ),
                        name = "My Secret Stick",
                        includeDescription = true,
                        lastUsedOn = null,
                        kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                        messages = persistentListOf(),
                        accounts = persistentListOf(),
                        personas = persistentListOf(),
                        hasHiddenEntities = false,
                        isEnabled = true
                    ),
                    FactorSourceCard(
                        id = FactorSourceId.Hash.init(
                            kind = FactorSourceKind.OFF_DEVICE_MNEMONIC,
                            mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                        ),
                        name = "ShizzleWords",
                        includeDescription = true,
                        lastUsedOn = null,
                        kind = FactorSourceKind.OFF_DEVICE_MNEMONIC,
                        messages = persistentListOf(),
                        accounts = persistentListOf(),
                        personas = persistentListOf(),
                        hasHiddenEntities = false,
                        isEnabled = true
                    )
                ),
                authenticationFactor = FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.DEVICE,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "My Phone",
                    includeDescription = true,
                    lastUsedOn = null,
                    kind = FactorSourceKind.DEVICE,
                    messages = persistentListOf(),
                    accounts = persistentListOf(),
                    personas = persistentListOf(),
                    hasHiddenEntities = false,
                    isEnabled = true
                ),
            ),
            SetupRegularAccessViewModel.State(
                selectThreshold = null,
                threshold = Threshold.All,
                status = SecurityShieldBuilderStatus.Invalid(
                    reason = SecurityShieldBuilderStatusInvalidReason(
                        isPrimaryRoleFactorListEmpty = false,
                        isAuthSigningFactorMissing = false,
                        isRecoveryRoleFactorListEmpty = true,
                        isConfirmationRoleFactorListEmpty = false
                    )
                )
            ),
            SetupRegularAccessViewModel.State(
                selectThreshold = null,
                threshold = Threshold.Specific(2.toUByte()),
                status = SecurityShieldBuilderStatus.Invalid(
                    reason = SecurityShieldBuilderStatusInvalidReason(
                        isPrimaryRoleFactorListEmpty = true,
                        isAuthSigningFactorMissing = true,
                        isRecoveryRoleFactorListEmpty = false,
                        isConfirmationRoleFactorListEmpty = false
                    )
                )
            ),
            SetupRegularAccessViewModel.State(
                thresholdFactors = persistentListOf(
                    FactorSourceCard(
                        id = FactorSourceId.Hash.init(
                            kind = FactorSourceKind.DEVICE,
                            mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                        ),
                        name = "My Phone",
                        includeDescription = true,
                        lastUsedOn = null,
                        kind = FactorSourceKind.DEVICE,
                        messages = persistentListOf(),
                        accounts = persistentListOf(),
                        personas = persistentListOf(),
                        hasHiddenEntities = false,
                        isEnabled = true
                    ),
                    FactorSourceCard(
                        id = FactorSourceId.Hash.init(
                            kind = FactorSourceKind.DEVICE,
                            mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                        ),
                        name = "My second phone",
                        includeDescription = true,
                        lastUsedOn = null,
                        kind = FactorSourceKind.DEVICE,
                        messages = persistentListOf(),
                        accounts = persistentListOf(),
                        personas = persistentListOf(),
                        hasHiddenEntities = false,
                        isEnabled = true
                    )
                ),
                selectThreshold = null,
                threshold = Threshold.Specific(2.toUByte()),
                status = SecurityShieldBuilderStatus.Weak(
                    reason = SecurityShieldBuilderRuleViolation.PrimaryCannotHaveMultipleDevices()
                )
            ),
            SetupRegularAccessViewModel.State(
                selectThreshold = SetupRegularAccessViewModel.State.SelectThreshold(
                    current = Threshold.All,
                    items = persistentListOf(
                        Threshold.All,
                        Threshold.Specific(2.toUByte()),
                        Threshold.Specific(1.toUByte())
                    )
                ),
                threshold = Threshold.Specific(2.toUByte()),
                status = SecurityShieldBuilderStatus.Invalid(
                    reason = SecurityShieldBuilderStatusInvalidReason(
                        isPrimaryRoleFactorListEmpty = true,
                        isAuthSigningFactorMissing = false,
                        isRecoveryRoleFactorListEmpty = false,
                        isConfirmationRoleFactorListEmpty = false
                    )
                )
            )
        )
}
