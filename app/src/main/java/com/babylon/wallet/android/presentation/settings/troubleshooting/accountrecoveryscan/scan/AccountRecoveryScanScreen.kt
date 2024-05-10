package com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.scan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountSelectionCard
import com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.scan.AccountRecoveryScanViewModel.Companion.ACCOUNTS_PER_SCAN
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.Constants
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSource
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.launch

@Composable
fun AccountRecoveryScanScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: AccountRecoveryScanViewModel,
    onRecoveryComplete: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    if (state.isNoMnemonicErrorVisible) {
        BasicPromptAlertDialog(
            finish = {
                viewModel.dismissNoMnemonicError()
            },
            titleText = stringResource(id = R.string.transactionReview_noMnemonicError_title),
            messageText = stringResource(id = R.string.transactionReview_noMnemonicError_text),
            dismissText = null
        )
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is Event.RecoverComplete -> onRecoveryComplete()
                Event.OnBackClick -> onBackClick()
                Event.CloseScan -> onBackClick()
            }
        }
    }

    AccountRecoveryScanContent(
        modifier = modifier,
        onBackClick = viewModel::onBackClick,
        onScanMoreClick = viewModel::onScanMoreClick,
        state = state,
        onAccountSelected = viewModel::onAccountSelected,
        onContinueClick = {
            viewModel.onContinueClick { context.biometricAuthenticateSuspend() }
        },
        isScanningNetwork = state.isScanningNetwork,
        onMessageShown = viewModel::onMessageShown
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountRecoveryScanContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onScanMoreClick: () -> Unit,
    state: AccountRecoveryScanViewModel.State,
    onAccountSelected: (Selectable<Account>) -> Unit,
    onContinueClick: () -> Unit,
    isScanningNetwork: Boolean,
    onMessageShown: () -> Unit
) {
    val pages = ScanCompletePages.entries.toTypedArray()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { pages.size })

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )
    val backHandler = {
        if (pagerState.currentPage == ScanCompletePages.InactiveAccounts.ordinal) {
            scope.launch {
                pagerState.animateScrollToPage(ScanCompletePages.ActiveAccounts.ordinal)
            }
        } else {
            onBackClick()
        }
    }
    BackHandler(onBack = { backHandler() })

    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = {
                    backHandler()
                },
                windowInsets = WindowInsets.statusBars
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.defaultBackground,
        bottomBar = {
            if (state.contentState == AccountRecoveryScanViewModel.State.ContentState.ScanComplete) {
                val activeAccountsShown = pagerState.currentPage == ScanCompletePages.ActiveAccounts.ordinal
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (activeAccountsShown) {
                        RadixTextButton(
                            text = stringResource(
                                id = R.string.accountRecoveryScan_scanComplete_scanNextBatchButton,
                                ACCOUNTS_PER_SCAN
                            ),
                            onClick = onScanMoreClick
                        )
                    }
                    RadixPrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.accountRecoveryScan_scanComplete_continueButton),
                        onClick = {
                            if (pagerState.currentPage == ScanCompletePages.ActiveAccounts.ordinal) {
                                if (state.inactiveAccounts.isNotEmpty()) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(ScanCompletePages.InactiveAccounts.ordinal)
                                    }
                                } else {
                                    onContinueClick()
                                }
                            } else {
                                onContinueClick()
                            }
                        },
                        enabled = isScanningNetwork.not(),
                        isLoading = isScanningNetwork
                    )
                }
            }
        }
    ) { padding ->
        when (state.contentState) {
            AccountRecoveryScanViewModel.State.ContentState.ScanInProgress -> {
                AnimatedVisibility(visible = true) {
                    ScanInProgressContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        isLedgerDevice = state.recoveryFactorSource?.let {
                            it is FactorSource.Ledger
                        } ?: false,
                        isOlympiaSeedPhrase = state.isOlympiaSeedPhrase,
                        isScanningNetwork = state.isScanningNetwork
                    )
                }
            }

            AccountRecoveryScanViewModel.State.ContentState.ScanComplete -> {
                AnimatedVisibility(visible = true) {
                    ScanCompleteContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        pagerState = pagerState,
                        activeAccounts = state.activeAccounts,
                        inactiveAccounts = state.inactiveAccounts,
                        allScannedAccountsSize = state.recoveredAccounts.size,
                        onAccountSelected = onAccountSelected
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScanCompleteContent(
    modifier: Modifier,
    pagerState: PagerState,
    activeAccounts: PersistentList<Account>,
    inactiveAccounts: PersistentList<Selectable<Account>>,
    allScannedAccountsSize: Int,
    onAccountSelected: (Selectable<Account>) -> Unit
) {
    val pages = ScanCompletePages.entries.toTypedArray()
    HorizontalPager(state = pagerState, userScrollEnabled = false) { page ->
        when (pages[page]) {
            ScanCompletePages.ActiveAccounts -> {
                ActiveAccountsPage(
                    modifier = modifier,
                    activeAccounts = activeAccounts,
                    allScannedAccountsSize = allScannedAccountsSize
                )
            }

            ScanCompletePages.InactiveAccounts -> {
                InactiveAccountsPage(
                    modifier = modifier,
                    inactiveAccounts = inactiveAccounts,
                    onAccountSelected = onAccountSelected
                )
            }
        }
    }
}

@Composable
private fun ActiveAccountsPage(
    modifier: Modifier,
    activeAccounts: PersistentList<Account>,
    allScannedAccountsSize: Int
) {
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault)
    ) {
        item {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = stringResource(id = R.string.accountRecoveryScan_scanComplete_headerTitle),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = stringResource(
                    id = R.string.accountRecoveryScan_scanComplete_headerSubtitle,
                    allScannedAccountsSize
                ).formattedSpans(
                    RadixTheme.typography.body1Header.toSpanStyle()
                ),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }

        if (activeAccounts.isEmpty()) {
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                        .background(RadixTheme.colors.gray4, RadixTheme.shapes.roundedRectMedium)
                        .padding(RadixTheme.dimensions.paddingXXLarge),
                    text = stringResource(id = R.string.accountRecoveryScan_scanComplete_noAccounts),
                    color = RadixTheme.colors.gray2,
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.secondaryHeader
                )
            }
        } else {
            items(activeAccounts) { account ->
                SimpleAccountCard(modifier = Modifier.fillMaxWidth(), account = account)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            }
        }
    }
}

