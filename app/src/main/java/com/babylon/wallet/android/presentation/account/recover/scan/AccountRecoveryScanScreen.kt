@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package com.babylon.wallet.android.presentation.account.recover.scan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.usecases.RecoverAccountsForFactorSourceUseCase
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountSelectionCard
import com.babylon.wallet.android.presentation.status.signing.FactorSourceInteractionBottomDialog
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.Constants
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.babylon.wallet.android.utils.formattedSpans
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network

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
            title = stringResource(id = R.string.transactionReview_noMnemonicError_title),
            text = stringResource(id = R.string.transactionReview_noMnemonicError_text),
            dismissText = null
        )
    }
    LaunchedEffect(Unit) {
        if (state.recoveryFactorSource is RecoveryFactorSource.VirtualDeviceFactorSource) {
            viewModel.startRecoveryScan()
        } else {
            context.biometricAuthenticate { authenticated ->
                if (authenticated) {
                    viewModel.startScanForExistingFactorSource()
                } else {
                    onBackClick()
                }
            }
        }
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
        onMessageShown = {},
        onScanMoreClick = viewModel::startRecoveryScan,
        sharedState = state,
        onAccountSelected = viewModel::onAccountSelected,
        onContinueClick = {
            viewModel.onContinueClick { context.biometricAuthenticateSuspend() }
        },
        isRestoring = state.isRestoring
    )
    AnimatedVisibility(visible = state.interactionState != null, enter = slideInVertically(), exit = slideOutVertically()) {
        state.interactionState?.let {
            FactorSourceInteractionBottomDialog(
                modifier = Modifier.fillMaxHeight(0.6f),
                onDismissDialogClick = viewModel::onDismissSigningStatusDialog,
                interactionState = it
            )
        }
    }
}

@Composable
private fun AccountRecoveryScanContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onMessageShown: () -> Unit,
    onScanMoreClick: () -> Unit,
    sharedState: AccountRecoveryScanViewModel.State,
    onAccountSelected: (Selectable<Network.Account>) -> Unit,
    onContinueClick: () -> Unit,
    isRestoring: Boolean
) {
    val pages = ScanCompletePages.values()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { pages.size })

    val snackBarHostState = remember { SnackbarHostState() }
    val backHandler = {
        if (pagerState.currentPage == ScanCompletePages.InactiveAccounts.ordinal) {
            scope.launch {
                pagerState.animateScrollToPage(ScanCompletePages.ActiveAccounts.ordinal)
            }
        } else {
            onBackClick()
        }
    }
    BackHandler(onBack = {
        backHandler()
    })
    SnackbarUIMessage(
        message = sharedState.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

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
            if (sharedState.contentState == AccountRecoveryScanViewModel.State.ContentState.ScanComplete) {
                val activeAccountsShown = pagerState.currentPage == ScanCompletePages.ActiveAccounts.ordinal
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (activeAccountsShown) {
                        RadixTextButton(
                            text = stringResource(
                                id = R.string.accountRecoveryScan_scanComplete_scanNextBatchButton,
                                RecoverAccountsForFactorSourceUseCase.accountsPerScanPage
                            ),
                            onClick = onScanMoreClick
                        )
                    }
                    RadixPrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.common_continue),
                        onClick = {
                            if (pagerState.currentPage == ScanCompletePages.ActiveAccounts.ordinal) {
                                if (sharedState.inactiveAccounts.isNotEmpty()) {
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
                        enabled = isRestoring.not(),
                        isLoading = isRestoring
                    )
                }
            }
        }
    ) { padding ->
        when (sharedState.contentState) {
            AccountRecoveryScanViewModel.State.ContentState.ScanInProgress -> {
                AnimatedVisibility(visible = true) {
                    ScanInProgressContent(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
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
                        activeAccounts = sharedState.activeAccounts,
                        inactiveAccounts = sharedState.inactiveAccounts,
                        allScannedAccountsSize = sharedState.recoveredAccounts.size,
                        onAccountSelected = onAccountSelected
                    )
                }
            }
        }
    }
}

@Composable
fun ScanCompleteContent(
    modifier: Modifier,
    pagerState: PagerState,
    activeAccounts: PersistentList<Network.Account>,
    inactiveAccounts: PersistentList<Selectable<Network.Account>>,
    allScannedAccountsSize: Int,
    onAccountSelected: (Selectable<Network.Account>) -> Unit
) {
    val pages = ScanCompletePages.values()
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
                InactiveAccountsPage(modifier = modifier, inactiveAccounts = inactiveAccounts, onAccountSelected = onAccountSelected)
            }
        }
    }
}

@Composable
private fun ActiveAccountsPage(
    modifier: Modifier,
    activeAccounts: PersistentList<Network.Account>,
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
                        .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
                        .background(RadixTheme.colors.gray4, RadixTheme.shapes.roundedRectMedium)
                        .padding(RadixTheme.dimensions.paddingXLarge),
                    text = "No new accounts found.",
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
    inactiveAccounts: PersistentList<Selectable<Network.Account>>,
    onAccountSelected: (Selectable<Network.Account>) -> Unit
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
                text = "Add Inactive Accounts?",
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = "These Accounts were never used, but you may have created them. Check any addresses that you wish to keep:",
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }

        items(inactiveAccounts) { account ->
            val gradientColor = getAccountGradientColorsFor(account.data.appearanceID)
            AccountSelectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .background(
                        brush = Brush.horizontalGradient(gradientColor),
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
private fun ScanInProgressContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = "Scan in Progress",
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        val text = buildAnnotatedString {
            append("Scanning for Accounts that have been included in at least one transaction, using:")
            append("\n\n")
            withStyle(style = RadixTheme.typography.body1Header.toSpanStyle()) {
                append("Babylon Seed Phrase")
            }
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
    }
}

internal enum class ScanCompletePages {
    ActiveAccounts, InactiveAccounts
}

@Preview
@Composable
fun RestoreMnemonicsSeedPhraseContent() {
    RadixWalletTheme {
        val state = AccountRecoveryScanViewModel.State()
        AccountRecoveryScanContent(
            onBackClick = {},
            onMessageShown = {},
            onScanMoreClick = {},
            sharedState = state,
            onAccountSelected = {},
            onContinueClick = {},
            isRestoring = false
        )
    }
}
