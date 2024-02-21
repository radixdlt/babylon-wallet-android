package com.babylon.wallet.android.presentation.history.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.modifier.applyIf

@Composable
fun SingleTag(
    modifier: Modifier = Modifier,
    selected: Boolean,
    text: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null
) {
    val tagBorderModifier = Modifier
        .border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.circle)
        .padding(horizontal = RadixTheme.dimensions.paddingMedium, vertical = RadixTheme.dimensions.paddingSmall)
    val tagSelectedModifier = Modifier
        .background(RadixTheme.colors.gray1, shape = RadixTheme.shapes.circle)
        .padding(horizontal = RadixTheme.dimensions.paddingMedium, vertical = RadixTheme.dimensions.paddingSmall)
    Row(
        modifier = modifier
            .clip(RadixTheme.shapes.circle)
            .applyIf(onClick != null, Modifier.clickable { onClick?.invoke() })
            .applyIf(selected, tagSelectedModifier)
            .applyIf(!selected, tagBorderModifier),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon?.invoke()
        Text(
            text = text,
            style = RadixTheme.typography.body2Header,
            color = if (selected) RadixTheme.colors.white else RadixTheme.colors.gray1
        )
        if (selected) {
            Icon(
                modifier = Modifier
                    .applyIf(
                        onCloseClick != null,
                        Modifier.clickable {
                            onCloseClick?.invoke()
                        }
                    )
                    .size(12.dp),
                painter = painterResource(id = DSR.ic_close),
                contentDescription = null,
                tint = RadixTheme.colors.gray3
            )
        }
    }
}
