package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.resources.Tag
import com.babylon.wallet.android.presentation.ui.composables.name

@Composable
fun Tag(
    modifier: Modifier = Modifier,
    tag: Tag
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = if (tag == Tag.Official) {
                painterResource(id = R.drawable.ic_radix_tag)
            } else {
                painterResource(id = R.drawable.ic_token_tag)
            },
            contentDescription = "tag image",
            tint = RadixTheme.colors.gray2
        )

        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingSmall),
            text = tag.name(),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray2
        )
    }
}
