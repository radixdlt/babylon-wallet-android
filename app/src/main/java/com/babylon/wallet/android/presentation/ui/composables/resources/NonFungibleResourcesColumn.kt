package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources

@Composable
fun NonFungibleResourcesColumn(
    modifier: Modifier = Modifier,
    resources: Resources?,
    contentPadding: PaddingValues = PaddingValues(
        start = RadixTheme.dimensions.paddingMedium,
        end = RadixTheme.dimensions.paddingMedium,
        top = RadixTheme.dimensions.paddingLarge,
        bottom = 100.dp
    ),
    nftItem: @Composable (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
) {
    val collections = resources?.nonFungibleResources.orEmpty()
    val collapsedState = remember(collections) {
        collections.map { true }.toMutableStateList()
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        nonFungibleResources(
            collections = collections,
            collapsedState = collapsedState,
            nftItem = nftItem
        )
    }
}

fun LazyListScope.nonFungibleResources(
    collections: List<Resource.NonFungibleResource>,
    collapsedState: SnapshotStateList<Boolean>,
    nftItem: @Composable (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
) {
    collections.forEachIndexed { collectionIndex, collection ->
        val collapsed = collapsedState[collectionIndex]

        item(
            key = collection.resourceAddress,
            contentType = { "collection" }
        ) {
            NonFungibleResourceCollectionHeader(
                modifier = Modifier.padding(bottom = 1.dp),
                collection = collection,
                collapsed = collapsed,
                parentSectionClick = {
                    collapsedState[collectionIndex] = !collapsed
                }
            )
        }

        items(
            items = if (collapsed) emptyList() else collection.items,
            key = { item -> item.globalAddress },
            contentType = { "nft" }
        ) { item ->
            val bottomCorners by animateDpAsState(
                targetValue = if (collection.items.last().globalAddress == item.globalAddress) 12.dp else 0.dp
            )
            Card(
                modifier = Modifier
                    .padding(vertical = 1.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(0.dp, 0.dp, bottomCorners, bottomCorners),
                colors = CardDefaults.cardColors(
                    containerColor = RadixTheme.colors.defaultBackground
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                nftItem(collection, item)
            }
        }

        if (collectionIndex != collections.lastIndex) {
            item { Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault)) }
        }
    }
}
