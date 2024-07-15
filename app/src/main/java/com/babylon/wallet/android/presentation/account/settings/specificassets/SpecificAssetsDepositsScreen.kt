@file:OptIn(ExperimentalFoundationApi::class)

package com.babylon.wallet.android.presentation.account.settings.specificassets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.account.composable.UnknownDepositRulesStateInfo
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.AccountThirdPartyDepositsViewModel
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.AssetType
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.model.displayTitleAsNFTCollection
import com.babylon.wallet.android.presentation.model.displayTitleAsToken
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogHeader
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.radixdlt.sargon.DepositAddressExceptionRule
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.samples.sampleRandom
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.core.domain.resources.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecificAssetsDepositsScreen(
    sharedViewModel: AccountThirdPartyDepositsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by sharedViewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val kb = LocalSoftwareKeyboardController.current
    val hideCallback = {
        kb?.hide()
        sharedViewModel.setAddAssetSheetVisible(false)
        scope.launch { sheetState.hide() }
    }
    BackHandler {
        if (sheetState.isVisible) {
            hideCallback()
        } else {
            onBackClick()
        }
    }
    val dialogState = state.deleteDialogState
    if (dialogState is DeleteDialogState.AboutToDeleteAssetException) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    sharedViewModel.onDeleteAsset(dialogState.assetException)
                } else {
                    sharedViewModel.hideDeletePrompt()
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_removeAsset),
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1
                )
            },
            message = {
                Text(
                    text = when (dialogState.assetException.assetException?.exceptionRule) {
                        DepositAddressExceptionRule.ALLOW -> {
                            stringResource(id = R.string.accountSettings_specificAssetsDeposits_removeAssetMessageAllow)
                        }

                        DepositAddressExceptionRule.DENY -> {
                            stringResource(id = R.string.accountSettings_specificAssetsDeposits_removeAssetMessageDeny)
                        }

                        null -> ""
                    },
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(
                id = R.string.common_remove
            ),
            dismissText = stringResource(
                id = R.string.common_cancel
            )
        )
    }

    SpecificAssetsDepositsContent(
        onBackClick = onBackClick,
        onMessageShown = sharedViewModel::onMessageShown,
        error = state.error,
        onShowAddAssetSheet = {
            sharedViewModel.onAssetExceptionRuleChanged(
                when (it) {
                    SpecificAssetsTab.Allowed -> DepositAddressExceptionRule.ALLOW
                    SpecificAssetsTab.Denied -> DepositAddressExceptionRule.DENY
                }
            )
            scope.launch {
                sharedViewModel.setAddAssetSheetVisible(true)
                sheetState.show()
            }
        },
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.gray5),
        allowedAssets = state.allowedAssets,
        deniedAssets = state.deniedAssets,
        onDeleteAsset = sharedViewModel::showDeletePrompt
    )

    if (state.isAddAssetSheetVisible) {
        BottomSheetDialogWrapper(
            addScrim = true,
            showDragHandle = true,
            onDismiss = {
                hideCallback()
            },
            showDefaultTopBar = false
        ) {
            AddAssetSheet(
                onResourceAddressChanged = sharedViewModel::assetExceptionAddressTyped,
                asset = state.assetExceptionToAdd,
                onAddAsset = {
                    hideCallback()
                    sharedViewModel.onAddAssetException()
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .fillMaxWidth()
                    .clip(RadixTheme.shapes.roundedRectTopDefault),
                onAssetExceptionRuleChanged = sharedViewModel::onAssetExceptionRuleChanged,
                onDismiss = {
                    hideCallback()
                }
            )
        }
    }
}

@Composable
fun AddAssetSheet(
    onResourceAddressChanged: (String) -> Unit,
    asset: AssetType.ExceptionType,
    onAddAsset: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onAssetExceptionRuleChanged: (DepositAddressExceptionRule) -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(
                rememberScrollState()
            )
            .imePadding(),
        verticalArrangement = Arrangement.Center,
    ) {
        BottomDialogHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingSmall),
            onDismissRequest = onDismiss
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_addAnAssetTitle),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_addAnAssetSubtitle),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                onValueChanged = onResourceAddressChanged,
                value = asset.addressToDisplay,
                hint = stringResource(id = R.string.accountSettings_specificAssetsDeposits_addAnAssetInputHint),
                singleLine = true,
                error = null
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)) {
                LabeledRadioButton(
                    modifier = Modifier.weight(1f),
                    label = stringResource(id = R.string.accountSettings_specificAssetsDeposits_addAnAssetAllow),
                    selected = asset.rule == DepositAddressExceptionRule.ALLOW,
                    onSelected = {
                        onAssetExceptionRuleChanged(DepositAddressExceptionRule.ALLOW)
                    }
                )
                LabeledRadioButton(
                    modifier = Modifier.weight(1f),
                    label = stringResource(id = R.string.accountSettings_specificAssetsDeposits_addAnAssetDeny),
                    selected = asset.rule == DepositAddressExceptionRule.DENY,
                    onSelected = {
                        onAssetExceptionRuleChanged(DepositAddressExceptionRule.DENY)
                    }
                )
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
            RadixPrimaryButton(
                text = stringResource(R.string.accountSettings_specificAssetsDeposits_addAnAssetButton),
                onClick = {
                    onAddAsset()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = asset.addressValid,
                isLoading = false
            )
        }
    }
}

