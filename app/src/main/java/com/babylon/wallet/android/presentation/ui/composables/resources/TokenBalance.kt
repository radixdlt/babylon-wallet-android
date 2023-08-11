package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Resource
import rdx.works.core.displayableQuantity

@Composable
fun TokenBalance(resource: Resource.FungibleResource, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        resource.amount?.let { amount ->
            Text(
                modifier = Modifier.alignByBaseline(),
                text = amount.displayableQuantity(),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
        }
        Text(
            modifier = Modifier.alignByBaseline(),
            text = " ${resource.symbol}",
            style = RadixTheme.typography.header,
            color = RadixTheme.colors.gray1
        )
    }
}
