package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.model.displaySubtitle
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.card.CommonCard
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.resources.Resource

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.nftsTab(
    assetsViewData: AssetsViewData,
    state: AssetsViewState,
    action: AssetsViewAction,
    onInfoClick: (GlossaryItem) -> Unit
) {
    if (assetsViewData.isNonFungibleCollectionsEmpty) {
        item {
            EmptyResourcesContent(
                modifier = Modifier.fillMaxWidth(),
                tab = AssetsTab.Nfts,
                onInfoClick = onInfoClick
            )
        }
    }

    item {
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
    }

    val openedCollection = state.openedNFTCollectionId?.let { openedCollectionId ->
        assetsViewData.nonFungibleCollections.firstOrNull {
            it.collection.address.string == openedCollectionId
        }
    }

    if (openedCollection != null) {
        stickyHeader(
            key = "opened-${openedCollection.collection.address.string}",
            contentType = { "collection" }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                NFTHeader(
                    collection = openedCollection,
                    state = state,
                    action = action,
                    isOpened = true
                )
            }
        }

        when (state.nftsViewMode) {
            NFTsViewMode.Full -> {
                items(
                    count = openedCollection.collection.amount.toInt(),
                    key = { index -> "${openedCollection.collection.address}$index" },
                    contentType = { "nft" }
                ) { index ->
                    NFTItem(index, openedCollection.collection, state, action)
                }
            }

            NFTsViewMode.Grid -> {
                val columns = 2
                items(
                    count = (openedCollection.collection.amount.toInt() + columns - 1) / columns,
                    key = { rowIndex -> "${openedCollection.collection.address}-grid-$rowIndex" },
                    contentType = { "nft_grid_row" }
                ) { rowIndex ->
                    NFTGridRow(rowIndex, openedCollection.collection, state, action)
                }
            }

            NFTsViewMode.Row -> {
                items(
                    count = openedCollection.collection.amount.toInt(),
                    key = { index -> "${openedCollection.collection.address}-row-$index" },
                    contentType = { "nft_row" }
                ) { index ->
                    NFTRowItem(index, openedCollection.collection, state, action)
                }
            }
        }
    } else {
        assetsViewData.nonFungibleCollections.forEachIndexed { outerIndex, nonFungible ->
            item(
                key = nonFungible.collection.address.string,
                contentType = { "collection" }
            ) {
                NFTHeader(
                    collection = nonFungible,
                    state = state,
                    action = action,
                    isOpened = false,
                    modifier = Modifier.padding(top = if (outerIndex == 0) 0.dp else RadixTheme.dimensions.paddingLarge)
                )
            }
        }
    }
}

