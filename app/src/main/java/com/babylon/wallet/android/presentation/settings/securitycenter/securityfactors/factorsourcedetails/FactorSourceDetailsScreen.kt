package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.factorsourcedetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DefaultSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.RenameBottomSheet
import com.babylon.wallet.android.presentation.ui.composables.SwitchSettingsItem
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind

@Composable
fun FactorSourceDetailsScreen(
    viewModel: FactorSourceDetailsViewModel,
    navigateToViewSeedPhrase: (factorSourceId: FactorSourceId.Hash) -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is FactorSourceDetailsViewModel.Event.NavigateToSeedPhrase -> navigateToViewSeedPhrase(event.factorSourceId)
                FactorSourceDetailsViewModel.Event.NavigateBack -> onBackClick()
            }
        }
    }

    FactorSourceDetailsContent(
        factorSourceName = state.factorSourceName,
        factorSourceKind = state.factorSourceKind,
        isFactorSourceNameUpdated = state.isFactorSourceNameUpdated,
        isArculusPinEnabled = state.isArculusPinEnabled,
        onRenameFactorSourceClick = viewModel::onRenameFactorSourceClick,
        onViewSeedPhraseClick = viewModel::onViewSeedPhraseClick,
        onArculusPinCheckedChange = viewModel::onArculusPinCheckedChange,
        onChangeArculusPinClick = viewModel::onChangeArculusPinClick,
        onSnackbarMessageShown = viewModel::onSnackbarMessageShown,
        onBackClick = onBackClick
    )

    if (state.isRenameBottomSheetVisible) {
        RenameBottomSheet(
            renameInput = state.renameFactorSourceInput,
            titleRes = R.string.renameLabel_factorSource_title,
            subtitleRes = R.string.renameLabel_factorSource_subtitle,
            errorValidationMessageRes = R.string.renameLabel_factorSource_empty,
            errorTooLongNameMessageRes = R.string.renameLabel_factorSource_tooLong,
            onNameChange = viewModel::onRenameFactorSourceChanged,
            onUpdateNameClick = viewModel::onRenameFactorSourceUpdateClick,
            onDismiss = viewModel::onRenameFactorSourceDismissed
        )
    }
}

@Composable
private fun FactorSourceDetailsContent(
    modifier: Modifier = Modifier,
    factorSourceName: String,
    factorSourceKind: FactorSourceKind,
    isFactorSourceNameUpdated: Boolean,
    isArculusPinEnabled: Boolean,
    onRenameFactorSourceClick: () -> Unit,
    onViewSeedPhraseClick: () -> Unit,
    onArculusPinCheckedChange: (Boolean) -> Unit,
    onChangeArculusPinClick: () -> Unit,
    onSnackbarMessageShown: () -> Unit,
    onBackClick: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = stringResource(R.string.linkedConnectors_renameConnector_successHud)
    LaunchedEffect(isFactorSourceNameUpdated) {
        if (isFactorSourceNameUpdated) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
            onSnackbarMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = RadixTheme.colors.gray5,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = factorSourceName,
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )
                HorizontalDivider(color = RadixTheme.colors.gray4)
            }
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackbarHostState
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding)
        ) {
            Text(
                text = "Manage Factor",
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray2,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )

            DefaultSettingsItem(
                title = factorSourceName,
                subtitle = stringResource(id = R.string.factorSources_detail_rename),
                leadingIconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_account_label,
                onClick = onRenameFactorSourceClick
            )

            factorSourceKind.AdditionalSettingsItems(
                isArculusPinEnabled = isArculusPinEnabled,
                onViewSeedPhraseClick = onViewSeedPhraseClick,
                onArculusPinCheckedChange = onArculusPinCheckedChange,
                onChangeArculusPinClick = onChangeArculusPinClick
            )

            HorizontalDivider(color = RadixTheme.colors.gray4)

            Text(
                text = "Test",
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray2,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )

            DefaultSettingsItem(
                title = stringResource(id = R.string.factorSources_detail_spotCheck),
                subtitle = stringResource(id = R.string.factorSources_detail_testCanUse),
                leadingIconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_check_circle_outline,
                onClick = {}
            )
            HorizontalDivider(color = RadixTheme.colors.gray4)
        }
    }
}

