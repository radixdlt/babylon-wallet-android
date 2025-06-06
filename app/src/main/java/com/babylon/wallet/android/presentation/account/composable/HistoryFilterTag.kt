package com.babylon.wallet.android.presentation.account.composable

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
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.modifier.applyIf

@Composable
fun HistoryFilterTag(
    modifier: Modifier = Modifier,
    selected: Boolean,
    text: String,
    showCloseIcon: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null
) {
    val tagBorderModifier = Modifier
        .border(1.dp, RadixTheme.colors.divider, shape = RadixTheme.shapes.circle)
        .padding(horizontal = RadixTheme.dimensions.paddingMedium, vertical = RadixTheme.dimensions.paddingSmall)
    val tagSelectedModifier = Modifier
        .background(
            color = RadixTheme.colors.chipBackground,
            shape = RadixTheme.shapes.circle
        )
        .padding(horizontal = RadixTheme.dimensions.paddingMedium, vertical = RadixTheme.dimensions.paddingSmall)

    val contentColor = if (selected) {
        if (RadixTheme.config.isDarkTheme) RadixTheme.colors.text else White
    } else {
        RadixTheme.colors.text
    }
    Row(
        modifier = modifier
            .clip(RadixTheme.shapes.circle)
            .applyIf(
                onClick != null,
                Modifier.clickable {
                    onClick?.invoke()
                }
            )
            .applyIf(selected, tagSelectedModifier)
            .applyIf(!selected, tagBorderModifier),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon?.invoke()
        Text(
            text = text,
            style = RadixTheme.typography.body1HighImportance,
            color = contentColor
        )
        if (showCloseIcon) {
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
                tint = contentColor
            )
        }
    }
}