@Composable
private fun NFTItem(
    index: Int,
    collection: Resource.NonFungibleResource,
    state: AssetsViewState,
    action: AssetsViewAction
) {
    CommonCard(
        modifier = Modifier
            .padding(top = if (index == 0) RadixTheme.dimensions.paddingDefault else 1.dp)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        itemIndex = index,
        allItemsSize = collection.amount.toInt()
    ) {
        val nft = collection.items.getOrNull(index)
        if (nft != null) {
            NonFungibleResourceItem(
                collection = collection,
                item = nft,
                action = action
            )
        } else {
            LaunchedEffect(collection.address, state.fetchingNFTsPerCollection) {
                if (collection.address !in state.fetchingNFTsPerCollection) {
                    action.onNextNFtsPageRequest(collection)
                }
            }

            NonFungibleResourcePlaceholder(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        }
    }
}

@Composable
private fun NFTGridRow(
    rowIndex: Int,
    collection: Resource.NonFungibleResource,
    state: AssetsViewState,
    action: AssetsViewAction
) {
    val columns = 2
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            .padding(top = RadixTheme.dimensions.paddingDefault),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        repeat(columns) { column ->
            val index = rowIndex * columns + column
            if (index < collection.amount.toInt()) {
                NFTGridCard(
                    modifier = Modifier.weight(1f)
                ) {
                    NFTGridItemContent(index, collection, state, action)
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NFTGridCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = RadixTheme.colors.card,
        shadowElevation = if (RadixTheme.config.isDarkTheme) 0.dp else 4.dp
    ) {
        content()
    }
}

@Composable
private fun NFTGridItemContent(
    index: Int,
    collection: Resource.NonFungibleResource,
    state: AssetsViewState,
    action: AssetsViewAction
) {
    val nft = collection.items.getOrNull(index)
    if (nft != null) {
        NonFungibleResourceGridItem(
            collection = collection,
            item = nft,
            action = action
        )
    } else {
        LaunchedEffect(collection.address, state.fetchingNFTsPerCollection) {
            if (collection.address !in state.fetchingNFTsPerCollection) {
                action.onNextNFtsPageRequest(collection)
            }
        }

        NonFungibleResourcePlaceholder(
            modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
            isCompact = true
        )
    }
}

@Composable
private fun NFTRowItem(
    index: Int,
    collection: Resource.NonFungibleResource,
    state: AssetsViewState,
    action: AssetsViewAction
) {
    CommonCard(
        modifier = Modifier
            .padding(top = if (index == 0) RadixTheme.dimensions.paddingDefault else 1.dp)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        itemIndex = index,
        allItemsSize = collection.amount.toInt()
    ) {
        val nft = collection.items.getOrNull(index)
        if (nft != null) {
            NonFungibleResourceItem(
                collection = collection,
                item = nft,
                action = action,
                viewMode = NFTsViewMode.Row
            )
        } else {
            LaunchedEffect(collection.address, state.fetchingNFTsPerCollection) {
                if (collection.address !in state.fetchingNFTsPerCollection) {
                    action.onNextNFtsPageRequest(collection)
                }
            }

            NonFungibleResourceRowPlaceholder(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        }
    }
}

@Composable
private fun NFTHeader(
    collection: NonFungibleCollection,
    state: AssetsViewState,
    action: AssetsViewAction,
    isOpened: Boolean,
    modifier: Modifier = Modifier
) {
    val rowModifier = if (isOpened) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .clickable {
                action.onCollectionClick(collection.resource.address.string)
            }
    }
    var controlsVisible by remember(collection.resource.address.string, isOpened) {
        mutableStateOf(false)
    }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(isOpened) {
        controlsVisible = isOpened
    }

    val animationSpec = tween<androidx.compose.ui.unit.Dp>(durationMillis = 180)
    val backSlotWidth by animateDpAsState(
        targetValue = if (isOpened && controlsVisible) 36.dp else 0.dp,
        animationSpec = animationSpec,
        label = "nftHeaderBackSlotWidth"
    )
    val backSlotGap by animateDpAsState(
        targetValue = if (isOpened && controlsVisible) RadixTheme.dimensions.paddingSmall else 0.dp,
        animationSpec = animationSpec,
        label = "nftHeaderBackSlotGap"
    )
    val toggleSlotWidth by animateDpAsState(
        targetValue = if (isOpened && controlsVisible) 40.dp else 0.dp,
        animationSpec = animationSpec,
        label = "nftHeaderToggleSlotWidth"
    )
    val controlsAlpha by animateFloatAsState(
        targetValue = if (isOpened && controlsVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "nftHeaderControlsAlpha"
    )
    val closeCollection = {
        coroutineScope.launch {
            controlsVisible = false
            delay(120)
            action.onCollectionClick(collection.resource.address.string)
        }
    }

    Surface(
        modifier = modifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        shape = RoundedCornerShape(12.dp),
        color = RadixTheme.colors.card,
        shadowElevation = if (RadixTheme.config.isDarkTheme) 0.dp else 4.dp
    ) {
        Row(
            modifier = rowModifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (isOpened) {
                Box(
                    modifier = Modifier
                        .width(backSlotWidth)
                        .height(36.dp)
                        .alpha(controlsAlpha)
                        .throttleClickable {
                            closeCollection()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NFTHeaderBackIcon(
                            modifier = Modifier.fillMaxSize(),
                            color = RadixTheme.colors.icon
                        )
                    }
                }
                Spacer(modifier = Modifier.width(backSlotGap))
            }

            Thumbnail.NonFungible(
                modifier = Modifier.size(48.dp),
                collection = collection.resource
            )

            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = collection.displayTitle(),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.text,
                    maxLines = 1
                )

                Text(
                    text = collection.displaySubtitle(),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.textSecondary,
                )
            }

            if (isOpened) {
                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))
                Box(
                    modifier = Modifier
                        .width(toggleSlotWidth)
                        .height(40.dp)
                        .alpha(controlsAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    NFTViewModeToggle(
                        viewMode = state.nftsViewMode,
                        onClick = {
                            action.onNFTsViewModeClick(state.nftsViewMode.next())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NFTHeaderBackIcon(
    modifier: Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.5.dp.toPx()
        drawLine(
            color = color,
            start = center.copy(x = size.width * 0.9f),
            end = center.copy(x = size.width * 0.15f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = center.copy(x = size.width * 0.15f),
            end = center.copy(x = size.width * 0.45f, y = size.height * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = center.copy(x = size.width * 0.15f),
            end = center.copy(x = size.width * 0.45f, y = size.height * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun NFTViewModeToggle(
    viewMode: NFTsViewMode,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(RadixTheme.colors.backgroundSecondary)
            .throttleClickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        NFTViewModeIcon(
            modifier = Modifier.size(18.dp),
            viewMode = viewMode,
            color = RadixTheme.colors.icon
        )
    }
}

@Composable
private fun NFTViewModeIcon(
    modifier: Modifier,
    viewMode: NFTsViewMode,
    color: Color
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        when (viewMode) {
            NFTsViewMode.Full -> {
                val iconSize = size.minDimension * 0.74f
                val left = (size.width - iconSize) / 2f
                val top = (size.height - iconSize) / 2f
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(iconSize, iconSize),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = stroke
                )
            }

            NFTsViewMode.Grid -> {
                val cell = size.minDimension * 0.3f
                val gap = size.minDimension * 0.2f
                val total = cell * 2 + gap
                val startX = (size.width - total) / 2f
                val startY = (size.height - total) / 2f
                repeat(2) { row ->
                    repeat(2) { column ->
                        drawRoundRect(
                            color = color,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                x = startX + column * (cell + gap),
                                y = startY + row * (cell + gap)
                            ),
                            size = androidx.compose.ui.geometry.Size(cell, cell),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                            style = stroke
                        )
                    }
                }
            }

            NFTsViewMode.Row -> {
                val rectWidth = size.width * 0.88f
                val rectHeight = size.height * 0.26f
                val gap = size.height * 0.16f
                val startX = (size.width - rectWidth) / 2f
                val startY = (size.height - rectHeight * 2 - gap) / 2f
                repeat(2) { index ->
                    drawRoundRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            x = startX,
                            y = startY + index * (rectHeight + gap)
                        ),
                        size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                        style = stroke
                    )
                }
            }
        }
    }
}

private fun NFTsViewMode.next(): NFTsViewMode = when (this) {
    NFTsViewMode.Full -> NFTsViewMode.Grid
    NFTsViewMode.Grid -> NFTsViewMode.Row
    NFTsViewMode.Row -> NFTsViewMode.Full
}

@Composable
private fun NonFungibleResourceGridItem(
    collection: Resource.NonFungibleResource,
    item: Resource.NonFungibleResource.Item,
    action: AssetsViewAction
) {
    val clickableModifier = Modifier.throttleClickable {
        when (action) {
            is AssetsViewAction.Click -> {
                action.onNonFungibleItemClick(collection, item)
            }

            is AssetsViewAction.Selection -> {
                action.onNFTCheckChanged(collection, item, !action.isSelected(item.globalId))
            }
        }
    }

    Column(modifier = clickableModifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(RadixTheme.colors.backgroundSecondary)
        ) {
            Thumbnail.NFT(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(RadixTheme.dimensions.paddingXSmall),
                nft = item,
                cropped = false,
                cornerRadius = 12.dp,
                forcedAspectRatio = 1f,
                contentScale = ContentScale.Fit,
                backgroundColor = RadixTheme.colors.backgroundSecondary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
        ) {
            item.name?.let { name ->
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = name,
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.text,
                    maxLines = 1
                )
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "ID: ${item.localId.formatted()}",
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.textSecondary,
                maxLines = 1
            )

            if (action is AssetsViewAction.Selection) {
                val isSelected = remember(item, action) {
                    action.isSelected(item.globalId)
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    AssetsViewCheckBox(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        isSelected = isSelected,
                        onCheckChanged = { isChecked ->
                            action.onNFTCheckChanged(collection, item, isChecked)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NonFungibleResourceItem(
    modifier: Modifier = Modifier,
    collection: Resource.NonFungibleResource,
    item: Resource.NonFungibleResource.Item,
    action: AssetsViewAction,
    viewMode: NFTsViewMode = NFTsViewMode.Full
) {
    val clickableModifier = modifier.throttleClickable {
        when (action) {
            is AssetsViewAction.Click -> {
                action.onNonFungibleItemClick(collection, item)
            }

            is AssetsViewAction.Selection -> {
                action.onNFTCheckChanged(collection, item, !action.isSelected(item.globalId))
            }
        }
    }

    if (viewMode == NFTsViewMode.Row) {
        NonFungibleResourceRowItem(
            modifier = clickableModifier,
            collection = collection,
            item = item,
            action = action
        )
        return
    }

    Column(
        modifier = clickableModifier
            .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingLarge),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
    ) {
        Thumbnail.NFT(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = RadixTheme.dimensions.paddingSmall),
            nft = item
        )

        item.name?.let { name ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = RadixTheme.dimensions.paddingXXSmall),
                text = name,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.text,
                maxLines = if (viewMode == NFTsViewMode.Grid) 1 else Int.MAX_VALUE
            )
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = item.localId.formatted(),
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.textSecondary,
            maxLines = if (viewMode == NFTsViewMode.Grid) 1 else Int.MAX_VALUE
        )

        if (action is AssetsViewAction.Selection) {
            val isSelected = remember(item, action) {
                action.isSelected(item.globalId)
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                AssetsViewCheckBox(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    isSelected = isSelected,
                    onCheckChanged = { isChecked ->
                        action.onNFTCheckChanged(collection, item, isChecked)
                    }
                )
            }
        }
    }
}

@Composable
private fun NonFungibleResourceRowItem(
    modifier: Modifier = Modifier,
    collection: Resource.NonFungibleResource,
    item: Resource.NonFungibleResource.Item,
    action: AssetsViewAction
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Thumbnail.NFT(
            modifier = Modifier.size(64.dp),
            nft = item
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
        ) {
            item.name?.let { name ->
                Text(
                    text = name,
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.text,
                    maxLines = 1
                )
            }

            Text(
                text = item.localId.formatted(),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.textSecondary,
                maxLines = 1
            )
        }

        if (action is AssetsViewAction.Selection) {
            val isSelected = remember(item, action) {
                action.isSelected(item.globalId)
            }
            AssetsViewCheckBox(
                isSelected = isSelected,
                onCheckChanged = { isChecked ->
                    action.onNFTCheckChanged(collection, item, isChecked)
                }
            )
        }
    }
}

@Composable
private fun NonFungibleResourcePlaceholder(
    modifier: Modifier,
    isCompact: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(Thumbnail.NFTAspectRatio)
                .radixPlaceholder(
                    visible = true,
                    shape = RoundedCornerShape(Thumbnail.NFTCornerRadius)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(23.dp)
                .radixPlaceholder(
                    visible = true,
                    shape = RoundedCornerShape(Thumbnail.NFTCornerRadius)
                )
        )

        if (!isCompact) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.5f)
                    .height(23.dp)
                    .radixPlaceholder(
                        visible = true,
                        shape = RoundedCornerShape(Thumbnail.NFTCornerRadius)
                    )
            )
        }
    }
}

@Composable
private fun NonFungibleResourceRowPlaceholder(
    modifier: Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .radixPlaceholder(
                    visible = true,
                    shape = RoundedCornerShape(Thumbnail.NFTCornerRadius)
                )
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(23.dp)
                    .radixPlaceholder(
                        visible = true,
                        shape = RoundedCornerShape(Thumbnail.NFTCornerRadius)
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(23.dp)
                    .radixPlaceholder(
                        visible = true,
                        shape = RoundedCornerShape(Thumbnail.NFTCornerRadius)
                    )
            )
        }
    }
}

@Preview
@UsesSampleValues
@Composable
fun NFTCItemCollapsedPreview() {
    RadixWalletTheme {
        LazyColumn {
            nftsTab(
                assetsViewData = previewAssetViewData,
                state = AssetsViewState(AssetsTab.Nfts, mapOf(ResourceAddress.sampleMainnet().string to true), emptySet()),
                action = AssetsViewAction.Click(
                    onTabClick = {},
                    onCollectionClick = {},
                    onNextNFtsPageRequest = {},
                    onStakesRequest = {},
                    onNonFungibleItemClick = { _, _ -> },
                    onFungibleClick = {},
                    onLSUClick = {},
                    onPoolUnitClick = {},
                    onClaimClick = {},
                    onNFTsViewModeClick = {}
                ),
                onInfoClick = {}
            )
        }
    }
}

@Preview
@UsesSampleValues
@Composable
fun NFTItemExpandedPreview() {
    RadixWalletTheme {
        LazyColumn {
            nftsTab(
                assetsViewData = previewAssetViewData,
                state = AssetsViewState(AssetsTab.Nfts, mapOf(ResourceAddress.sampleMainnet().string to false), emptySet()),
                action = AssetsViewAction.Click(
                    onTabClick = {},
                    onCollectionClick = {},
                    onNextNFtsPageRequest = {},
                    onStakesRequest = {},
                    onNonFungibleItemClick = { _, _ -> },
                    onFungibleClick = {},
                    onLSUClick = {},
                    onPoolUnitClick = {},
                    onClaimClick = {},
                    onNFTsViewModeClick = {}
                ),
                onInfoClick = {}
            )
        }
    }
}
