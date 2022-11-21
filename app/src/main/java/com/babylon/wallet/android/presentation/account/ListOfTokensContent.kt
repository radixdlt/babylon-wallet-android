package com.babylon.wallet.android.presentation.account

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.data.mockdata.mockTokenUiList
import com.babylon.wallet.android.presentation.model.TokenUiModel
import com.babylon.wallet.android.presentation.ui.theme.BabylonWalletTheme

@Suppress("UnstableCollections")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListOfTokensContent(
    tokenItems: List<TokenUiModel>,
    modifier: Modifier = Modifier,
    xrdTokenUi: TokenUiModel? = null,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 32.dp),
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (xrdTokenUi != null) {
            stickyHeader {
                TokenItemCard(token = xrdTokenUi, isFirst = true)
            }
        }
        items(
            items = tokenItems,
            key = { item: TokenUiModel ->
                item.id
            },
            itemContent = {
                TokenItemCard(token = it)
            }
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun ListOfTokenItemsPreview() {
    BabylonWalletTheme {
        ListOfTokensContent(
            tokenItems = mockTokenUiList,
            modifier = Modifier.heightIn(min = 200.dp, max = 600.dp)
        )
    }
}
