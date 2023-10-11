package com.babylon.wallet.android.presentation.ui.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.Red1
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable

@Composable
fun DefaultSettingsItem(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    subtitleView: @Composable (ColumnScope.() -> Unit)? = null,
    iconView: @Composable (BoxScope.() -> Unit)? = null,
    showNotificationDot: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(RadixTheme.colors.defaultBackground)
            .throttleClickable(onClick = onClick)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        iconView?.let { icon ->
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Center
            ) {
                icon()

                if (showNotificationDot) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd),
                        backgroundColor = Red1
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .height(IntrinsicSize.Max),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = RadixTheme.typography.body2Header,
                color = RadixTheme.colors.gray1
            )
            subtitleView?.let { subtitle ->
                subtitle()
            }
        }
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RadixTheme.colors.gray1
        )
    }
}

@Composable
fun DefaultSettingsItem(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    showNotificationDot: Boolean = false
) {
    DefaultSettingsItem(
        modifier = modifier,
        title = title,
        onClick = onClick,
        subtitleView = subtitle?.let {
            {
                Text(
                    text = it,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )
            }
        },
        iconView = icon?.let {
            {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = it),
                    contentDescription = null
                )
            }
        },
        showNotificationDot = showNotificationDot
    )
}
