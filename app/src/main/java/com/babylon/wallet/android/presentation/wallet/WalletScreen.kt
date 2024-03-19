package com.babylon.wallet.android.presentation.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.assets.TotalFiatBalanceView
import com.babylon.wallet.android.presentation.ui.composables.assets.TotalFiatBalanceViewToggle
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.Constants.RADIX_START_PAGE_URL
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.babylon.wallet.android.utils.openUrl
import rdx.works.core.domain.assets.SupportedCurrency
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun WalletScreen(
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel,
    onMenuClick: () -> Unit,
    onAccountClick: (Network.Account) -> Unit = { },
    onNavigateToMnemonicBackup: (FactorSourceID.FromHash) -> Unit,
    onNavigateToMnemonicRestore: () -> Unit,
    onAccountCreationClick: () -> Unit,
    showNPSSurvey: () -> Unit
) {
    val context = LocalContext.current
    val walletState by viewModel.state.collectAsStateWithLifecycle()

    WalletContent(
        modifier = modifier,
        state = walletState,
        onMenuClick = onMenuClick,
        onShowHideBalanceToggle = viewModel::onShowHideBalanceToggle,
        onAccountClick = onAccountClick,
        onAccountCreationClick = onAccountCreationClick,
        onRefresh = viewModel::onRefresh,
        onMessageShown = viewModel::onMessageShown,
        onApplySecuritySettings = viewModel::onApplySecuritySettings,
        onRadixBannerDismiss = viewModel::onRadixBannerDismiss
    )

    LaunchedEffect(Unit) {
        viewModel.babylonFactorSourceDoesNotExistEvent.collect {
            viewModel.createBabylonFactorSource { context.biometricAuthenticateSuspend() }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is WalletEvent.NavigateToMnemonicBackup -> onNavigateToMnemonicBackup(it.factorSourceId)
                is WalletEvent.NavigateToMnemonicRestore -> onNavigateToMnemonicRestore()
                WalletEvent.ShowNpsSurvey -> showNPSSurvey()
            }
        }
    }
    SyncNpsSurveyState(walletState, viewModel::dismissSurvey)
}

/**
 * NPS survey is new composable destination, so current lifecycle is paused when NPS takes over,
 * and resumed when wallet screen is shown again. We use that fact to mark survey as shown.
 *
 */
