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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.AddFactorButton
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.FactorsContainerView
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.ShieldBuilderTitleView
import com.babylon.wallet.android.presentation.settings.securitycenter.common.composables.ShieldSetupStatusView
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.ListItemPicker
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.StatusMessageText
import com.babylon.wallet.android.presentation.ui.composables.card.RemovableFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.MeasureViewSize
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.StatusMessage
import com.babylon.wallet.android.presentation.ui.modifier.noIndicationClickable
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.SecurityShieldBuilderInvalidReason
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
        onNumberOfFactorsClick = viewModel::onNumberOfFactorsClick,
        onNumberOfFactorsSelect = viewModel::onNumberOfFactorsSelect,
        onNumberOfFactorsDismiss = viewModel::onNumberOfFactorsSelectionDismiss,
        onAddFactorClick = viewModel::onAddThresholdFactorClick,
        onRemoveFactorClick = viewModel::onRemoveThresholdFactorClick,
        onAddOverrideClick = viewModel::onAddOverrideClick,
        onRemoveOverrideFactorClick = viewModel::onRemoveOverrideFactorClick,
        onRemoveAllOverrideFactorsClick = viewModel::onRemoveAllOverrideFactorsClick,
        onAddAuthenticationFactorClick = viewModel::onAddAuthenticationFactorClick,
        onRemoveAuthenticationFactorClick = viewModel::onRemoveAuthenticationFactorClick,
        onContinueClick = onContinue
    )
}

@Composable
private fun SetupRegularAccessContent(
    modifier: Modifier = Modifier,
    state: SetupRegularAccessViewModel.State,
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onNumberOfFactorsClick: () -> Unit,
    onNumberOfFactorsSelect: (SetupRegularAccessViewModel.State.NumberOfFactors) -> Unit,
    onNumberOfFactorsDismiss: () -> Unit,
    onAddFactorClick: () -> Unit,
    onRemoveFactorClick: (FactorSourceCard) -> Unit,
    onAddOverrideClick: () -> Unit,
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
                title = "Step 1 of 2", // TODO crowdin
                onBackClick = onDismiss,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                text = stringResource(R.string.common_continue)
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
                    title = "Regular Access", // TODO crowdin
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

                Text(
                    text = "Factors required to withdraw assets from Accounts.", // TODO crowdin
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
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

                ThresholdFactorsView(
                    numberOfFactors = state.numberOfFactors,
                    factors = state.thresholdFactors,
                    onNumberOfFactorsClick = onNumberOfFactorsClick,
                    onAddFactorClick = onAddFactorClick,
                    onRemoveFactorClick = onRemoveFactorClick
                )

                OverrideFactorsView(
                    overrideFactors = state.overrideFactors,
                    onAddClick = onAddOverrideClick,
                    onRemoveClick = onRemoveOverrideFactorClick,
                    onRemoveAllClick = onRemoveAllOverrideFactorsClick
                )
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

            HorizontalDivider(color = RadixTheme.colors.gray3)

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

            AuthenticationFactorView(
                factor = state.authenticationFactor,
                onRemoveClick = onRemoveAuthenticationFactorClick,
                onAddClick = onAddAuthenticationFactorClick
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
        }
    }

    state.selectNumberOfFactors?.let {
        SelectNumberOfFactorsSheet(
            selectNumberOfFactors = it,
            onSelect = onNumberOfFactorsSelect,
            onDismiss = onNumberOfFactorsDismiss
        )
    }
}

