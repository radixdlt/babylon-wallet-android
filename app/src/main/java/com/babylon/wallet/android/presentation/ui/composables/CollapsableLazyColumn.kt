package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.model.NftClassUi

@Composable
fun CollapsableLazyColumn(
    sections: List<CollapsableSection>,
    modifier: Modifier = Modifier
) {
    val collapsedState = remember(sections) { sections.map { true }.toMutableStateList() }
    LazyColumn(modifier) {
        sections.forEachIndexed { i, dataItem ->
            val collapsed = collapsedState[i]
            item(key = "header_$i") {
                CollapsableParentItemView(
                    nftImageUrl = dataItem.nftClass.iconUrl,
                    nftName = dataItem.nftClass.name,
                    nftsInCirculation = dataItem.nftClass.nftsInCirculation,
                    nftsInPossession = dataItem.nftClass.nftsInPossession,
                    nftChildCount = dataItem.nftClass.nft.size,
                    collapsed = collapsed,
                    arrowText = if (collapsed)
                        stringResource(id = R.string.show_plus)
                    else
                        stringResource(id = R.string.hide_minus)
                ) {
                    collapsedState[i] = !collapsed
                }
            }
            if (!collapsed) {
                items(dataItem.nftClass.nft) { row ->
                    var bottomCornersRounded = false
                    if (dataItem.nftClass.nft.last() == row) {
                        bottomCornersRounded = true
                    }
                    CollapsableChildItemView(
                        bottomCornersRounded = bottomCornersRounded,
                        nftId = row.id,
                        imageUrl = row.imageUrl,
                        nftMetadata = row.nftsMetadata
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CollapsableLazyColumnPreview() {
    CollapsableLazyColumn(
        sections = listOf(
            CollapsableSection(
                nftClass = NftClassUi(
                    name = "NBA Top Shot",
                    nftsInPossession = "1",
                    nftsInCirculation = "250000",
                    iconUrl = "https://images.unsplash.com/photo-1628373383885-4be0bc0172fa?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=130&q=80",
                    nft = listOf(
                        NftClassUi.NftUi(
                            id = "238589090",
                            imageUrl = "https://images.unsplash.com/photo-1628373383885-4be0bc0172fa?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=130&q=80",
                            nftsMetadata = listOf(Pair("Type", "Devin Booker - Dunk"))
                        )
                    )
                )
            ),
            CollapsableSection(
                nftClass = NftClassUi(
                    name = "NBA Top 2222",
                    nftsInPossession = "3",
                    nftsInCirculation = "250000",
                    iconUrl = "https://images.unsplash.com/photo-1628373383885-4be0bc0172fa?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=130&q=80",
                    nft = listOf(
                        NftClassUi.NftUi(
                            id = "238589090",
                            imageUrl = "https://images.unsplash.com/photo-1628373383885-4be0bc0172fa?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=130&q=80",
                            nftsMetadata = listOf(Pair("Type", "Devin Booker - Dunk"))
                        ),
                        NftClassUi.NftUi(
                            id = "238543534",
                            imageUrl = "https://images.unsplash.com/photo-1628373383885-4be0bc0172fa?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=130&q=80",
                            nftsMetadata = listOf(Pair("Type", "James Bond"))
                        ),
                        NftClassUi.NftUi(
                            id = "342402342",
                            imageUrl = "https://images.unsplash.com/photo-1628373383885-4be0bc0172fa?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=130&q=80",
                            nftsMetadata = listOf(Pair("Type", "Kevin home alone"))
                        )
                    )
                )
            ),
            CollapsableSection(
                nftClass = NftClassUi(
                    name = "NBA Top 3333",
                    nftsInPossession = "11",
                    nftsInCirculation = "250000",
                    iconUrl = "https://images.unsplash.com/photo-1628373383885-4be0bc0172fa?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=130&q=80",
                    nft = listOf(
                        NftClassUi.NftUi(
                            id = "238589090",
                            imageUrl = "https://images.unsplash.com/photo-1628373383885-4be0bc0172fa?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=130&q=80",
                            nftsMetadata = listOf(Pair("Type", "Devin Booker - Dunk"))
                        )
                    )
                )
            ),
        )
    )
}

data class CollapsableSection(val nftClass: NftClassUi)