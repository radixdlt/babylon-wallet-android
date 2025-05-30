@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package com.babylon.wallet.android.presentation.onboarding.restore.withoutbackup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.utils.formattedSpans
import kotlinx.coroutines.launch

@Composable
fun RestoreWithoutBackupScreen(
    viewModel: RestoreWithoutBackupViewModel,
    onBack: () -> Unit,
    onRestoreConfirmed: () -> Unit,
    onNewUserConfirmClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    if (state.dialogPrompt != RestoreWithoutBackupViewModel.State.PromptState.None) {
        BasicPromptAlertDialog(
            finish = { accepted ->
                viewModel.onDismissPrompt()

                if (accepted) {
                    onNewUserConfirmClick()
                }
            },
            titleText = stringResource(id = R.string.recoverWalletWithoutProfile_start_useNewWalletAlertTitle),
            messageText = when (state.dialogPrompt) {
                RestoreWithoutBackupViewModel.State.PromptState.Olympia -> {
                    stringResource(id = R.string.recoverWalletWithoutProfile_start_useNewWalletAlertMessageOlympia)
                }
                RestoreWithoutBackupViewModel.State.PromptState.Ledger -> {
                    stringResource(id = R.string.recoverWalletWithoutProfile_start_useNewWalletAlertMessageHardware)
                }
                else -> stringResource(id = R.string.empty)
            },
            confirmText = stringResource(id = R.string.common_continue),
        )
    }
    RestoreWithoutBackupContent(
        onBackClick = viewModel::onBackClick,
        onShowLedgerPrompt = viewModel::onShowLedgerPrompt,
        onShowOlympiaPrompt = viewModel::onShowOlympiaPrompt,
        onConfirmRecoverWithMainSeedPhrase = onRestoreConfirmed
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                RestoreWithoutBackupViewModel.Event.OnDismiss -> onBack()
            }
        }
    }
}

@Composable
private fun RestoreWithoutBackupContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onShowLedgerPrompt: () -> Unit,
    onConfirmRecoverWithMainSeedPhrase: () -> Unit,
    onShowOlympiaPrompt: () -> Unit,
) {
    val pages = remember {
        Pages.entries.toTypedArray()
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val backCallback = {
        if (pagerState.currentPage != 0) {
            scope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        } else {
            onBackClick()
        }
    }
    BackHandler {
        backCallback()
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                windowInsets = WindowInsets.statusBarsAndBanner,
                title = "",
                onBackClick = {
                    backCallback()
                }
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = stringResource(id = R.string.recoverWalletWithoutProfile_start_headerTitle),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            HorizontalPager(
                modifier = Modifier.weight(1f),
                state = pagerState,
                userScrollEnabled = false
            ) { page ->
                when (pages[page]) {
                    Pages.SelectRecoveryOption -> {
                        SelectRecoveryOptionSection(
                            onRecoverWithMainSeedPhraseClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            onShowLedgerPrompt = onShowLedgerPrompt,
                            onShowOlympiaPrompt = onShowOlympiaPrompt,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                                .verticalScroll(rememberScrollState())
                        )
                    }

                    Pages.BDFSRecoveryInfo -> {
                        BDFSRecoveryInfoSection(
                            Modifier.fillMaxSize(),
                            onConfirmRecoverWithMainSeedPhrase = onConfirmRecoverWithMainSeedPhrase
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectRecoveryOptionSection(
    onRecoverWithMainSeedPhraseClick: () -> Unit,
    onShowLedgerPrompt: () -> Unit,
    modifier: Modifier = Modifier,
    onShowOlympiaPrompt: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.recoverWalletWithoutProfile_start_headerSubtitle),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text
        )
        HorizontalDivider(color = RadixTheme.colors.divider)
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingSmall
                ),
            text = stringResource(id = R.string.recoverWalletWithoutProfile_start_babylonSectionTitle).formattedSpans(
                RadixTheme.typography.body1Header.toSpanStyle()
            ),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.text
        )
        RadixSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Recover Control with Main Seed Phrase",
            onClick = onRecoverWithMainSeedPhraseClick
        )
        HorizontalDivider(color = RadixTheme.colors.divider)
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingSmall
                ),
            text = stringResource(id = R.string.recoverWalletWithoutProfile_start_hardwareSectionTitle).formattedSpans(
                RadixTheme.typography.body1Header.toSpanStyle()
            ),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.text
        )
        RadixSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.recoverWalletWithoutProfile_start_hardwareSectionButton),
            onClick = onShowLedgerPrompt
        )
        HorizontalDivider(color = RadixTheme.colors.divider)
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingSmall
                ),
            text = stringResource(id = R.string.recoverWalletWithoutProfile_start_olympiaSectionTitle).formattedSpans(
                RadixTheme.typography.body1Header.toSpanStyle()
            ),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.text
        )
        RadixSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.recoverWalletWithoutProfile_start_olympiaSectionButton),
            onClick = onShowOlympiaPrompt
        )
    }
}

@Composable
private fun BDFSRecoveryInfoSection(
    modifier: Modifier = Modifier,
    onConfirmRecoverWithMainSeedPhrase: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.recoverWalletWithoutProfile_info_headerSubtitle).formattedSpans(
                RadixTheme.typography.body1Header.toSpanStyle()
            ),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text
        )
        Spacer(modifier = Modifier.weight(1f))
        RadixBottomBar(onClick = onConfirmRecoverWithMainSeedPhrase, text = stringResource(id = R.string.common_continue))
    }
}

private enum class Pages {
    SelectRecoveryOption, BDFSRecoveryInfo
}

@Preview
@Composable
fun RestoreWithoutBackupPreview() {
    RadixWalletTheme {
        RestoreWithoutBackupContent(
            onBackClick = {},
            onShowLedgerPrompt = {},
            onConfirmRecoverWithMainSeedPhrase = {},
            onShowOlympiaPrompt = {}
        )
    }
}
