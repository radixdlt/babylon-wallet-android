package com.babylon.wallet.android.presentation.account.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.HistoryFilters
import com.babylon.wallet.android.presentation.model.displayTitleAsToken
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.resources.Resource

@Composable
fun FiltersStrip(
    historyFilters: HistoryFilters?,
    userInteractionEnabled: Boolean,
    onTransactionTypeFilterRemoved: () -> Unit,
    onTransactionClassFilterRemoved: () -> Unit,
    onResourceFilterRemoved: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .background(RadixTheme.colors.background)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium),
        contentPadding = PaddingValues(
            start = RadixTheme.dimensions.paddingMedium,
            end = RadixTheme.dimensions.paddingMedium,
            bottom = RadixTheme.dimensions.paddingMedium
        ),
        userScrollEnabled = userInteractionEnabled
    ) {
        historyFilters?.transactionType?.let { transactionType ->
            item(key = transactionType.name) {
                HistoryFilterTag(
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null
                    ),
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
        historyFilters?.resource?.let { resource ->
            item(key = resource.address.string) {
                val name = when (resource) {
                    is Resource.FungibleResource -> resource.displayTitleAsToken()
                    is Resource.NonFungibleResource -> resource.name
                }

                HistoryFilterTag(
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null
                    ),
                    selected = true,
                    text = name.ifEmpty { resource.address.formatted() },
                    onCloseClick = {
                        onResourceFilterRemoved()
                    }
                )
            }
        }
        historyFilters?.transactionClass?.let { txClass ->
            item(key = txClass.name) {
                HistoryFilterTag(
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null
                    ),
                    selected = true,
                    text = txClass.description(),
                    onCloseClick = {
                        if (userInteractionEnabled) onTransactionClassFilterRemoved()
                    }
                )
            }
        }
    }
}