@Composable
private fun LabeledRadioButton(modifier: Modifier, label: String, selected: Boolean, onSelected: () -> Unit) {
    Row(
        modifier = modifier.clickable {
            onSelected()
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        RadioButton(
            selected = selected,
            colors = RadioButtonDefaults.colors(
                selectedColor = RadixTheme.colors.gray1,
                unselectedColor = RadixTheme.colors.gray3,
                disabledSelectedColor = Color.White
            ),
            onClick = onSelected,
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = label,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpecificAssetsDepositsContent(
    onBackClick: () -> Unit,
    onMessageShown: () -> Unit,
    error: UiMessage?,
    onShowAddAssetSheet: (SpecificAssetsTab) -> Unit,
    modifier: Modifier = Modifier,
    allowedAssets: PersistentList<AssetType.ExceptionType>?,
    deniedAssets: PersistentList<AssetType.ExceptionType>?,
    onDeleteAsset: (AssetType.ExceptionType) -> Unit
) {
    var selectedTab by remember {
        mutableStateOf(SpecificAssetsTab.Allowed)
    }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(0) {
        SpecificAssetsTab.entries.size
    }
    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = error,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.accountSettings_specificAssetsDeposits),
                onBackClick = onBackClick,
                containerColor = RadixTheme.colors.defaultBackground,
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground)
            ) {
                RadixPrimaryButton(
                    text = stringResource(R.string.accountSettings_specificAssetsDeposits_addAnAssetButton),
                    onClick = {
                        onShowAddAssetSheet(selectedTab)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RadixTheme.dimensions.paddingDefault),
                    enabled = allowedAssets != null && deniedAssets != null
                )
            }
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SpecificAssetsTabs(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingDefault)
                    .fillMaxWidth(0.8f)
                    .background(RadixTheme.colors.gray4, shape = RadixTheme.shapes.roundedRectSmall),
                pagerState = pagerState,
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    scope.launch {
                        pagerState.animateScrollToPage(SpecificAssetsTab.entries.indexOf(tab))
                    }
                    selectedTab = tab
                }
            )
            HorizontalPager(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = pagerState,
                userScrollEnabled = false,
            ) { tabIndex ->
                when (SpecificAssetsTab.entries[tabIndex]) {
                    SpecificAssetsTab.Allowed -> {
                        when {
                            allowedAssets == null -> UnknownDepositRulesStateInfo(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(RadixTheme.dimensions.paddingDefault)
                            )

                            allowedAssets.isEmpty() -> {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                                    text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_emptyAllowAll),
                                    textAlign = TextAlign.Center,
                                    style = RadixTheme.typography.body1HighImportance,
                                    color = RadixTheme.colors.gray2
                                )
                            }

                            else -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                                        text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_allowInfo),
                                        style = RadixTheme.typography.body1HighImportance,
                                        color = RadixTheme.colors.gray2
                                    )
                                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                                    AssetsList(assets = allowedAssets, onDeleteAsset = onDeleteAsset)
                                }
                            }
                        }
                    }

                    SpecificAssetsTab.Denied -> {
                        when {
                            deniedAssets == null -> UnknownDepositRulesStateInfo(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(RadixTheme.dimensions.paddingDefault)
                            )

                            deniedAssets.isEmpty() -> {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                                    text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_emptyDenyAll),
                                    textAlign = TextAlign.Center,
                                    style = RadixTheme.typography.body1HighImportance,
                                    color = RadixTheme.colors.gray2
                                )
                            }

                            else -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                                        text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_denyInfo),
                                        style = RadixTheme.typography.body1HighImportance,
                                        color = RadixTheme.colors.gray2
                                    )
                                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                                    AssetsList(assets = deniedAssets, onDeleteAsset = onDeleteAsset)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetsList(
    assets: PersistentList<AssetType.ExceptionType>,
    modifier: Modifier = Modifier,
    onDeleteAsset: (AssetType.ExceptionType) -> Unit
) {
    val lastItem = assets.last()
    LazyColumn(modifier = modifier) {
        items(assets) { asset ->
            AssetItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground)
                    .padding(RadixTheme.dimensions.paddingDefault),
                asset = asset,
                onDeleteAsset = onDeleteAsset
            )
            if (lastItem != asset) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    color = RadixTheme.colors.gray5
                )
            }
        }
    }
}

