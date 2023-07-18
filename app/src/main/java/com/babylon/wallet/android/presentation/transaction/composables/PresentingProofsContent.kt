package com.babylon.wallet.android.presentation.transaction.composables

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Badge
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.collections.immutable.ImmutableList

@Composable
fun PresentingProofsContent(
    badges: ImmutableList<Badge>,
    modifier: Modifier = Modifier
) {
    if (badges.isNotEmpty()) {
        Column(modifier = modifier) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = RadixTheme.colors.gray4
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            Row(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            ) {
                Text(
                    text = stringResource(id = R.string.transactionReview_presentingHeading).uppercase(),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_info_outline),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray3
                )
            }

            Column(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingMedium)
            ) {
                badges.forEachIndexed { index, badge ->
                    val lastItem = index == badges.lastIndex
                    Row(
                        modifier = Modifier
                            .padding(RadixTheme.dimensions.paddingMedium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
                    ) {
                        val placeholder =
                            rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb()))
                        AsyncImage(
                            model = rememberImageUrl(fromUrl = badge.icon.toString(), size = ImageSize.SMALL),
                            placeholder = placeholder,
                            fallback = placeholder,
                            error = placeholder,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RadixTheme.shapes.circle)
                        )
                        Text(
                            text = badge.name.orEmpty(),
                            style = RadixTheme.typography.body1HighImportance,
                            color = RadixTheme.colors.gray1
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (!lastItem) {
                        Divider(
                            modifier = Modifier
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                                .fillMaxWidth(),
                            thickness = 1.dp,
                            color = RadixTheme.colors.gray4
                        )
                    }
                }
            }
        }
    }
}