@Composable
private fun FactorSourceKind.AdditionalSettingsItems(
    isArculusPinEnabled: Boolean,
    onViewSeedPhraseClick: () -> Unit,
    onArculusPinCheckedChange: (Boolean) -> Unit,
    onChangeArculusPinClick: () -> Unit
) {
    when (this) {
        FactorSourceKind.DEVICE -> {
            HorizontalDivider(
                modifier = Modifier
                    .background(color = RadixTheme.colors.defaultBackground)
                    .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                color = RadixTheme.colors.gray4
            )
            DefaultSettingsItem(
                title = stringResource(id = R.string.factorSources_detail_viewSeedPhrase),
                subtitle = stringResource(id = R.string.factorSources_detail_writeSeedPhrase),
                leadingIconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_show, // TODO update icon
                onClick = onViewSeedPhraseClick
            )
//            DefaultSettingsItem(
//                isErrorText = true,
//                title = stringResource(id = R.string.factorSources_detail_seedPhraseLost),
//                subtitle = stringResource(id = R.string.factorSources_detail_enterSeedPhrase),
//                leadingIconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_show, // TODO update icon
//                onClick = onViewSeedPhraseClick
//            )
        }
        FactorSourceKind.ARCULUS_CARD -> {
            HorizontalDivider(
                modifier = Modifier
                    .background(color = RadixTheme.colors.defaultBackground)
                    .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                color = RadixTheme.colors.gray4
            )
            SwitchSettingsItem(
                modifier = Modifier
                    .background(RadixTheme.colors.defaultBackground)
                    .fillMaxWidth()
                    .padding(all = RadixTheme.dimensions.paddingDefault),
                titleRes = R.string.factorSources_detail_changePin,
                iconResource = com.babylon.wallet.android.designsystem.R.drawable.ic_account_label,
                checked = isArculusPinEnabled,
                onCheckedChange = onArculusPinCheckedChange
            )
            HorizontalDivider(
                modifier = Modifier
                    .background(color = RadixTheme.colors.defaultBackground)
                    .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
                color = RadixTheme.colors.gray4
            )
            DefaultSettingsItem(
                title = stringResource(id = R.string.factorSources_detail_changePin),
                leadingIconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_account_label, // TODO update icon
                onClick = onChangeArculusPinClick
            )
        }
        else -> {}
    }
}

@Preview
@Composable
private fun DeviceFactorSourceDetailsPreview() {
    RadixWalletPreviewTheme {
        FactorSourceDetailsContent(
            factorSourceName = "My factor source",
            factorSourceKind = FactorSourceKind.DEVICE,
            isFactorSourceNameUpdated = false,
            isArculusPinEnabled = true,
            onRenameFactorSourceClick = {},
            onViewSeedPhraseClick = {},
            onArculusPinCheckedChange = {},
            onChangeArculusPinClick = {},
            onSnackbarMessageShown = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun LedgerFactorSourceDetailsPreview() {
    RadixWalletPreviewTheme {
        FactorSourceDetailsContent(
            factorSourceName = "My factor source",
            factorSourceKind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
            isFactorSourceNameUpdated = false,
            isArculusPinEnabled = true,
            onRenameFactorSourceClick = {},
            onViewSeedPhraseClick = {},
            onArculusPinCheckedChange = {},
            onChangeArculusPinClick = {},
            onSnackbarMessageShown = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun ArculusFactorSourceDetailsPreview() {
    RadixWalletPreviewTheme {
        FactorSourceDetailsContent(
            factorSourceName = "My factor source",
            factorSourceKind = FactorSourceKind.ARCULUS_CARD,
            isFactorSourceNameUpdated = false,
            isArculusPinEnabled = true,
            onRenameFactorSourceClick = {},
            onViewSeedPhraseClick = {},
            onArculusPinCheckedChange = {},
            onChangeArculusPinClick = {},
            onSnackbarMessageShown = {},
            onBackClick = {}
        )
    }
}