@Composable
private fun InactiveAccountsPage(
    modifier: Modifier,
    inactiveAccounts: PersistentList<Selectable<Account>>,
    onAccountSelected: (Selectable<Account>) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault)
    ) {
        item {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = stringResource(id = R.string.accountRecoveryScan_selectInactiveAccounts_header_title),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = stringResource(id = R.string.accountRecoveryScan_selectInactiveAccounts_header_subtitle),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }

        items(inactiveAccounts) { account ->
            AccountSelectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .background(
                        brush = account.data.appearanceId.gradient(),
                        shape = RadixTheme.shapes.roundedRectMedium
                    )
                    .clip(RadixTheme.shapes.roundedRectMedium)
                    .throttleClickable {
                        onAccountSelected(account)
                    },
                accountName = Constants.DEFAULT_ACCOUNT_NAME,
                address = account.data.address,
                checked = account.selected,
                isSingleChoice = false,
                radioButtonClicked = {
                    onAccountSelected(account)
                }
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        }
    }
}

@Composable
private fun ScanInProgressContent(
    modifier: Modifier = Modifier,
    isOlympiaSeedPhrase: Boolean,
    isLedgerDevice: Boolean,
    isScanningNetwork: Boolean
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.accountRecoveryScan_inProgress_headerTitle),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        val text = buildAnnotatedString {
            append(stringResource(id = R.string.accountRecoveryScan_inProgress_headerSubtitle))
            append("\n\n")
            append(
                if (isLedgerDevice) {
                    stringResource(id = R.string.accountRecoveryScan_inProgress_factorSourceLedgerHardwareDevice)
                        .formattedSpans(boldStyle = RadixTheme.typography.body1Header.toSpanStyle())
                } else if (isOlympiaSeedPhrase) {
                    stringResource(id = R.string.accountRecoveryScan_inProgress_factorSourceOlympiaSeedPhrase)
                        .formattedSpans(boldStyle = RadixTheme.typography.body1Header.toSpanStyle())
                } else {
                    stringResource(id = R.string.accountRecoveryScan_inProgress_factorSourceBabylonSeedPhrase)
                        .formattedSpans(boldStyle = RadixTheme.typography.body1Header.toSpanStyle())
                }
            )
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = text,
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(64.dp))
        if (isScanningNetwork) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = RadixTheme.colors.gray1
            )
        }
    }
}

internal enum class ScanCompletePages {
    ActiveAccounts, InactiveAccounts
}

@Preview(showBackground = true)
@Composable
fun ScanInProgressContentPreview() {
    RadixWalletTheme {
        ScanInProgressContent(
            isOlympiaSeedPhrase = false,
            isLedgerDevice = false,
            isScanningNetwork = true
        )
    }
}
