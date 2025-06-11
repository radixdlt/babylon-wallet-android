package com.babylon.wallet.android.presentation.discover.common.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable

@Composable
fun SimpleListItemView(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .defaultCardShadow()
            .clip(RadixTheme.shapes.roundedRectMedium)
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.card,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .throttleClickable(onClick = onClick)
            .padding(RadixTheme.dimensions.paddingSemiLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon?.invoke()

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingDefault))

        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingDefault))

        trailingIcon?.invoke()
    }
}