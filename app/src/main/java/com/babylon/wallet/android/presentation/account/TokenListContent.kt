package com.babylon.wallet.android.presentation.account

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.model.TokenUiModel

@Suppress("UnstableCollections")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TokenListContent(
    tokenItems: List<TokenUiModel>,
    modifier: Modifier = Modifier,
    xrdTokenUi: TokenUiModel? = null,
    onFungibleTokenClick: (TokenUiModel) -> Unit,
) {
    if (tokenItems.isEmpty() && xrdTokenUi == null) {
        AssetEmptyState(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(id = R.string.you_have_no_tokens),
            subtitle = stringResource(
                R.string.what_are_tokens
            ),
            onInfoClick = {}
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(RadixTheme.dimensions.paddingMedium),
            modifier = modifier,
        ) {
            if (xrdTokenUi != null) {
                stickyHeader {
                    TokenItemCard(
                        token = xrdTokenUi,
                        modifier = Modifier
                            .shadow(4.dp, RadixTheme.shapes.roundedRectMedium)
                            .fillMaxWidth()
                            .background(
                                RadixTheme.colors.defaultBackground,
                                RadixTheme.shapes.roundedRectMedium
                            )
                            .clip(RadixTheme.shapes.roundedRectMedium)
                            .clickable {
                                onFungibleTokenClick(xrdTokenUi)
                            },
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                }
            }
            itemsIndexed(
                items = tokenItems,
                key = { _, item: TokenUiModel ->
                    item.id
                },
                itemContent = { index, item ->
                    val lastItem = index == tokenItems.size - 1
                    val shape = when {
                        index == 0 && lastItem -> RadixTheme.shapes.roundedRectMedium
                        index == 0 -> RadixTheme.shapes.roundedRectTopMedium
                        lastItem -> RadixTheme.shapes.roundedRectBottomMedium
                        else -> RectangleShape
                    }
                    TokenItemCard(
                        token = item,
                        modifier = Modifier
//                        .shadow(elevation = 4.dp, shape = shape)
                            .fillMaxWidth()
                            .background(RadixTheme.colors.defaultBackground, shape)
                            .clip(shape)
                            .clickable {
                                onFungibleTokenClick(item)
                            }
                    )
                    if (!lastItem) {
                        Divider(Modifier.fillMaxWidth(), 1.dp, RadixTheme.colors.gray5)
                    }
                }
            )
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListOfTokenItemsEmptyPreview() {
    RadixWalletTheme {
        TokenListContent(
            tokenItems = emptyList(),
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
        TokenListContent(
            tokenItems = SampleDataProvider().mockTokenUiList,
            modifier = Modifier.heightIn(min = 200.dp, max = 600.dp),
            onFungibleTokenClick = {}
        )
    }
}
