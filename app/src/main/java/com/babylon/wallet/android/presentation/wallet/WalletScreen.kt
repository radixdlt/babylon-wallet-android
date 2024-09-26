package com.babylon.wallet.android.presentation.wallet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.locker.AccountLockerDeposit
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.HomeCardsCarousel
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.assets.TotalFiatBalanceView
import com.babylon.wallet.android.presentation.ui.composables.assets.TotalFiatBalanceViewToggle
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.presentation.wallet.WalletViewModel.Event
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.babylon.wallet.android.utils.openUrl
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.HomeCard
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toUrl
import com.radixdlt.sargon.samples.AccountMainnetSample
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.SupportedCurrency
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun WalletScreen(
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel,
    onMenuClick: () -> Unit,
    onAccountClick: (Account) -> Unit = { },
    onNavigateToSecurityCenter: () -> Unit,
    onAccountCreationClick: () -> Unit,
    showNPSSurvey: () -> Unit,
    onNavigateToRelinkConnectors: () -> Unit,
    onNavigateToConnectCloudBackup: () -> Unit,
    onNavigateToLinkConnector: () -> Unit,
) {
    val context = LocalContext.current
    val walletState by viewModel.state.collectAsStateWithLifecycle()
    val popUpScreen by viewModel.popUpScreen().collectAsStateWithLifecycle()

    WalletContent(
        modifier = modifier,
        state = walletState,
        onMenuClick = onMenuClick,
        onShowHideBalanceToggle = viewModel::onShowHideBalanceToggle,
        onAccountClick = onAccountClick,
        onAccountCreationClick = onAccountCreationClick,
        onRefresh = viewModel::onRefresh,
        onMessageShown = viewModel::onMessageShown,
        onApplySecuritySettingsClick = viewModel::onApplySecuritySettingsClick,
        onLockerDepositClick = viewModel::onLockerDepositClick,
        onCardClick = viewModel::onCardClick,
        onCardCloseClick = viewModel::onCardClose
    )

    LifecycleEventEffect(event = Lifecycle.Event.ON_START) {
        viewModel.onStart()
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.processBufferedDeepLinkRequest()
    }

    LaunchedEffect(Unit) {
        viewModel.babylonFactorSourceDoesNotExistEvent.collect {
            viewModel.createBabylonFactorSource { context.biometricAuthenticateSuspend() }
        }
    }

    LaunchedEffect(popUpScreen) {
        when (popUpScreen) {
            WalletViewModel.PopUpScreen.RELINK_CONNECTORS -> onNavigateToRelinkConnectors()
            WalletViewModel.PopUpScreen.CONNECT_CLOUD_BACKUP -> onNavigateToConnectCloudBackup()
            WalletViewModel.PopUpScreen.NPS_SURVEY -> showNPSSurvey()
            null -> return@LaunchedEffect
        }
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is Event.NavigateToSecurityCenter -> onNavigateToSecurityCenter()
                Event.NavigateToLinkConnector -> onNavigateToLinkConnector()
                is Event.OpenUrl -> context.openUrl(it.url)
            }
        }
    }

    SyncPopUpScreensState(popUpScreen, viewModel::onPopUpScreenDismissed)
}

/**
 * A [WalletViewModel.PopUpScreen] is a composable destination, so current lifecycle is stopped when it takes over,
 * and started when wallet screen is shown again. We use that fact to mark it as shown.
 */
