package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin.BiometricsPinViewModel.State
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.composables.FactorSourcesList
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.SimpleSelectableSingleChoiceFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.SyncSheetState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleStokenet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricsPinScreen(
    viewModel: BiometricsPinViewModel,
    onBackClick: () -> Unit,
    onNavigateToDeviceFactorSourceDetails: (factorSourceId: FactorSourceId) -> Unit,
    onNavigateToAddBiometricPin: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is BiometricsPinViewModel.Event.NavigateToDeviceFactorSourceDetails -> {
                    onNavigateToDeviceFactorSourceDetails(event.factorSourceId)
                }
            }
        }
    }

    BiometricsPinContent(
        state = state,
        onBackClick = onBackClick,
        onDeviceFactorSourceClick = viewModel::onDeviceFactorSourceClick,
        onAddBiometricsPinClick = onNavigateToAddBiometricPin,
        onChangeMainDeviceFactorSourceClick = viewModel::onChangeMainDeviceFactorSourceClick,
        onInfoClick = onInfoClick
    )

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SyncSheetState(
        sheetState = bottomSheetState,
        isSheetVisible = state.isMainDeviceFactorSourceBottomSheetVisible,
        onSheetClosed = viewModel::onDismissMainDeviceFactorSourceBottomSheet
    )

    if (state.isMainDeviceFactorSourceBottomSheetVisible) {
        DefaultModalSheetLayout(
            enableImePadding = false,
            sheetState = bottomSheetState,
            sheetContent = {
                ChangeMainDeviceFactorSourceContent(
                    factorSources = state.selectableDeviceFactorIds,
                    isContinueButtonEnabled = state.isContinueButtonEnabled,
                    isChangingMainDeviceFactorSourceInProgress = state.isChangingMainDeviceFactorSourceInProgress,
                    onDeviceFactorSourceSelect = viewModel::onDeviceFactorSourceSelect,
                    onContinueClick = viewModel::onConfirmChangeMainDeviceFactorSource,
                    onDismissClick = viewModel::onDismissMainDeviceFactorSourceBottomSheet
                )
            },
            onDismissRequest = viewModel::onDismissMainDeviceFactorSourceBottomSheet
        )
    }
}

@Composable
private fun BiometricsPinContent(
    modifier: Modifier = Modifier,
    state: State,
    onBackClick: () -> Unit,
    onDeviceFactorSourceClick: (FactorSourceId) -> Unit,
    onAddBiometricsPinClick: () -> Unit,
    onChangeMainDeviceFactorSourceClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.factorSources_card_deviceTitle),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.Start
        ) {
            HorizontalDivider(color = RadixTheme.colors.gray4)

            FactorSourcesList(
                mainFactorSource = state.mainDeviceFactorSource,
                factorSources = state.otherDeviceFactorSources,
                factorSourceDescriptionText = R.string.factorSources_card_deviceDescription,
                addFactorSourceButtonTitle = R.string.factorSources_list_deviceAdd,
                glossaryItem = GlossaryItem.biometricspin,
                onFactorSourceClick = onDeviceFactorSourceClick,
                onAddFactorSourceClick = onAddBiometricsPinClick,
                onChangeMainFactorSourceClick = onChangeMainDeviceFactorSourceClick,
                onInfoClick = onInfoClick
            )
        }
    }
}

@Composable
private fun ChangeMainDeviceFactorSourceContent(
    modifier: Modifier = Modifier,
    factorSources: ImmutableList<Selectable<FactorSourceCard>>,
    isContinueButtonEnabled: Boolean,
    isChangingMainDeviceFactorSourceInProgress: Boolean,
    onDeviceFactorSourceSelect: (FactorSourceCard) -> Unit,
    onContinueClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.empty),
                containerColor = RadixTheme.colors.defaultBackground,
                windowInsets = WindowInsets(0.dp),
                backIconType = BackIconType.Close,
                onBackClick = onDismissClick,
            )
        },
        bottomBar = {
            RadixBottomBar(
                enabled = isContinueButtonEnabled,
                isLoading = isChangingMainDeviceFactorSourceInProgress,
                onClick = onContinueClick,
                text = stringResource(R.string.common_continue),
                insets = WindowInsets(0.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .background(RadixTheme.colors.defaultBackground)
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            item {
                Text(
                    text = stringResource(R.string.factorSources_changeMain_title),
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )
            }

            item {
                Text(
                    text = stringResource(id = R.string.factorSources_changeMain_subtitle),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            }

            items(factorSources) {
                SimpleSelectableSingleChoiceFactorSourceCard(
                    item = it,
                    onSelect = onDeviceFactorSourceSelect
                )
            }
        }
    }
}

@UsesSampleValues
private val otherDeviceFactorSources = persistentListOf(
    FactorSourceCard(
        id = FactorSourceId.Hash.init(
            kind = FactorSourceKind.DEVICE,
            mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
        ),
        name = "My Phone",
        includeDescription = false,
        lastUsedOn = "Today",
        kind = FactorSourceKind.DEVICE,
        messages = persistentListOf(FactorSourceStatusMessage.SecurityPrompt.LostFactorSource),
        accounts = persistentListOf(Account.sampleMainnet()),
        personas = persistentListOf(
            Persona.sampleMainnet(),
            Persona.sampleStokenet()
        ),
        hasHiddenEntities = true,
        isEnabled = true
    ),
    FactorSourceCard(
        id = FactorSourceId.Hash.init(
            kind = FactorSourceKind.DEVICE,
            mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
        ),
        name = "XXX phone",
        includeDescription = false,
        lastUsedOn = "Last year",
        kind = FactorSourceKind.DEVICE,
        messages = persistentListOf(),
        accounts = persistentListOf(),
        personas = persistentListOf(),
        hasHiddenEntities = true,
        isEnabled = true
    )
)

@Composable
@Preview
@UsesSampleValues
private fun BiometricsPinPreview() {
    RadixWalletPreviewTheme {
        BiometricsPinContent(
            state = State(
                mainDeviceFactorSource = FactorSourceCard(
                    id = FactorSourceId.Hash.init(
                        kind = FactorSourceKind.DEVICE,
                        mnemonicWithPassphrase = MnemonicWithPassphrase.sample(),
                    ),
                    name = "My cool phone",
                    includeDescription = false,
                    lastUsedOn = "Today",
                    kind = FactorSourceKind.DEVICE,
                    messages = persistentListOf(FactorSourceStatusMessage.NoSecurityIssues),
                    accounts = persistentListOf(Account.sampleMainnet()),
                    personas = persistentListOf(
                        Persona.sampleMainnet(),
                        Persona.sampleStokenet()
                    ),
                    hasHiddenEntities = false,
                    isEnabled = true
                ),
                otherDeviceFactorSources = otherDeviceFactorSources,
            ),
            onBackClick = {},
            onDeviceFactorSourceClick = {},
            onAddBiometricsPinClick = {},
            onChangeMainDeviceFactorSourceClick = {},
            onInfoClick = {}
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun ChangeMainDeviceFactorSourceBottomSheetPreview() {
    RadixWalletPreviewTheme {
        ChangeMainDeviceFactorSourceContent(
            factorSources = otherDeviceFactorSources.map { Selectable(it) }.toImmutableList(),
            isChangingMainDeviceFactorSourceInProgress = false,
            isContinueButtonEnabled = false,
            onDeviceFactorSourceSelect = {},
            onContinueClick = {},
            onDismissClick = {}
        )
    }
}
