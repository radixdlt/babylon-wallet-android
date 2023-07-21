@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class
)

package com.babylon.wallet.android.presentation.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.SetStatusBarColor
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.presentation.account.composable.FungibleTokenBottomSheetDetails
import com.babylon.wallet.android.presentation.account.composable.NonFungibleTokenBottomSheetDetails
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab
import com.babylon.wallet.android.presentation.transfer.assets.ResourcesTabs
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.resources.FungibleResourceItem
import com.babylon.wallet.android.presentation.ui.composables.resources.NonFungibleResourceItem
import com.babylon.wallet.android.presentation.ui.composables.resources.fungibleResources
import com.babylon.wallet.android.presentation.ui.composables.resources.nonFungibleResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.FactorSource

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onAccountPreferenceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onNavigateToMnemonicBackup: (FactorSource.FactorSourceID.FromHash) -> Unit,
    onTransferClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SetStatusBarColor(color = Color.Transparent, useDarkIcons = !isSystemInDarkTheme())
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is AccountEvent.NavigateToMnemonicBackup -> {
                    onNavigateToMnemonicBackup(it.factorSourceId)
                }
            }
        }
    }
    AccountScreenContent(
        state = state,
        onAccountPreferenceClick = {
            onAccountPreferenceClick(it)
        },
        onBackClick = onBackClick,
        onRefresh = viewModel::refresh,
        onHistoryClick = {},
        onTransferClick = onTransferClick,
        onMessageShown = viewModel::onMessageShown,
        onFungibleResourceClicked = viewModel::onFungibleResourceClicked,
        onNonFungibleItemClicked = viewModel::onNonFungibleResourceClicked,
        modifier = modifier,
        onApplySecuritySettings = viewModel::onApplySecuritySettings
    )
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun AccountScreenContent(
    modifier: Modifier = Modifier,
    state: AccountUiState,
    onAccountPreferenceClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onHistoryClick: () -> Unit,
    onTransferClick: (String) -> Unit,
    onMessageShown: () -> Unit,
    onFungibleResourceClicked: (Resource.FungibleResource) -> Unit,
    onNonFungibleItemClicked: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
    onApplySecuritySettings: () -> Unit
) {
    val gradient = remember(state.accountWithResources) {
        val appearanceId = state.accountWithResources?.account?.appearanceID ?: 0
        AccountGradientList[appearanceId % AccountGradientList.size]
    }

    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val scope = rememberCoroutineScope()
    BackHandler {
        if (bottomSheetState.isVisible) {
            scope.launch {
                bottomSheetState.hide()
            }
        } else {
            onBackClick()
        }
    }

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    ModalBottomSheetLayout(
        modifier = modifier,
        sheetState = bottomSheetState,
        sheetBackgroundColor = RadixTheme.colors.defaultBackground,
        scrimColor = Color.Black.copy(alpha = 0.3f),
        sheetShape = RadixTheme.shapes.roundedRectTopDefault,
        sheetContent = {
            SheetContent(
                modifier = Modifier.navigationBarsPadding(),
                state = state,
                scope = scope,
                bottomSheetState = bottomSheetState
            )
        },
    ) {
        val pullToRefreshState = rememberPullRefreshState(
            refreshing = state.isRefreshing,
            onRefresh = onRefresh,
            refreshingOffset = 116.dp
        )
        val lazyListState = rememberLazyListState()
        Box(
            modifier = Modifier
                .pullRefresh(pullToRefreshState)
        ) {
            Scaffold(
                modifier = Modifier
                    .navigationBarsPadding()
                    .background(Brush.horizontalGradient(gradient)),
                topBar = {
                    AccountTopBar(
                        state = state,
                        lazyListState = lazyListState,
                        onBackClick = onBackClick,
                        onAccountPreferenceClick = onAccountPreferenceClick,
                        onTransferClick = onTransferClick,
                        onApplySecuritySettings = onApplySecuritySettings
                    )
                },
                containerColor = Color.Transparent,
                floatingActionButton = {
                    if (state.isHistoryEnabled) {
                        HistoryButton(
                            modifier = Modifier.size(174.dp, 50.dp),
                            onHistoryClick
                        )
                    }
                },
                floatingActionButtonPosition = FabPosition.Center,
                snackbarHost = {
                    RadixSnackbarHost(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        hostState = snackBarHostState
                    )
                }
            ) { innerPadding ->
                AssetsContent(
                    innerPadding = innerPadding,
                    state = state,
                    lazyListState = lazyListState,
                    onFungibleTokenClick = {
                        onFungibleResourceClicked(it)
                        scope.launch {
                            bottomSheetState.show()
                        }
                    },
                    onNonFungibleItemClick = { nftCollection, nftItem ->
                        onNonFungibleItemClicked(nftCollection, nftItem)
                        scope.launch {
                            bottomSheetState.show()
                        }
                    }
                )
            }

            PullRefreshIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
                refreshing = state.isRefreshing,
                state = pullToRefreshState,
                contentColor = RadixTheme.colors.gray1,
                backgroundColor = RadixTheme.colors.defaultBackground,
            )
        }
    }
}