@Composable
fun SyncPopUpScreensState(popUpScreen: WalletViewModel.PopUpScreen?, onDismiss: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(popUpScreen) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (popUpScreen != null) {
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
    state: WalletViewModel.State,
    onMenuClick: () -> Unit,
    onShowHideBalanceToggle: (isVisible: Boolean) -> Unit,
    onAccountClick: (Account) -> Unit,
    onAccountCreationClick: () -> Unit,
    onRefresh: () -> Unit,
    onMessageShown: () -> Unit,
    onApplySecuritySettingsClick: () -> Unit,
    onLockerDepositClick: (WalletViewModel.State.AccountUiItem, AccountLockerDeposit) -> Unit,
    onCardClick: (HomeCard) -> Unit,
    onCardCloseClick: (HomeCard) -> Unit
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
                    }
                },
                windowInsets = WindowInsets.statusBarsAndBanner
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
        Box {
            WalletAccountList(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState),
                state = state,
                contentPadding = padding,
                onShowHideBalanceToggle = onShowHideBalanceToggle,
                onAccountClick = onAccountClick,
                onAccountCreationClick = onAccountCreationClick,
                onApplySecuritySettingsClick = onApplySecuritySettingsClick,
                onLockerDepositClick = onLockerDepositClick,
                onCardClick = onCardClick,
                onCardCloseClick = onCardCloseClick
            )

            PullRefreshIndicator(
                refreshing = state.isRefreshing,
                state = pullRefreshState,
                contentColor = RadixTheme.colors.gray1,
                backgroundColor = RadixTheme.colors.defaultBackground,
                modifier = Modifier
                    .padding(padding)
                    .align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun WalletAccountList(
    modifier: Modifier = Modifier,
    state: WalletViewModel.State,
    contentPadding: PaddingValues,
    onShowHideBalanceToggle: (isVisible: Boolean) -> Unit,
    onAccountClick: (Account) -> Unit,
    onAccountCreationClick: () -> Unit,
    onApplySecuritySettingsClick: () -> Unit,
    onLockerDepositClick: (WalletViewModel.State.AccountUiItem, AccountLockerDeposit) -> Unit,
    onCardClick: (HomeCard) -> Unit,
    onCardCloseClick: (HomeCard) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = contentPadding
    ) {
        if (state.cards.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                HomeCardsCarousel(
                    cards = state.cards,
                    onClick = onCardClick,
                    onCloseClick = onCardCloseClick
                )
            }
        }

        if (!state.isFiatPricesDisabled) {
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Text(
                    text = stringResource(R.string.homePage_totalValue).uppercase(),
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXSmall))
                TotalFiatBalanceView(
                    fiatPrice = state.totalBalance,
                    isLoading = state.isLoadingTotalBalance,
                    currency = SupportedCurrency.USD,
                    formattedContentStyle = RadixTheme.typography.header,
                    onVisibilityToggle = onShowHideBalanceToggle,
                    trailingContent = {
                        TotalFiatBalanceViewToggle(onToggle = onShowHideBalanceToggle)
                    }
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
            }
        } else {
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            }
        }

        itemsIndexed(state.accountUiItems) { _, accountWithAssets ->
            AccountCardView(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                    .throttleClickable {
                        onAccountClick(accountWithAssets.account)
                    },
                accountWithAssets = accountWithAssets,
                onApplySecuritySettingsClick = onApplySecuritySettingsClick,
                onLockerDepositClick = onLockerDepositClick
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }

        item {
            RadixSecondaryButton(
                text = stringResource(id = R.string.homePage_createNewAccount),
                onClick = onAccountCreationClick
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
private fun WalletContentPreview(
    @PreviewParameter(WalletUiStateProvider::class) uiState: WalletViewModel.State
) {
    RadixWalletPreviewTheme {
        WalletContent(
            state = uiState,
            onMenuClick = {},
            onShowHideBalanceToggle = {},
            onAccountClick = {},
            onAccountCreationClick = { },
            onRefresh = { },
            onMessageShown = {},
            onApplySecuritySettingsClick = {},
            onLockerDepositClick = { _, _ -> },
            onCardClick = {},
            onCardCloseClick = {}
        )
    }
}

@UsesSampleValues
class WalletUiStateProvider : PreviewParameterProvider<WalletViewModel.State> {

    override val values: Sequence<WalletViewModel.State>
        get() = sequenceOf(
            WalletViewModel.State(
                accountsWithAssets = listOf(
                    AccountWithAssets(
                        account = Account.sampleMainnet(),
                        assets = null
                    ),
                    AccountWithAssets(
                        account = Account.sampleMainnet.other(),
                        assets = Assets(
                            tokens = listOf(
                                Token(Resource.FungibleResource.sampleMainnet())
                            )
                        )
                    ),
                    AccountWithAssets(
                        account = AccountMainnetSample.carol,
                        assets = Assets(),
                    )
                ),
                accountsWithSecurityPrompts = mapOf(
                    Account.sampleMainnet.other().address to setOf(
                        SecurityPromptType.WRITE_DOWN_SEED_PHRASE,
                        SecurityPromptType.RECOVERY_REQUIRED
                    )
                ),
                prices = WalletViewModel.State.PricesState.Enabled(
                    pricesPerAccount = mapOf(
                        Account.sampleMainnet().address to emptyList(),
                        Account.sampleMainnet.other().address to listOf<AssetPrice>(
                            AssetPrice.TokenPrice(
                                asset = Token(Resource.FungibleResource.sampleMainnet()),
                                price = FiatPrice(Decimal192.sample(), currency = SupportedCurrency.USD)
                            )
                        )
                    )
                ),
                cards = listOf(
                    HomeCard.StartRadQuest,
                    HomeCard.Dapp(iconUrl = "https://stokenet-dashboard.radixdlt.com/dashboard_icon.png".toUrl()),
                    HomeCard.Connector
                ).toPersistentList()
            ),
            WalletViewModel.State()
        )
}
