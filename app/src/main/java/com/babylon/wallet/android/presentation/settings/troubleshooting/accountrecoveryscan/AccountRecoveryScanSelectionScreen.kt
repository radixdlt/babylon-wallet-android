package com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.FactorSourceId

@Composable
fun AccountRecoveryScanSelectionScreen(
    viewModel: AccountRecoveryScanSelectionViewModel,
    onBack: () -> Unit,
    onFactorSourceSelected: (FactorSourceId.Hash, Boolean) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AccountRecoveryScanSelectionContent(
        onBackClick = viewModel::onBackClick,
        onRecoverBabylonAccounts = viewModel::onRecoverBabylonAccounts,
        onRecoverOlympiaAccounts = viewModel::onRecoverOlympiaAccounts,
        isMainnet = state.isMainnet
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                AccountRecoveryScanSelectionViewModel.Event.OnDismiss -> {
                    onBack()
                }
                is AccountRecoveryScanSelectionViewModel.Event.FactorSourceSelected -> {
                    onFactorSourceSelected(it.factorSourceId, it.isForOlympia)
                }
            }
        }
    }
}

@Composable
private fun AccountRecoveryScanSelectionContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onRecoverBabylonAccounts: () -> Unit,
    onRecoverOlympiaAccounts: () -> Unit,
    isMainnet: Boolean,
) {
    val backCallback = {
        onBackClick()
    }
    BackHandler {
        backCallback()
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                windowInsets = WindowInsets.statusBarsAndBanner,
                title = stringResource(id = R.string.empty),
                onBackClick = {
                    backCallback()
                },
                backIconType = BackIconType.Close
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(padding)
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)

        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.accountSecuritySettings_accountRecoveryScan_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(id = R.string.accountRecoveryScan_subtitle),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge),
                color = RadixTheme.colors.divider
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.accountRecoveryScan_babylonSection_title),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.header,
                color = RadixTheme.colors.text
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = RadixTheme.dimensions.paddingDefault
                    ),
                text = stringResource(id = R.string.accountRecoveryScan_babylonSection_subtitle).formattedSpans(
                    RadixTheme.typography.body1Header.toSpanStyle()
                ),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )
            RadixSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.recoverWalletWithoutProfile_recoverBabylonAccounts),
                onClick = onRecoverBabylonAccounts
            )
            if (isMainnet) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingLarge),
                    color = RadixTheme.colors.divider
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                    text = stringResource(id = R.string.accountRecoveryScan_olympiaSection_title),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.header,
                    color = RadixTheme.colors.text
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.accountRecoveryScan_olympiaSection_subtitle).formattedSpans(
                        RadixTheme.typography.body1Header.toSpanStyle()
                    ),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )
                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.recoverWalletWithoutProfile_recoverOlympiaAccounts),
                    onClick = onRecoverOlympiaAccounts
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = RadixTheme.dimensions.paddingSmall,
                            horizontal = RadixTheme.dimensions.paddingDefault
                        ),
                    text = stringResource(id = R.string.accountRecoveryScan_olympiaSection_footnote)
                        .formattedSpans(
                            RadixTheme.typography.body1HighImportance.toSpanStyle()
                        ),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.text
                )
            }
        }
    }
}

@Preview
@Composable
fun AccountRecoveryScanSelectionPreview() {
    RadixWalletTheme {
        AccountRecoveryScanSelectionContent(
            onBackClick = {},
            onRecoverBabylonAccounts = {},
            onRecoverOlympiaAccounts = {},
            isMainnet = true
        )
    }
}