@Composable
fun SyncNpsSurveyState(walletState: WalletUiState, onDismiss: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(walletState.isNpsSurveyShown) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (walletState.isNpsSurveyShown) {
                    onDismiss()
                }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun WalletContent(
    modifier: Modifier = Modifier,
    state: WalletUiState,
    onMenuClick: () -> Unit,
    onShowHideBalanceToggle: (isVisible: Boolean) -> Unit,
    onAccountClick: (Network.Account) -> Unit,
    onAccountCreationClick: () -> Unit,
    onRefresh: () -> Unit,
    onMessageShown: () -> Unit,
    onRadixBannerDismiss: () -> Unit,
    onApplySecuritySettings: (Network.Account, SecurityPromptType) -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RadixTheme.colors.defaultBackground),
                title = {
                    Text(
                        text = stringResource(id = R.string.homePage_title),
                        style = RadixTheme.typography.title,
                        color = RadixTheme.colors.gray1
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = ImageVector.vectorResource(
                                    id = com.babylon.wallet.android.designsystem.R.drawable.ic_settings
                                ),
                                contentDescription = null,
                                tint = RadixTheme.colors.gray1
                            )
                        }

                        if (state.isSettingsWarningVisible) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(
                                        top = RadixTheme.dimensions.paddingSmall,
                                        end = RadixTheme.dimensions.paddingSmall
                                    ),
                                containerColor = RadixTheme.colors.red1
                            )
                        }
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        },
        containerColor = RadixTheme.colors.defaultBackground,
        contentColor = RadixTheme.colors.defaultText
    ) { padding ->
        val pullRefreshState = rememberPullRefreshState(state.isRefreshing, onRefresh = onRefresh)
        Box(modifier = Modifier.padding(padding)) {
            WalletAccountList(
                modifier = Modifier.pullRefresh(pullRefreshState),
                state = state,
                onShowHideBalanceToggle = onShowHideBalanceToggle,
                onAccountClick = onAccountClick,
                onAccountCreationClick = onAccountCreationClick,
                onApplySecuritySettings = onApplySecuritySettings,
                onRadixBannerDismiss = onRadixBannerDismiss
            )

            AnimatedVisibility(visible = state.isLoading) {
                CircularProgressIndicator(color = RadixTheme.colors.gray1)
            }

            PullRefreshIndicator(
                refreshing = state.isRefreshing,
                state = pullRefreshState,
                contentColor = RadixTheme.colors.gray1,
                backgroundColor = RadixTheme.colors.defaultBackground,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun WalletAccountList(
    modifier: Modifier = Modifier,
    state: WalletUiState,
    onShowHideBalanceToggle: (isVisible: Boolean) -> Unit,
    onAccountClick: (Network.Account) -> Unit,
    onAccountCreationClick: () -> Unit,
    onRadixBannerDismiss: () -> Unit,
    onApplySecuritySettings: (Network.Account, SecurityPromptType) -> Unit,
) {
    LazyColumn(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Text(
                text = stringResource(id = R.string.homePage_subtitle),
                modifier = Modifier.padding(
                    vertical = RadixTheme.dimensions.paddingMedium,
                    horizontal = RadixTheme.dimensions.paddingXXLarge
                ),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
            if (state.isFiatBalancesEnabled) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
                Text(
                    text = stringResource(R.string.homePage_totalValue).uppercase(),
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXSmall))
                TotalFiatBalanceView(
                    fiatPrice = state.totalFiatValueOfWallet,
                    isLoading = state.isWalletBalanceLoading,
                    currency = SupportedCurrency.USD,
                    formattedContentStyle = RadixTheme.typography.header,
                    onVisibilityToggle = onShowHideBalanceToggle,
                    trailingContent = {
                        TotalFiatBalanceViewToggle(onToggle = onShowHideBalanceToggle)
                    }
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            }
        }
        itemsIndexed(state.accountsAndAssets) { _, accountWithAssets ->
            AccountCardView(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                    .throttleClickable {
                        onAccountClick(accountWithAssets.account)
                    },
                accountWithAssets = accountWithAssets,
                fiatTotalValue = state.totalFiatValueForAccount(accountWithAssets.account.address),
                accountTag = state.getTag(accountWithAssets.account),
                isFiatBalancesEnabled = state.isFiatBalancesEnabled,
                isLoadingResources = accountWithAssets.assets == null,
                isLoadingBalance = accountWithAssets.assets == null ||
                    state.isBalanceLoadingForAccount(accountWithAssets.account.address),
                securityPromptType = state.securityPrompt(accountWithAssets.account),
                onApplySecuritySettings = {
                    onApplySecuritySettings(accountWithAssets.account, it)
                }
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }
        item {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            RadixSecondaryButton(
                text = stringResource(id = R.string.homePage_createNewAccount),
                onClick = onAccountCreationClick,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }

        item {
            if (state.isRadixBannerVisible) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
                RadixBanner(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                    onDismiss = onRadixBannerDismiss
                )
            } else {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
        }
    }
}

@Composable
private fun RadixBanner(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RadixTheme.shapes.roundedRectDefault,
        color = RadixTheme.colors.gray5
    ) {
        Box {
            Column(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
            ) {
                Image(
                    modifier = Modifier.height(50.dp),
                    painter = painterResource(id = R.drawable.ic_radix_banner),
                    contentScale = ContentScale.FillHeight,
                    contentDescription = null
                )

                Text(
                    text = stringResource(id = R.string.homePage_radixBanner_title),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                )

                Text(
                    text = stringResource(id = R.string.homePage_radixBanner_subtitle),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    textAlign = TextAlign.Center
                )

                val context = LocalContext.current
                RadixSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.homePage_radixBanner_action),
                    contentColor = RadixTheme.colors.gray1,
                    onClick = {
                        context.openUrl(RADIX_START_PAGE_URL)
                    },
                    trailingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_external_link),
                            contentDescription = null,
                            tint = RadixTheme.colors.gray1
                        )
                    }
                )
            }

            IconButton(
                modifier = Modifier.align(Alignment.TopEnd),
                onClick = onDismiss
            ) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray2
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun WalletContentPreview() {
    RadixWalletPreviewTheme {
        with(SampleDataProvider()) {
            WalletContent(
                state = WalletUiState(
                    accountsWithAssets = listOf(
                        sampleAccountWithoutResources(),
                        sampleAccountWithoutResources(name = "my account with a way too much long name")
                    ),
                    loading = false,
                    isBackupWarningVisible = true,
                    uiMessage = null
                ),
                onMenuClick = {},
                onShowHideBalanceToggle = {},
                onAccountClick = {},
                onAccountCreationClick = { },
                onRefresh = { },
                onMessageShown = {},
                onApplySecuritySettings = { _, _ -> },
                onRadixBannerDismiss = {}
            )
        }
    }
}
