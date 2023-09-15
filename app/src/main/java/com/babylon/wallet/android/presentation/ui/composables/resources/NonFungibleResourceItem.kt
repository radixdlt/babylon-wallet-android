package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail

@Composable
fun NonFungibleResourceItem(
    modifier: Modifier = Modifier,
    item: Resource.NonFungibleResource.Item,
) {
    Column(
        modifier = modifier,
        verticalArrangement = spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Thumbnail.NFT(
            modifier = Modifier.fillMaxWidth(),
            nft = item
        )
        item.nameMetadataItem?.name?.let { name ->
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = name,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1
            )
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = item.localId.displayable,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray2
        )
    }
}

@Composable
fun SelectableNonFungibleResourceItem(
    modifier: Modifier = Modifier,
    item: Resource.NonFungibleResource.Item,
    isSelected: Boolean,
    onCheckChanged: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .padding(vertical = RadixTheme.dimensions.paddingDefault)
            .padding(start = RadixTheme.dimensions.paddingDefault, end = RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        NonFungibleResourceItem(
            modifier = Modifier.weight(1f),
            item = item
        )

        Checkbox(
            checked = isSelected,
            onCheckedChange = onCheckChanged,
            colors = CheckboxDefaults.colors(
                checkedColor = RadixTheme.colors.gray1,
                uncheckedColor = RadixTheme.colors.gray2,
                checkmarkColor = Color.White
            )
        )
    }
}
