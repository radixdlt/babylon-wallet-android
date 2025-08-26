package com.babylon.wallet.android.presentation.selectfactorsource

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.SelectableSingleChoiceFactorSourceCard
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.FactorSourceCategoryHeaderView
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.ArculusCardFactorSource
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.OffDeviceMnemonicFactorSource
import com.radixdlt.sargon.PasswordFactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SelectFactorSourceScreen(
    modifier: Modifier = Modifier,
    viewModel: SelectFactorSourceViewModel,
    onDismiss: () -> Unit,
    onComplete: (FactorSourceId) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SelectFactorSourceContent(
        modifier = modifier,
        state = state,
        onBackClick = viewModel::onBackClick,
        onSelectFactorSource = viewModel::onSelectFactorSource,
        onContinueClick = viewModel::onContinueClick,
        onAddFactorSourceClick = viewModel::onAddFactorSourceClick
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                SelectFactorSourceViewModel.Event.Dismiss -> onDismiss()
                is SelectFactorSourceViewModel.Event.Complete -> onComplete(event.factorSourceId)
            }
        }
    }
}

@Composable
private fun SelectFactorSourceContent(
    modifier: Modifier = Modifier,
    state: SelectFactorSourceViewModel.State,
    onBackClick: () -> Unit,
    onSelectFactorSource: (FactorSourceCard) -> Unit,
    onContinueClick: () -> Unit,
    onAddFactorSourceClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.empty),
                windowInsets = WindowInsets.statusBarsAndBanner,
                onBackClick = onBackClick,
                containerColor = RadixTheme.colors.backgroundSecondary
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                text = stringResource(id = R.string.common_continue),
                enabled = state.isButtonEnabled
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
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
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
                    text = stringResource(id = R.string.addFactorSource_selectSecurityFactor),
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
                    text = state.context.description(),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )
            }

            items(state.items) {
                when (val item = it) {
                    is SelectFactorSourceViewModel.State.UiItem.CategoryHeader -> FactorSourceCategoryHeaderView(
                        modifier = Modifier.padding(top = RadixTheme.dimensions.paddingXLarge),
                        kind = item.kind
                    )

                    is SelectFactorSourceViewModel.State.UiItem.Factor -> SelectableSingleChoiceFactorSourceCard(
                        modifier = Modifier.padding(top = RadixTheme.dimensions.paddingMedium),
                        item = item.selectable,
                        onSelect = onSelectFactorSource
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                RadixSecondaryButton(
                    text = stringResource(id = R.string.addFactorSource_addNewSecurityFactor),
                    onClick = onAddFactorSourceClick
                )
            }
        }
    }
}

@Composable
private fun SelectFactorSourceInput.Context.description() = when (this) {
    SelectFactorSourceInput.Context.CreateAccount -> stringResource(id = R.string.addFactorSource_chooseSecurityFactorToCreateAccount)
    SelectFactorSourceInput.Context.CreatePersona -> stringResource(id = R.string.addFactorSource_chooseSecurityFactorToCreatePersona)
    is SelectFactorSourceInput.Context.AccountRecovery -> stringResource(id = R.string.addFactorSource_chooseSecurityFactorToRecover)
}

@Composable
@Preview
private fun SelectFactorSourceLightPreview(
    @PreviewParameter(SelectFactorSourcePreviewProvider::class) state: SelectFactorSourceViewModel.State
) {
    RadixWalletPreviewTheme {
        SelectFactorSourceContent(
            state = state,
            onBackClick = {},
            onSelectFactorSource = {},
            onContinueClick = {},
            onAddFactorSourceClick = {}
        )
    }
}

@Composable
@Preview
private fun SelectFactorSourceDarkPreview(
    @PreviewParameter(SelectFactorSourcePreviewProvider::class) state: SelectFactorSourceViewModel.State
) {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        SelectFactorSourceContent(
            state = state,
            onBackClick = {},
            onSelectFactorSource = {},
            onContinueClick = {},
            onAddFactorSourceClick = {}
        )
    }
}

@UsesSampleValues
class SelectFactorSourcePreviewProvider : PreviewParameterProvider<SelectFactorSourceViewModel.State> {

    val items = listOf(
        SelectFactorSourceViewModel.State.UiItem.CategoryHeader(FactorSourceKind.DEVICE),
        SelectFactorSourceViewModel.State.UiItem.Factor(
            Selectable(
                data = DeviceFactorSource.sample().asGeneral().toFactorSourceCard(
                    accounts = persistentListOf(
                        Account.sampleMainnet()
                    )
                ),
                selected = false
            )
        ),
        SelectFactorSourceViewModel.State.UiItem.CategoryHeader(FactorSourceKind.ARCULUS_CARD),
        SelectFactorSourceViewModel.State.UiItem.Factor(
            Selectable(
                data = ArculusCardFactorSource.sample().asGeneral().toFactorSourceCard(),
                selected = false
            )
        ),
        SelectFactorSourceViewModel.State.UiItem.CategoryHeader(FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET),
        SelectFactorSourceViewModel.State.UiItem.Factor(
            Selectable(
                data = LedgerHardwareWalletFactorSource.sample().asGeneral().toFactorSourceCard(),
                selected = false
            )
        ),
        SelectFactorSourceViewModel.State.UiItem.Factor(
            Selectable(
                data = LedgerHardwareWalletFactorSource.sample.other().asGeneral().toFactorSourceCard(),
                selected = false
            )
        ),
        SelectFactorSourceViewModel.State.UiItem.CategoryHeader(FactorSourceKind.PASSWORD),
        SelectFactorSourceViewModel.State.UiItem.Factor(
            Selectable(
                data = PasswordFactorSource.sample().asGeneral().toFactorSourceCard(),
                selected = true
            )
        ),
        SelectFactorSourceViewModel.State.UiItem.CategoryHeader(FactorSourceKind.OFF_DEVICE_MNEMONIC),
        SelectFactorSourceViewModel.State.UiItem.Factor(
            Selectable(
                data = OffDeviceMnemonicFactorSource.sample().asGeneral().toFactorSourceCard(),
                selected = false
            )
        )
    )

    override val values: Sequence<SelectFactorSourceViewModel.State>
        get() = sequenceOf(
            SelectFactorSourceViewModel.State(
                isLoading = false,
                context = SelectFactorSourceInput.Context.CreateAccount,
                items = items
            ),
            SelectFactorSourceViewModel.State(
                isLoading = true,
                context = SelectFactorSourceInput.Context.CreateAccount
            )
        )
}
