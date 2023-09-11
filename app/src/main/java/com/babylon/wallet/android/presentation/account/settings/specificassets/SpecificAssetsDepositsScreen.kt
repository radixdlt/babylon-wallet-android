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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.AccountThirdPartyDepositsViewModel
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.AssetType
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogDragHandle
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.utils.truncatedHash
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network.Account.OnLedgerSettings.ThirdPartyDeposits

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SpecificAssetsDepositsScreen(
    sharedViewModel: AccountThirdPartyDepositsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by sharedViewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val kb = LocalSoftwareKeyboardController.current
    val hideCallback = {
        kb?.hide()
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
            text = {
                Text(
                    text = when (dialogState.assetException.exceptionRule) {
                        ThirdPartyDeposits.DepositAddressExceptionRule.Allow -> {
                            stringResource(id = R.string.accountSettings_specificAssetsDeposits_removeAssetMessageAllow)
                        }

                        ThirdPartyDeposits.DepositAddressExceptionRule.Deny -> {
                            stringResource(id = R.string.accountSettings_specificAssetsDeposits_removeAssetMessageDeny)
                        }
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
    DefaultModalSheetLayout(
        modifier = modifier,
        wrapContent = true,
        sheetContent = {
            AddAssetSheet(
                onResourceAddressChanged = sharedViewModel::assetExceptionAddressTyped,
                asset = state.assetExceptionToAdd,
                onAddAsset = {
                    hideCallback()
                    sharedViewModel.onAddAssetException()
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .background(RadixTheme.colors.gray4)
                    .clip(RadixTheme.shapes.roundedRectTopDefault),
                onAssetExceptionRuleChanged = sharedViewModel::onAssetExceptionRuleChanged,
                onDismiss = {
                    hideCallback()
                }
            )
        },
        sheetState = sheetState
    ) {
        SpecificAssetsDepositsContent(
            onBackClick = onBackClick,
            onMessageShown = sharedViewModel::onMessageShown,
            error = state.error,
            onShowAddAssetSheet = {
                sharedViewModel.onAssetExceptionRuleChanged(
                    when (it) {
                        SpecificAssetsTab.Allowed -> ThirdPartyDeposits.DepositAddressExceptionRule.Allow
                        SpecificAssetsTab.Denied -> ThirdPartyDeposits.DepositAddressExceptionRule.Deny
                    }
                )
                scope.launch {
                    sheetState.show()
                }
            },
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxSize()
                .background(RadixTheme.colors.gray5),
            allowedAssets = state.allowedAssets,
            deniedAssets = state.deniedAssets,
            onDeleteAsset = sharedViewModel::showDeletePrompt
        )
    }
}

@Composable
fun AddAssetSheet(
    onResourceAddressChanged: (String) -> Unit,
    asset: AssetType.AssetException,
    onAddAsset: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onAssetExceptionRuleChanged: (ThirdPartyDeposits.DepositAddressExceptionRule) -> Unit
) {
    Column(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopDefault)
            .verticalScroll(
                rememberScrollState()
            )
            .imePadding(),
        verticalArrangement = Arrangement.Center,
    ) {
        BottomDialogDragHandle(
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopDefault)
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
                value = asset.assetException.address,
                hint = stringResource(id = R.string.accountSettings_specificAssetsDeposits_addAnAssetInputHint),
                singleLine = true,
                error = null
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)) {
                LabeledRadioButton(
                    modifier = Modifier.weight(1f),
                    label = stringResource(id = R.string.accountSettings_specificAssetsDeposits_addAnAssetAllow),
                    selected = asset.assetException.exceptionRule == ThirdPartyDeposits.DepositAddressExceptionRule.Allow,
                    onSelected = {
                        onAssetExceptionRuleChanged(ThirdPartyDeposits.DepositAddressExceptionRule.Allow)
                    }
                )
                LabeledRadioButton(
                    modifier = Modifier.weight(1f),
                    label = stringResource(id = R.string.accountSettings_specificAssetsDeposits_addAnAssetDeny),
                    selected = asset.assetException.exceptionRule == ThirdPartyDeposits.DepositAddressExceptionRule.Deny,
                    onSelected = {
                        onAssetExceptionRuleChanged(ThirdPartyDeposits.DepositAddressExceptionRule.Deny)
                    }
                )
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
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
    allowedAssets: PersistentList<AssetType.AssetException>,
    deniedAssets: PersistentList<AssetType.AssetException>,
    onDeleteAsset: (ThirdPartyDeposits.AssetException) -> Unit
) {
    var selectedTab by remember {
        mutableStateOf(SpecificAssetsTab.Allowed)
    }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState()
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
                modifier = Modifier.navigationBarsPadding()
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
                        .padding(RadixTheme.dimensions.paddingDefault)
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
                        pagerState.animateScrollToPage(SpecificAssetsTab.values().indexOf(tab))
                    }
                    selectedTab = tab
                }
            )
            HorizontalPager(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                pageCount = SpecificAssetsTab.values().size,
                state = pagerState,
                userScrollEnabled = false
            ) { tabIndex ->
                when (SpecificAssetsTab.values()[tabIndex]) {
                    SpecificAssetsTab.Allowed -> {
                        if (allowedAssets.isEmpty()) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                                text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_emptyAllowAll),
                                textAlign = TextAlign.Center,
                                style = RadixTheme.typography.body1HighImportance,
                                color = RadixTheme.colors.gray2
                            )
                        } else {
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

                    SpecificAssetsTab.Denied -> {
                        if (deniedAssets.isEmpty()) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                                text = stringResource(id = R.string.accountSettings_specificAssetsDeposits_emptyAllowAll),
                                textAlign = TextAlign.Center,
                                style = RadixTheme.typography.body1HighImportance,
                                color = RadixTheme.colors.gray2
                            )
                        } else {
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

@Composable
private fun AssetsList(
    assets: PersistentList<AssetType.AssetException>,
    modifier: Modifier = Modifier,
    onDeleteAsset: (ThirdPartyDeposits.AssetException) -> Unit
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
                Divider(
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
    asset: AssetType.AssetException,
    onDeleteAsset: (ThirdPartyDeposits.AssetException) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        val placeholder = if (asset.isNft) {
            painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_nfts)
        } else {
            painterResource(id = R.drawable.img_placeholder)
        }
        AsyncImage(
            model = rememberImageUrl(
                fromUrl = asset.assetIcon,
                size = ImageSize.SMALL
            ),
            placeholder = placeholder,
            fallback = placeholder,
            error = placeholder,
            contentDescription = null,
            contentScale = if (asset.isNft) ContentScale.Inside else ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(if (asset.isNft) RadixTheme.shapes.roundedRectSmall else RadixTheme.shapes.circle)
                .applyIf(asset.isNft, Modifier.background(RadixTheme.colors.gray3, RadixTheme.shapes.roundedRectSmall))
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (asset.assetName.isNotBlank()) {
                Text(
                    text = asset.assetName,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1
                )
            }
            Text(
                text = asset.assetException.address.truncatedHash(),
                textAlign = TextAlign.Start,
                maxLines = 1,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        }
        IconButton(
            onClick = {
                onDeleteAsset(asset.assetException)
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
    object None : DeleteDialogState
    data class AboutToDeleteAssetException(val assetException: ThirdPartyDeposits.AssetException) : DeleteDialogState
    data class AboutToDeleteAssetDepositor(val depositor: ThirdPartyDeposits.DepositorAddress) : DeleteDialogState
}

@Preview(showBackground = true)
@Composable
fun SpecificAssetsDepositsPreview() {
    RadixWalletTheme {
        with(SampleDataProvider()) {
            SpecificAssetsDepositsContent(
                onBackClick = {},
                onMessageShown = {},
                error = null,
                onShowAddAssetSheet = {},
                allowedAssets = persistentListOf(sampleAssetException(), sampleAssetException(true), sampleAssetException()),
                deniedAssets = persistentListOf(sampleAssetException(), sampleAssetException(true), sampleAssetException())
            ) {}
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddAssetSheetPreview() {
    RadixWalletTheme {
        AddAssetSheet(
            onResourceAddressChanged = {},
            asset = AssetType.AssetException(),
            onAddAsset = {},
            onAssetExceptionRuleChanged = {},
            onDismiss = {}
        )
    }
}
