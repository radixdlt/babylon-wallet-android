package com.babylon.wallet.android.presentation.transaction.composables

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.assets.dashedCircleBorder

@Composable
fun SectionTitle(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Image(
            modifier = Modifier
                .size(24.dp)
                .dashedCircleBorder(RadixTheme.colors.gray3),
            painter = painterResource(id = iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(RadixTheme.colors.gray2),
            contentScale = ContentScale.Inside
        )

        Text(
            text = stringResource(id = titleRes).uppercase(),
            style = RadixTheme.typography.body1Link,
            color = RadixTheme.colors.gray2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
@Preview
private fun SectionTitlePreview() {
    RadixWalletPreviewTheme {
        SectionTitle(
            titleRes = R.string.interactionReview_depositsHeading,
            iconRes = DSR.ic_arrow_received_downward
        )
    }
}
