@file:OptIn(ExperimentalFoundationApi::class)

package com.babylon.wallet.android.presentation.settings.account.specificassets

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogDragHandle
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.babylon.wallet.android.utils.truncatedHash
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SpecificAssetsDepositsScreen(
    viewModel: SpecificAssetsDepositsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val hideCallback = { scope.launch { sheetState.hide() } }
    BackHandler {
        if (sheetState.isVisible) {
            hideCallback()
        } else {
            onBackClick()
        }
    }
    ModalBottomSheetLayout(
        modifier = modifier
//            .systemBarsPadding()
            .navigationBarsPadding(),
        sheetContent = {
            AddAssetSheet(
                onResourceAddressChanged = {},
                resourceAddress = "",
                resourceAddressValid = true,
                onAddAsset = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                hideCallback()
            }
        },
        sheetState = sheetState,
        sheetBackgroundColor = RadixTheme.colors.gray4,
        sheetShape = RadixTheme.shapes.roundedRectTopDefault
    ) {
        SpecificAssetsDepositsContent(
            onBackClick = onBackClick,
            loading = state.isLoading,
            onMessageShown = viewModel::onMessageShown,
            error = state.error,
            modifier = Modifier
                .fillMaxSize()
                .background(RadixTheme.colors.gray5),
            onShowAddAssetSheet = {
                scope.launch {
                    sheetState.show()
                }
            },
            allowedAssets = state.allowedAssets,
            deniedAssets = state.deniedAssets
        )
    }
}

@Composable
fun AddAssetSheet(
    onResourceAddressChanged: (String) -> Unit,
    resourceAddress: String,
    resourceAddressValid: Boolean,
    onAddAsset: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = modifier.background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopDefault).verticalScroll(
            rememberScrollState()
        ).imePadding(),
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
                value = resourceAddress,
                hint = stringResource(id = R.string.accountSettings_specificAssetsDeposits_addAnAssetInputHint),
                singleLine = true,
                error = null
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)) {
                LabeledRadioButton(
                    modifier = Modifier.weight(1f),
                    label = stringResource(id = R.string.accountSettings_specificAssetsDeposits_addAnAssetAllow),
                    selected = false,
                    onSelected = {}
                )
                LabeledRadioButton(
                    modifier = Modifier.weight(1f),
                    label = stringResource(id = R.string.accountSettings_specificAssetsDeposits_addAnAssetDeny),
                    selected = false,
                    onSelected = {}
                )
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            RadixPrimaryButton(
                text = stringResource(R.string.accountSettings_specificAssetsDeposits_addAnAssetButton),
                onClick = {
                    onAddAsset()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = resourceAddressValid,
                isLoading = false
            )
        }
    }
}

@Composable
private fun LabeledRadioButton(modifier: Modifier, label: String, selected: Boolean, onSelected: () -> Unit) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
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
    loading: Boolean,
    onMessageShown: () -> Unit,
    error: UiMessage?,
    onShowAddAssetSheet: () -> Unit,
    modifier: Modifier = Modifier,
    allowedAssets: ImmutableList<Asset>,
    deniedAssets: ImmutableList<Asset>
) {
    var selectedTab by remember {
        mutableStateOf(SpecificAssetsTab.Allowed)
    }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState()
    var showNotSecuredDialog by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.accountSettings_specificAssetsDeposits),
            onBackClick = onBackClick,
            containerColor = RadixTheme.colors.defaultBackground
        )
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                pageCount = SpecificAssetsTab.values().size,
                state = pagerState,
                userScrollEnabled = false
            ) { tabIndex ->
                val tab = SpecificAssetsTab.values().get(tabIndex)
                when (tab) {
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
                                AssetsList(allowedAssets)
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
                                AssetsList(deniedAssets)
                            }
                        }
                    }
                }
            }
            if (loading) {
                FullscreenCircularProgressContent()
            }
            SnackbarUiMessageHandler(message = error, onMessageShown = {
                onMessageShown()
            })
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.defaultBackground)
        ) {
            RadixPrimaryButton(
                text = stringResource(R.string.accountSettings_specificAssetsDeposits_addAnAssetButton),
                onClick = onShowAddAssetSheet,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault)
            )
        }

        if (showNotSecuredDialog) {
            NotSecureAlertDialog(finish = {
                showNotSecuredDialog = false
            })
        }
    }
}

@Composable
private fun AssetsList(
    assets: ImmutableList<Asset>,
    modifier: Modifier = Modifier
) {
    val lastItem = assets.last()
    LazyColumn(modifier = modifier) {
        items(assets) { asset ->
            AssetItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground)
                    .padding(RadixTheme.dimensions.paddingDefault),
                asset
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
    asset: Asset
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        AsyncImage(
            model = rememberImageUrl(
                fromUrl = Uri.parse(asset.iconUrl),
                size = ImageSize.SMALL
            ),
            placeholder = painterResource(id = R.drawable.img_placeholder),
            fallback = painterResource(id = R.drawable.img_placeholder),
            error = painterResource(id = R.drawable.img_placeholder),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RadixTheme.shapes.circle)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = asset.name,
                textAlign = TextAlign.Start,
                maxLines = 1,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1
            )
            Text(
                text = asset.address.truncatedHash(),
                textAlign = TextAlign.Start,
                maxLines = 1,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        }
        IconButton(
            onClick = {
//                onDeleteAsset(asset)
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

@Preview(showBackground = true)
@Composable
fun SpecificAssetsDepositsPreview() {
    RadixWalletTheme {
        SpecificAssetsDepositsContent(
            onBackClick = {},
            loading = false,
            onMessageShown = {},
            error = null,
            onShowAddAssetSheet = {},
            allowedAssets = persistentListOf(Asset.sampleAsset(), Asset.sampleAsset(), Asset.sampleAsset()),
            deniedAssets = persistentListOf(Asset.sampleAsset(), Asset.sampleAsset(), Asset.sampleAsset())
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddAssetSheetPreview() {
    RadixWalletTheme {
        AddAssetSheet(
            onResourceAddressChanged = {},
            resourceAddress = "res1",
            resourceAddressValid = true,
            onAddAsset = {}
        ) {}
    }
}
