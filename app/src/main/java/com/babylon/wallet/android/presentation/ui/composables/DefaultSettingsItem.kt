package com.babylon.wallet.android.presentation.ui.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable

@Composable
fun DefaultSettingsItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null
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
        icon?.let {
            Icon(painter = painterResource(id = it), contentDescription = null)
        }
        Column(modifier = Modifier.height(IntrinsicSize.Max), verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1
            )
            subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RadixTheme.colors.gray1
        )
    }
}
