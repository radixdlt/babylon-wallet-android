package com.babylon.wallet.android.presentation.account

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.presentation.ui.composables.resources.FungibleResourceCard
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
    val restResources = resources?.nonXrdFungibles.orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(RadixTheme.dimensions.paddingMedium),
        modifier = modifier,
    ) {
        if (xrdItem != null) {
            stickyHeader {
                FungibleResourceCard(
                    modifier = Modifier.padding(top = RadixTheme.dimensions.paddingDefault)
                ) {
                    FungibleItemRow(
                        modifier = Modifier
                            .height(83.dp)
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                            .clickable {
                                onFungibleTokenClick(xrdItem)
                            },
                        fungible = xrdItem
                    )
                }
            }
        }

        itemsIndexed(
            items = restResources,
            key = { _, item ->
                item.resourceAddress
            },
            itemContent = { index, resource ->
                val topPadding = if (index == 0) RadixTheme.dimensions.paddingDefault else 0.dp
                FungibleResourceCard(
                    modifier = Modifier.padding(top = topPadding),
                    itemIndex = index,
                    allItemsSize = restResources.size,
                    bottomContent = if (index != restResources.lastIndex) {
                        {
                            Divider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = RadixTheme.colors.gray4
                            )
                        }
                    } else {
                        null
                    }
                ) {
                    FungibleItemRow(
                        modifier = Modifier
                            .height(83.dp)
                            .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                            .clickable {
                                onFungibleTokenClick(resource)
                            },
                        fungible = resource
                    )
                }
            }
        )
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
