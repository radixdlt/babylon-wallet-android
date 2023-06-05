package com.babylon.wallet.android.presentation.account

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FungibleResourcesContent(
    modifier: Modifier = Modifier,
    resources: Resources?,
    onFungibleTokenClick: (Resource.FungibleResource) -> Unit,
) {
    val xrdItem = resources?.xrd
    val restItems = resources?.nonXrdFungibles.orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(RadixTheme.dimensions.paddingMedium),
        modifier = modifier,
    ) {
        if (xrdItem != null) {
            stickyHeader {
                FungibleItemRow(
                    fungible = xrdItem,
                    modifier = Modifier
                        .shadow(4.dp, RadixTheme.shapes.roundedRectMedium)
                        .fillMaxWidth()
                        .background(
                            RadixTheme.colors.defaultBackground,
                            RadixTheme.shapes.roundedRectMedium
                        )
                        .clip(RadixTheme.shapes.roundedRectMedium)
                        .clickable {
                            onFungibleTokenClick(xrdItem)
                        },
                )
            }
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            }
        }
        itemsIndexed(
            items = restItems,
            key = { _, item ->
                item.resourceAddress
            },
            itemContent = { index, item ->
                val isLastItem = index == restItems.lastIndex
                val shape = when {
                    index == 0 && isLastItem -> RadixTheme.shapes.roundedRectMedium
                    index == 0 -> RadixTheme.shapes.roundedRectTopMedium
                    isLastItem -> RadixTheme.shapes.roundedRectBottomMedium
                    else -> RectangleShape
                }
                FungibleItemRow(
                    fungible = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RadixTheme.colors.defaultBackground, shape)
                        .clip(shape)
                        .clickable {
                            onFungibleTokenClick(item)
                        }
                )

                if (!isLastItem) {
                    Divider(Modifier.fillMaxWidth(), 1.dp, RadixTheme.colors.gray5)
                }
            }
        )
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListOfTokenItemsEmptyPreview() {
    RadixWalletTheme {
        FungibleResourcesContent(
            resources = null,
            modifier = Modifier.heightIn(min = 200.dp, max = 600.dp),
            onFungibleTokenClick = {}
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun ListOfTokenItemsPreview() {
    RadixWalletTheme {
        FungibleResourcesContent(
            resources = Resources(
                fungibleResources = persistentListOf(
                    Resource.FungibleResource(
                        resourceAddress = "account_rdx_abcdef",
                        amount = BigDecimal.TEN,
                        symbolMetadataItem = SymbolMetadataItem(symbol = "XRD")
                    )
                ),
                nonFungibleResources = persistentListOf()
            ),
            modifier = Modifier.heightIn(min = 200.dp, max = 600.dp),
            onFungibleTokenClick = {}
        )
    }
}
