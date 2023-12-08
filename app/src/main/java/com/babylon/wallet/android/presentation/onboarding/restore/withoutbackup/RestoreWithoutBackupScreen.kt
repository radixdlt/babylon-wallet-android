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
import androidx.compose.foundation.layout.statusBars
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
import com.babylon.wallet.android.presentation.ui.composables.BottomPrimaryButton
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
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
                if (accepted) {
                    onNewUserConfirmClick()
                } else {
                    viewModel.onDismissPrompt()
                }
            },
            title = "No Babylon Seed Phrase", // TODO crowdin
            text = when (state.dialogPrompt) {
                RestoreWithoutBackupViewModel.State.PromptState.Olympia -> {
                    "Tap “I’m a New Wallet User”. After completing wallet creation, in Settings you can perform an “account recovery scan” using your Olympia device" // TODO crowdin,
                }

                RestoreWithoutBackupViewModel.State.PromptState.Ledger -> {
                    "Tap “I’m a New Wallet User”. After completing wallet creation, in Settings you can perform an “account recovery scan” using your Ledger seed phrase" // TODO crowdin,
                }

                else -> ""
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
    val pages = Pages.values()
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
                windowInsets = WindowInsets.statusBars,
                title = "",
                onBackClick = {
                    backCallback()
                }
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
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
                text = "Recover Control Without Backup", // TODO crowdin
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.title
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
            text = "If you have no wallet backup in the cloud or as an exported backup file, you still have " +
                    "other restore options.", // TODO crowdin
            style = RadixTheme.typography.body1Regular
        )
        HorizontalDivider(color = RadixTheme.colors.gray4)
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingSmall
                ),
            text = "I have my main “Babylon” 24-word seed phrase.", // TODO crowdin
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )
        RadixSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Recover Control with Main Seed Phrase",
            onClick = onRecoverWithMainSeedPhraseClick
        )
        HorizontalDivider(color = RadixTheme.colors.gray4)
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingSmall
                ),
            text = "I only want to restore Ledger hardware wallet Accounts.", // TODO crowdin
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )
        RadixSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Ledger-only restore",
            onClick = onShowLedgerPrompt
        )
        HorizontalDivider(color = RadixTheme.colors.gray4)
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingSmall
                ),
            text = "I only have Accounts created on the Radix Olympia network.", // TODO crowdin
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray1
        )
        RadixSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Olympia-only Restore",
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
            text = stringResource(id = R.string.settings_bdfsInfo).formattedSpans(RadixTheme.typography.body1Header.toSpanStyle()),
            style = RadixTheme.typography.body1Regular
        )
        Spacer(modifier = Modifier.weight(1f))
        BottomPrimaryButton(onClick = onConfirmRecoverWithMainSeedPhrase, text = stringResource(id = R.string.common_continue))
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
