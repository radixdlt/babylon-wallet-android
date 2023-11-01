@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Badge
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.Red1
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun WalletScreen(
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel,
    onMenuClick: () -> Unit,
    onAccountClick: (Network.Account) -> Unit = { },
    onNavigateToMnemonicBackup: (FactorSourceID.FromHash) -> Unit,
    onNavigateToMnemonicRestore: (FactorSourceID.FromHash) -> Unit,
    onAccountCreationClick: () -> Unit
) {
    val context = LocalContext.current
    val walletState by viewModel.state.collectAsStateWithLifecycle()

    WalletContent(
        modifier = modifier,
        state = walletState,
        onMenuClick = onMenuClick,
        onAccountClick = onAccountClick,
        onAccountCreationClick = onAccountCreationClick,
        onRefresh = viewModel::onRefresh,
        onMessageShown = viewModel::onMessageShown,
        onApplySecuritySettings = viewModel::onApplySecuritySettings
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
                is WalletEvent.NavigateToMnemonicRestore -> onNavigateToMnemonicRestore(it.factorSourceId)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun WalletContent(
    modifier: Modifier = Modifier,
    state: WalletUiState,
    onMenuClick: () -> Unit,
    onAccountClick: (Network.Account) -> Unit,
    onAccountCreationClick: () -> Unit,
    onRefresh: () -> Unit,
    onMessageShown: () -> Unit,
    onApplySecuritySettings: (Network.Account, SecurityPromptType) -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.error,
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
                                backgroundColor = Red1
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
                onAccountClick = onAccountClick,
                onAccountCreationClick = onAccountCreationClick,
                onApplySecuritySettings = onApplySecuritySettings
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
    onAccountClick: (Network.Account) -> Unit,
    onAccountCreationClick: () -> Unit,
    onApplySecuritySettings: (Network.Account, SecurityPromptType) -> Unit,
) {
    LazyColumn(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Text(
                text = stringResource(id = R.string.homePage_subtitle),
                modifier = Modifier.padding(
                    vertical = RadixTheme.dimensions.paddingMedium,
                    horizontal = RadixTheme.dimensions.paddingXLarge
                ),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
        }
        itemsIndexed(state.accountResources) { _, accountWithResources ->
            AccountCardView(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                    .throttleClickable {
                        onAccountClick(accountWithResources.account)
                    },
                accountWithAssets = accountWithResources,
                accountTag = state.getTag(accountWithResources.account),
                isLoadingResources = accountWithResources.assets == null,
                securityPromptType = state.securityPrompt(accountWithResources.account),
                onApplySecuritySettings = {
                    onApplySecuritySettings(accountWithResources.account, it)
                }
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }
        item {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            RadixSecondaryButton(
                text = stringResource(id = R.string.homePage_createNewAccount),
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
            WalletContent(
                state = WalletUiState(
                    accountsWithResources = listOf(sampleAccountWithResources(), sampleAccountWithResources()),
                    loading = false,
                    isSettingsWarningVisible = true,
                    error = null
                ),
                onMenuClick = {},
                onAccountClick = {},
                onAccountCreationClick = { },
                onRefresh = { },
                onMessageShown = {},
                onApplySecuritySettings = { _, _ -> }
            )
        }
    }
}
