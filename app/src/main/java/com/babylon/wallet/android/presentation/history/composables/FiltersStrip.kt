package com.babylon.wallet.android.presentation.history.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.HistoryFilters
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.utils.truncatedHash

@Composable
fun FiltersStrip(
    historyFilters: HistoryFilters?,
    userInteractionEnabled: Boolean,
    onTransactionTypeFilterRemoved: () -> Unit,
    onTransactionClassFilterRemoved: () -> Unit,
    onResourceFilterRemoved: (Resource) -> Unit,
    modifier: Modifier = Modifier,
    timeFilterScrollState: LazyListState,
) {
    LazyRow(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium),
        contentPadding = PaddingValues(
            top = RadixTheme.dimensions.paddingMedium,
            start = RadixTheme.dimensions.paddingMedium,
            end = RadixTheme.dimensions.paddingMedium
        ),
        userScrollEnabled = userInteractionEnabled,
        state = timeFilterScrollState
    ) {
        historyFilters?.transactionType?.let { transactionType ->
            item {
                SingleTag(
                    selected = true,
                    text = transactionType.label(),
                    leadingIcon = {
                        Icon(painter = painterResource(id = transactionType.icon()), contentDescription = null, tint = Color.Unspecified)
                    },
                    onCloseClick = {
                        if (userInteractionEnabled) onTransactionTypeFilterRemoved()
                    }
                )
            }
        }
        items(historyFilters?.resources.orEmpty().toList()) { resource ->
            val name = when (resource) {
                is Resource.FungibleResource -> resource.displayTitle
                is Resource.NonFungibleResource -> resource.name
            }
            SingleTag(
                selected = true,
                text = name.ifEmpty { resource.resourceAddress.truncatedHash() },
                onCloseClick = {
                    onResourceFilterRemoved(resource)
                }
            )
        }
        historyFilters?.transactionClass?.let { txClass ->
            item {
                SingleTag(selected = true, text = txClass.description(), onCloseClick = {
                    if (userInteractionEnabled) onTransactionClassFilterRemoved()
                })
            }
        }
    }
}