@Composable
private fun AssetItem(
    modifier: Modifier,
    asset: AssetType.ExceptionType,
    onDeleteAsset: (AssetType.ExceptionType) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        when (asset.resource) {
            is Resource.FungibleResource -> Thumbnail.Fungible(token = asset.resource, modifier = Modifier.size(44.dp))
            is Resource.NonFungibleResource -> Thumbnail.NonFungible(
                collection = asset.resource,
                modifier = Modifier.size(44.dp)
            )

            else -> {}
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val name = when (asset.resource) {
                is Resource.FungibleResource -> asset.resource.displayTitleAsToken()
                is Resource.NonFungibleResource -> asset.resource.displayTitleAsNFTCollection()
                null -> null
            }

            Text(
                text = name.orEmpty(),
                maxLines = 1,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1
            )

            Text(
                text = asset.assetException?.address?.formatted().orEmpty(),
                textAlign = TextAlign.Start,
                maxLines = 1,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        }
        IconButton(
            onClick = {
                onDeleteAsset(asset)
            }
        ) {
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_delete_outline),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )
        }
    }
}

@Composable
fun SpecificAssetsTabs(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    selectedTab: SpecificAssetsTab,
    onTabSelected: (SpecificAssetsTab) -> Unit
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(selectedTab) {
        if (selectedTab.ordinal != pagerState.currentPage) {
            scope.launch { pagerState.animateScrollToPage(page = selectedTab.ordinal) }
        }
    }

    SpecificAssetsTabs(
        modifier = modifier,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected
    )
}

@Composable
fun SpecificAssetsTabs(
    modifier: Modifier = Modifier,
    selectedTab: SpecificAssetsTab,
    onTabSelected: (SpecificAssetsTab) -> Unit
) {
    val tabIndex = remember(selectedTab) {
        SpecificAssetsTab.values().indexOf(selectedTab)
    }
    TabRow(
        modifier = modifier,
        selectedTabIndex = tabIndex,
        containerColor = Color.Transparent,
        divider = {},
        indicator = { tabPositions ->
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[tabIndex])
                    .fillMaxHeight()
                    .zIndex(-1f)
                    .padding(2.dp)
                    .background(RadixTheme.colors.white, RadixTheme.shapes.roundedRectSmall)
            )
        }
    ) {
        SpecificAssetsTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            Tab(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onTabSelected(tab)
                    }
                },
                selectedContentColor = RadixTheme.colors.gray1,
                unselectedContentColor = RadixTheme.colors.gray1
            ) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = RadixTheme.dimensions.paddingMedium,
                        vertical = RadixTheme.dimensions.paddingSmall
                    ),
                    text = tab.name(),
                    style = if (isSelected) RadixTheme.typography.body1Header else RadixTheme.typography.body1Regular,
                )
            }
        }
    }
}

@Composable
private fun SpecificAssetsTab.name(): String = when (this) {
    SpecificAssetsTab.Allowed -> stringResource(id = R.string.accountSettings_specificAssetsDeposits_allow)
    SpecificAssetsTab.Denied -> stringResource(id = R.string.accountSettings_specificAssetsDeposits_deny)
}

enum class SpecificAssetsTab {
    Allowed,
    Denied
}

sealed interface DeleteDialogState {
    data object None : DeleteDialogState
    data class AboutToDeleteAssetException(val assetException: AssetType.ExceptionType) : DeleteDialogState
    data class AboutToDeleteAssetDepositor(val depositor: AssetType.DepositorType) : DeleteDialogState
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun SpecificAssetsDepositsPreview() {
    val assetExceptionProvider = {
        AssetType.ExceptionType(assetAddress = ResourceAddress.sampleRandom(NetworkId.MAINNET))
    }
    RadixWalletTheme {
        SpecificAssetsDepositsContent(
            onBackClick = {},
            onMessageShown = {},
            error = null,
            onShowAddAssetSheet = {},
            allowedAssets = List(3) { assetExceptionProvider() }.toPersistentList(),
            deniedAssets = List(3) { assetExceptionProvider() }.toPersistentList()
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
fun AddAssetSheetPreview() {
    RadixWalletTheme {
        AddAssetSheet(
            onResourceAddressChanged = {},
            asset = AssetType.ExceptionType(),
            onAddAsset = {},
            onAssetExceptionRuleChanged = {},
            onDismiss = {}
        )
    }
}