@Composable
private fun OverrideFactorsView(
    overrideFactors: PersistentList<FactorSourceCard>,
    onAddClick: () -> Unit,
    onRemoveClick: (FactorSourceCard) -> Unit,
    onRemoveAllClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (overrideFactors.isEmpty()) {
            RadixTextButton(
                modifier = Modifier
                    .padding(top = RadixTheme.dimensions.paddingXXSmall)
                    .align(Alignment.End),
                text = "Add an override",
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
                onClick = onAddClick
            )
        } else {
            Text(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingDefault)
                    .align(Alignment.CenterHorizontally),
                text = "OR", // TODO crowdin
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
                    text = "Override (advanced)", // TODO crowdin
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
                    text = "Or you can use the following:",
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
                            text = "OR", // TODO crowdin
                            style = RadixTheme.typography.body1Link,
                            color = RadixTheme.colors.gray1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

                AddFactorButton(
                    onClick = onAddClick
                )
            }
        }
    }
}

@Composable
private fun ThresholdFactorsView(
    modifier: Modifier = Modifier,
    numberOfFactors: SetupRegularAccessViewModel.State.NumberOfFactors,
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
            Text(
                text = buildAnnotatedString {
                    append("You'll need to use") // TODO crowdin
                    appendInlineContent(id = "button")
                    append("of the following:") // TODO crowdin
                },
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1,
                inlineContent = mapOf(
                    "button" to InlineTextContent(
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
    numberOfFactors: SetupRegularAccessViewModel.State.NumberOfFactors
) {
    Row(
        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (numberOfFactors) {
                SetupRegularAccessViewModel.State.NumberOfFactors.All -> "All" // TODO crowdin
                is SetupRegularAccessViewModel.State.NumberOfFactors.Count -> numberOfFactors.value.toString()
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
    onRemoveClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
    ) {
        Text(
            text = "Single factor required to log in to dApps with Personas and prove ownership of Accounts.",
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))

        if (factor == null) {
            StatusMessageText(
                modifier = Modifier.padding(
                    top = RadixTheme.dimensions.paddingSmall,
                    bottom = RadixTheme.dimensions.paddingSemiLarge
                ),
                message = StatusMessage(
                    message = "You need to choose a factor to continue", // TODO crowdin
                    type = StatusMessage.Type.WARNING
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
    selectNumberOfFactors: SetupRegularAccessViewModel.State.SelectNumberOfFactors,
    onSelect: (SetupRegularAccessViewModel.State.NumberOfFactors) -> Unit,
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
                text = "Set the number of factors required to sign", // TODO crowdin
                style = RadixTheme.typography.header,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXXLarge))

            var selectedItem by remember(selectNumberOfFactors.current) { mutableStateOf(selectNumberOfFactors.current) }

            ListItemPicker(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                items = selectNumberOfFactors.items,
                selectedValue = selectedItem,
                onValueChange = {
                    selectedItem = it
                },
                label = { item ->
                    when (item) {
                        SetupRegularAccessViewModel.State.NumberOfFactors.All -> "All (Recommended)" // TODO crowdin
                        is SetupRegularAccessViewModel.State.NumberOfFactors.Count -> item.value.toString()
                    }
                }
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXXLarge))

            RadixBottomBar(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelect(selectedItem) },
                text = "Set", // TODO crowdin
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
            onAddFactorClick = {},
            onRemoveFactorClick = {},
            onAddOverrideClick = {},
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
                        hasHiddenEntities = false
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
                        hasHiddenEntities = false
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
                        hasHiddenEntities = false
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
                        hasHiddenEntities = false
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
                    hasHiddenEntities = false
                ),
            ),
            SetupRegularAccessViewModel.State(
                selectNumberOfFactors = SetupRegularAccessViewModel.State.SelectNumberOfFactors(
                    current = SetupRegularAccessViewModel.State.NumberOfFactors.All,
                    items = persistentListOf(
                        SetupRegularAccessViewModel.State.NumberOfFactors.All,
                        SetupRegularAccessViewModel.State.NumberOfFactors.Count(2),
                        SetupRegularAccessViewModel.State.NumberOfFactors.Count(1)
                    )
                ),
                numberOfFactors = SetupRegularAccessViewModel.State.NumberOfFactors.Count(2),
                status = SecurityShieldBuilderInvalidReason.PrimaryRoleMustHaveAtLeastOneFactor()
            )
        )
}