@Composable
private fun SheetContent(
    modifier: Modifier = Modifier,
    state: AccountUiState,
    scope: CoroutineScope,
    bottomSheetState: ModalBottomSheetState
) {
    when (val selected = state.selectedResource) {
        is SelectedResource.SelectedNonFungibleResource -> {
            NonFungibleTokenBottomSheetDetails(
                modifier = modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                item = selected.item,
                nonFungibleItem = selected.nonFungible,
                onCloseClick = {
                    scope.launch {
                        bottomSheetState.hide()
                    }
                }
            )
        }

        is SelectedResource.SelectedFungibleResource -> {
            FungibleTokenBottomSheetDetails(
                modifier = modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                fungible = selected.fungible,
                onCloseClick = {
                    scope.launch {
                        bottomSheetState.hide()
                    }
                }
            )
        }

        else -> {}
    }
}

@Composable
fun AssetsContent(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues,
    lazyListState: LazyListState,
    state: AccountUiState,
    onFungibleTokenClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleItemClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    Surface(
        modifier = modifier.padding(
            start = innerPadding.calculateStartPadding(layoutDirection),
            end = innerPadding.calculateEndPadding(layoutDirection),
            top = innerPadding.calculateTopPadding()
        ),
        color = RadixTheme.colors.gray5,
        shape = RadixTheme.shapes.roundedRectTopDefault,
        elevation = 8.dp
    ) {
        var selectedTab by remember { mutableStateOf(ResourceTab.Tokens) }
        val resources = state.accountWithResources?.resources
        val xrdItem = resources?.xrd
        val restOfFungibles = resources?.nonXrdFungibles.orEmpty()

        val nonFungibleCollections = resources?.nonFungibleResources.orEmpty()
        val collapsedState = remember(nonFungibleCollections) {
            nonFungibleCollections.map { true }.toMutableStateList()
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = PaddingValues(
                    start = RadixTheme.dimensions.paddingDefault,
                    end = RadixTheme.dimensions.paddingDefault,
                    top = RadixTheme.dimensions.paddingLarge,
                    bottom = innerPadding.calculateBottomPadding()
                )
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = RadixTheme.dimensions.paddingLarge),
                        contentAlignment = Alignment.Center
                    ) {
                        ResourcesTabs(
                            selectedTab = selectedTab,
                            onTabSelected = {
                                selectedTab = it
                            }
                        )
                    }
                }

                if (resources != null) {
                    when (selectedTab) {
                        ResourceTab.Tokens -> fungibleResources(
                            xrdItem = xrdItem,
                            restOfFungibles = restOfFungibles
                        ) { _, item ->
                            FungibleResourceItem(
                                modifier = Modifier
                                    .height(83.dp)
                                    .clickable {
                                        onFungibleTokenClick(item)
                                    },
                                resource = item
                            )
                        }

                        ResourceTab.Nfts -> nonFungibleResources(
                            collections = nonFungibleCollections,
                            collapsedState = collapsedState,
                        ) { collection, item ->
                            NonFungibleResourceItem(
                                modifier = Modifier
                                    .padding(RadixTheme.dimensions.paddingDefault)
                                    .clickable {
                                        onNonFungibleItemClick(collection, item)
                                    },
                                item = item
                            )
                        }
                    }
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = RadixTheme.colors.gray1
                )
            }
        }
    }
}

@Composable
private fun HistoryButton(modifier: Modifier = Modifier, onHistoryClick: () -> Unit) {
    RadixSecondaryButton(
        text = stringResource(id = R.string.common_history),
        onClick = onHistoryClick,
        modifier = modifier,
        containerColor = RadixTheme.colors.gray2,
        contentColor = RadixTheme.colors.white,
        shape = RadixTheme.shapes.circle
    ) {
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_watch_later),
            tint = RadixTheme.colors.white,
            contentDescription = null
        )
    }
}

@Preview
@Composable
fun AccountContentPreview() {
    RadixWalletTheme {
        with(SampleDataProvider()) {
            AccountScreenContent(
                state = AccountUiState(
                    accountWithResources = AccountWithResources(
                        account = sampleAccount("acount_rdx_abcde"),
                        resources = Resources(
                            fungibleResources = sampleFungibleResources(),
                            nonFungibleResources = listOf()
                        ),
                    )
                ),
                onAccountPreferenceClick = {},
                onBackClick = {},
                onRefresh = {},
                onHistoryClick = {},
                onTransferClick = {},
                onMessageShown = {},
                onFungibleResourceClicked = {},
                onNonFungibleItemClicked = { _, _ -> },
                onApplySecuritySettings = {}
            )
        }
    }
}
