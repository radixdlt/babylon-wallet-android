@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Badge
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.AppState
import com.babylon.wallet.android.MainUiState
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.SetStatusBarColor
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.Red1
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.biometricAuthenticate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import rdx.works.profile.data.model.factorsources.FactorSource

@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAccountClick: (accountId: String) -> Unit = { },
    onNavigateToMnemonicBackup: (FactorSource.ID) -> Unit,
    onNavigateToMnemonicRestore: (String) -> Unit,
    onAccountCreationClick: () -> Unit,
    mainUiState: StateFlow<MainUiState>,
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToOnBoarding: () -> Unit,
    onNavigateToIncompatibleProfile: () -> Unit
) {
    val state by mainUiState.collectAsStateWithLifecycle()
    when (state.initialAppState) {
        is AppState.Wallet -> {
            val walletState by viewModel.state.collectAsStateWithLifecycle()

            SetStatusBarColor(color = RadixTheme.colors.orange2, useDarkIcons = !isSystemInDarkTheme())
            WalletScreenContent(
                onMenuClick = onMenuClick,
                onAccountClick = viewModel::onAccountClick,
                onAccountCreationClick = onAccountCreationClick,
                isRefreshing = walletState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = modifier,
                isLoading = walletState.isLoading,
                accounts = walletState.resources,
                isBackupWarningVisible = walletState.isBackupWarningVisible,
                error = walletState.error,
                onMessageShown = viewModel::onMessageShown,
                onApplySecuritySettings = viewModel::onApplyMnemonicBackup,
                onMnemonicRecovery = onNavigateToMnemonicRestore
            )
            LaunchedEffect(Unit) {
                viewModel.oneOffEvent.collect {
                    when (it) {
                        is WalletEvent.AccountClick -> onAccountClick(it.address)
                        is WalletEvent.NavigateToMnemonicBackup -> onNavigateToMnemonicBackup(it.factorSourceId)
                    }
                }
            }
        }
        is AppState.IncompatibleProfile -> {
            LaunchedEffect(state.initialAppState) {
                onNavigateToIncompatibleProfile()
            }
        }
        is AppState.Loading -> {
            FullscreenCircularProgressContent()
        }
        is AppState.OnBoarding -> {
            LaunchedEffect(state.initialAppState) {
                onNavigateToOnBoarding()
            }
        }
        is AppState.NewProfile -> {
            LaunchedEffect(state.initialAppState) {
                onNavigateToCreateAccount()
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun WalletScreenContent(
    onMenuClick: () -> Unit,
    onAccountClick: (accountId: String) -> Unit,
    onAccountCreationClick: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    accounts: ImmutableList<AccountResources>,
    isBackupWarningVisible: Boolean,
    error: UiMessage?,
    onMessageShown: () -> Unit,
    onApplySecuritySettings: (String) -> Unit,
    onMnemonicRecovery: (String) -> Unit,
) {
    Box(modifier = modifier.navigationBarsPadding()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = RadixTheme.colors.defaultBackground),
                    title = {
                        Text(
                            text = stringResource(id = R.string.home_toolbar_title),
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

                            if (isBackupWarningVisible) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(
                                            top = RadixTheme.dimensions.paddingSmall,
                                            end = RadixTheme.dimensions.paddingSmall
                                        ),
                                    backgroundColor = Red1
                                )
                            }
                        }
                    },
                    windowInsets = WindowInsets(0.dp)
                )
            },
            contentColor = RadixTheme.colors.defaultText,
            backgroundColor = RadixTheme.colors.defaultBackground
        ) { innerPadding ->
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = RadixTheme.colors.gray1
                    )
                }
            }
            AnimatedVisibility(visible = !isLoading, enter = fadeIn()) {
                val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = onRefresh)
                Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
                    WalletAccountList(
                        onAccountClick = onAccountClick,
                        onAccountCreationClick = onAccountCreationClick,
                        accounts = accounts,
                        onApplySecuritySettings = onApplySecuritySettings,
                        onMnemonicRecovery = onMnemonicRecovery
                    )
                    PullRefreshIndicator(
                        refreshing = isRefreshing,
                        state = pullRefreshState,
                        contentColor = RadixTheme.colors.gray1,
                        backgroundColor = RadixTheme.colors.defaultBackground,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
        SnackbarUiMessageHandler(message = error, onMessageShown = onMessageShown)
    }
}

@Suppress("UnstableCollections")
@Composable
private fun WalletAccountList(
    onAccountClick: (accountId: String) -> Unit,
    onAccountCreationClick: () -> Unit,
    onApplySecuritySettings: (String) -> Unit,
    onMnemonicRecovery: (String) -> Unit,
    accounts: List<AccountResources>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LazyColumn(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Text(
                text = stringResource(id = R.string.home_welcome_text),
                modifier = Modifier.padding(
                    vertical = RadixTheme.dimensions.paddingMedium,
                    horizontal = RadixTheme.dimensions.paddingXLarge
                ),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
        }
        itemsIndexed(accounts) { _, account ->
            val gradientColors = AccountGradientList[account.appearanceID % AccountGradientList.size]
            AccountCardView(
                address = account.address,
                accountName = account.displayName,
                isLegacyAccount = account.isOlympiaAccount,
                assets = account.fungibleTokens,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                    .background(Brush.linearGradient(gradientColors), shape = RadixTheme.shapes.roundedRectMedium)
                    .throttleClickable {
                        onAccountClick(account.address)
                    },
                showApplySecuritySettings = account.needMnemonicBackup(),
                onApplySecuritySettings = {
                    context.biometricAuthenticate { authenticatedSuccessfully ->
                        if (authenticatedSuccessfully) {
                            onApplySecuritySettings(account.address)
                        }
                    }
                },
                needMnemonicRecovery = account.needMnemonicRecovery(),
                onMnemonicRecovery = {
                    onMnemonicRecovery(account.address)
                }

            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }
        item {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            RadixSecondaryButton(
                text = stringResource(id = R.string.create_new_account),
                onClick = onAccountCreationClick,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(bottom = RadixTheme.dimensions.paddingLarge)
            )
        }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun WalletContentPreview() {
    RadixWalletTheme {
        with(SampleDataProvider()) {
            WalletScreenContent(
                onMenuClick = {},
                onAccountClick = {},
                onAccountCreationClick = { },
                isRefreshing = false,
                onRefresh = { },
                modifier = Modifier.fillMaxSize(),
                isLoading = false,
                accounts = persistentListOf(sampleAccountResource(), sampleAccountResource()),
                isBackupWarningVisible = true,
                error = null,
                onMessageShown = {},
                onApplySecuritySettings = {},
                onMnemonicRecovery = {}
            )
        }
    }
}
