package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.account.AssetEmptyState
import com.babylon.wallet.android.presentation.model.NftCollectionUiModel
import com.babylon.wallet.android.presentation.model.toNftUiModel

@Suppress("MutableParams", "UnstableCollections")
@Composable
fun NftListContent(
    collapsedState: SnapshotStateList<Boolean>, // TODO use an immutable object!
    item: List<NftCollectionUiModel>,
    onNftClick: (NftCollectionUiModel, NftCollectionUiModel.NftItemUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (item.isEmpty()) {
        AssetEmptyState(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(id = R.string.you_have_no_nfts),
            subtitle = stringResource(R.string.what_are_nfts),
            onInfoClick = {}
        )
    } else {
        LazyColumn(modifier) {
            item.forEachIndexed { i, dataItem ->
                val collapsed = collapsedState[i]
                item(key = "header_$i") {
                    NftTokenHeaderItem(
                        nftImageUrl = dataItem.iconUrl,
                        nftName = dataItem.name,
                        nftsInCirculation = "?",
                        nftsInPossession = dataItem.nft.size.toString(),
                        nftChildCount = dataItem.nft.size,
                        collapsed = collapsed
                    ) {
                        collapsedState[i] = !collapsed
                    }
                }
                items(
                    dataItem.nft,
                    key = { nft -> nft.id }
                ) { item ->
                    AnimatedVisibility(
                        visible = !collapsed,
                        enter = expandVertically(),
                        exit = shrinkVertically(animationSpec = tween(150))
                    ) {
                        var bottomCornersRounded = false
                        if (dataItem.nft.last() == item) {
                            bottomCornersRounded = true
                        }
                        NftTokenDetailItem(
                            nftId = item.id,
                            imageUrl = null, // TODO do we have image per ntf?
                            bottomCornersRounded = bottomCornersRounded,
                            nftMetadata = item.nftsMetadata,
                            onNftClick = { nftId ->
                                onNftClick(dataItem, dataItem.nft.first { it.id == nftId })
                            }
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NftTokenListEmpty() {
    BabylonWalletTheme {
        val mockNftUiList = SampleDataProvider().mockNftUiList
        NftListContent(
            item = emptyList(),
            collapsedState = remember(mockNftUiList) { mockNftUiList.map { true }.toMutableStateList() },
            onNftClick = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NftTokenListPreview() {
    BabylonWalletTheme {
        val mockNftUiList = SampleDataProvider().mockNftUiList
        NftListContent(
            item = mockNftUiList.toNftUiModel(),
            collapsedState = remember(mockNftUiList) { mockNftUiList.map { true }.toMutableStateList() },
            onNftClick = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NftTokenListExpandedPreview() {
    BabylonWalletTheme {
        val mockNftUiList = SampleDataProvider().mockNftUiList
        NftListContent(
            item = mockNftUiList.toNftUiModel(),
            collapsedState = remember(mockNftUiList) { mockNftUiList.map { false }.toMutableStateList() },
            onNftClick = { _, _ -> }
        )
    }
}
