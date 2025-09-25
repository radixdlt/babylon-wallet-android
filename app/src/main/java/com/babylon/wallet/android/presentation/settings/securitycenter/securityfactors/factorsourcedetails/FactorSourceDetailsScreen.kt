package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.factorsourcedetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.RenameBottomSheet
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.SyncSheetState
import com.radixdlt.sargon.ArculusCardFactorSource
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.samples.sample

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FactorSourceDetailsScreen(
    viewModel: FactorSourceDetailsViewModel,
    navigateToViewSeedPhrase: (factorSourceId: FactorSourceId.Hash) -> Unit,
    navigateToViewSeedPhraseRestore: () -> Unit,
    toChangeArculusPin: (FactorSourceId) -> Unit,
    toForgotArculusPin: (FactorSourceId) -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is FactorSourceDetailsViewModel.Event.NavigateToSeedPhrase -> {
                    navigateToViewSeedPhrase(event.factorSourceId)
                }

                is FactorSourceDetailsViewModel.Event.NavigateToSeedPhraseRestore -> {
                    navigateToViewSeedPhraseRestore()
                }

                FactorSourceDetailsViewModel.Event.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SyncSheetState(
        sheetState = bottomSheetState,
        isSheetVisible = state.isRenameBottomSheetVisible,
        onSheetClosed = viewModel::onRenameFactorSourceDismissed
    )

    FactorSourceDetailsContent(
        state = state,
        onRenameFactorSourceClick = viewModel::onRenameFactorSourceClick,
        onViewSeedPhraseClick = viewModel::onViewSeedPhraseClick,
        onChangeArculusPinClick = { toChangeArculusPin(requireNotNull(state.factorSource?.id)) },
        onForgotArculusPinClick = { toForgotArculusPin(requireNotNull(state.factorSource?.id)) },
        onMessageShown = viewModel::onMessageShown,
        onSpotCheckClick = viewModel::onSpotCheckClick,
        onBackClick = onBackClick
    )

    if (state.isRenameBottomSheetVisible) {
        RenameBottomSheet(
            sheetState = bottomSheetState,
            renameInput = state.renameFactorSourceInput,
            titleRes = R.string.renameLabel_factorSource_title,
            subtitleRes = R.string.renameLabel_factorSource_subtitle,
            errorValidationMessageRes = R.string.renameLabel_factorSource_empty,
            errorTooLongNameMessageRes = R.string.renameLabel_factorSource_tooLong,
            onNameChange = viewModel::onRenameFactorSourceChanged,
            onUpdateNameClick = viewModel::onRenameFactorSourceUpdateClick,
            onDismiss = viewModel::onRenameFactorSourceDismissed,
        )
    }
}

@Composable
private fun FactorSourceDetailsContent(
    modifier: Modifier = Modifier,
    state: FactorSourceDetailsViewModel.State,
    onRenameFactorSourceClick: () -> Unit,
    onViewSeedPhraseClick: () -> Unit,
    onChangeArculusPinClick: () -> Unit,
    onForgotArculusPinClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER")
    onSpotCheckClick: () -> Unit,
    onMessageShown: () -> Unit,
    onBackClick: () -> Unit,
) {
    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    Scaffold(
        modifier = modifier,
        containerColor = RadixTheme.colors.backgroundSecondary,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = state.factorSourceName,
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )
                HorizontalDivider(color = RadixTheme.colors.divider)
            }
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding)
        ) {
            Text(
                text = "Manage Factor",
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.textSecondary,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )

            DefaultSettingsItem(
                title = state.factorSourceName,
                subtitle = stringResource(id = R.string.factorSources_detail_rename),
                leadingIconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_account_label,
                onClick = onRenameFactorSourceClick
            )

            HorizontalDivider(color = RadixTheme.colors.divider)

            state.factorSourceKind.AdditionalSettingsItems(
                state = state,
                onViewSeedPhraseClick = onViewSeedPhraseClick,
                onChangeArculusPinClick = onChangeArculusPinClick,
                onForgotArculusPinClick = onForgotArculusPinClick
            )

//            Text(
//                text = "Test",
//                style = RadixTheme.typography.body1Header,
//                color = RadixTheme.colors.gray2,
//                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
//            )
//
//            DefaultSettingsItem(
//                title = stringResource(id = R.string.factorSources_detail_spotCheck),
//                subtitle = stringResource(id = R.string.factorSources_detail_testCanUse),
//                leadingIconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_check_circle_outline,
//                onClick = onSpotCheckClick
//            )
//            HorizontalDivider(color = RadixTheme.colors.gray4)
        }
    }
}

@Composable
private fun FactorSourceKind.AdditionalSettingsItems(
    state: FactorSourceDetailsViewModel.State,
    onViewSeedPhraseClick: () -> Unit,
    onChangeArculusPinClick: () -> Unit,
    onForgotArculusPinClick: () -> Unit
) {
    when (this) {
        FactorSourceKind.DEVICE -> {
            DefaultSettingsItem(
                title = if (state.isDeviceFactorSourceMnemonicNotAvailable) {
                    stringResource(id = R.string.factorSources_detail_seedPhraseLost)
                } else {
                    stringResource(id = R.string.factorSources_detail_viewSeedPhrase)
                },
                subtitle = if (state.isDeviceFactorSourceMnemonicNotAvailable) {
                    stringResource(id = R.string.factorSources_detail_enterSeedPhrase)
                } else {
                    stringResource(id = R.string.factorSources_detail_writeSeedPhrase)
                },
                leadingIconRes = DSR.ic_show,
                isErrorText = state.isDeviceFactorSourceMnemonicNotAvailable,
                onClick = onViewSeedPhraseClick
            )
        }

        FactorSourceKind.ARCULUS_CARD -> {
            DefaultSettingsItem(
                title = stringResource(id = R.string.factorSources_detail_changePin),
                leadingIconRes = DSR.ic_account_label,
                onClick = onChangeArculusPinClick
            )
            HorizontalDivider(
                modifier = Modifier
                    .background(color = RadixTheme.colors.card)
                    .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                color = RadixTheme.colors.divider
            )
            DefaultSettingsItem(
                title = stringResource(id = R.string.factorSources_detail_forgotPin),
                leadingIconRes = DSR.ic_account_label,
                onClick = onForgotArculusPinClick
            )
        }

        else -> {}
    }
}

@Preview
@Composable
@UsesSampleValues
private fun FactorSourceDetailsPreview(
    @PreviewParameter(FactorSourceDetailsPreviewProvider::class) state: FactorSourceDetailsViewModel.State
) {
    RadixWalletPreviewTheme {
        FactorSourceDetailsContent(
            state = state,
            onRenameFactorSourceClick = {},
            onViewSeedPhraseClick = {},
            onChangeArculusPinClick = {},
            onForgotArculusPinClick = {},
            onMessageShown = {},
            onSpotCheckClick = {},
            onBackClick = {}
        )
    }
}

@UsesSampleValues
class FactorSourceDetailsPreviewProvider : PreviewParameterProvider<FactorSourceDetailsViewModel.State> {

    override val values: Sequence<FactorSourceDetailsViewModel.State>
        get() = sequenceOf(
            FactorSourceDetailsViewModel.State(
                factorSource = DeviceFactorSource.sample().asGeneral(),
                uiMessage = null
            ),
            FactorSourceDetailsViewModel.State(
                factorSource = LedgerHardwareWalletFactorSource.sample().asGeneral(),
                uiMessage = null
            ),
            FactorSourceDetailsViewModel.State(
                factorSource = ArculusCardFactorSource.sample().asGeneral(),
                uiMessage = null
            )
        )
}
