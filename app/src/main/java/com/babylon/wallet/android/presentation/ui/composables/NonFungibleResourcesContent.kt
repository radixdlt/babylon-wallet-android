package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources

@Composable
fun NonFungibleResourcesContent(
    modifier: Modifier = Modifier,
    resources: Resources?,
    onNftClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
) {
    val collections = resources?.nonFungibleResources.orEmpty()
    val collapsedState = remember(collections) {
        collections.map { true }.toMutableStateList()
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = RadixTheme.dimensions.paddingDefault,
            end = RadixTheme.dimensions.paddingDefault,
            bottom = RadixTheme.dimensions.paddingDefault
        )
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
                NftTokenDetailItem(
                    modifier = Modifier.padding(vertical = 1.dp),
                    item = item,
                    bottomCornersRounded = collection.items.last().globalAddress == item.globalAddress,
                    onItemClicked = {
                        onNftClick(collection, item)
                    }
                )
            }

            if (collectionIndex != collections.lastIndex) {
                item { Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault)) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NftTokenListEmpty() {
    RadixWalletTheme {
        NonFungibleResourcesContent(
            resources = Resources.EMPTY,
            onNftClick = { _, _ -> }
        )
    }
}
