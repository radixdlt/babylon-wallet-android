package com.babylon.wallet.android.presentation.discover.common.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.BlogPost
import com.radixdlt.sargon.extensions.toUrl

@Composable
fun BlogPostItemView(
    item: BlogPost?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(241.dp),
        shape = RadixTheme.shapes.roundedRectMedium,
        colors = CardColors(
            containerColor = RadixTheme.colors.background,
            contentColor = RadixTheme.colors.text,
            disabledContainerColor = RadixTheme.colors.background,
            disabledContentColor = RadixTheme.colors.text
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .throttleClickable(
                    onClick = onClick
                )
        ) {
            SubcomposeAsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp)
                    .clip(shape = RadixTheme.shapes.roundedRectTopMedium)
                    .radixPlaceholder(
                        visible = item == null,
                        shape = RadixTheme.shapes.roundedRectTopMedium
                    ),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item?.image)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                loading = {
                    ImagePlaceholderView(enableShimmer = true)
                },
                error = {
                    ImagePlaceholderView(enableShimmer = false)
                },
                contentScale = ContentScale.Crop,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(
                        start = RadixTheme.dimensions.paddingMedium,
                        end = RadixTheme.dimensions.paddingMedium,
                        top = RadixTheme.dimensions.paddingXXSmall,
                        bottom = RadixTheme.dimensions.paddingXXSmall
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(
                            fraction = if (item == null) 0.7f else 1f
                        )
                        .radixPlaceholder(visible = item == null),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = item?.name.orEmpty(),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.text,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(id = R.drawable.ic_external_link),
                        contentDescription = null,
                        tint = RadixTheme.colors.icon
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagePlaceholderView(
    enableShimmer: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp)
            .radixPlaceholder(
                visible = enableShimmer,
                shape = RadixTheme.shapes.roundedRectTopMedium
            )
    )
}

@Composable
@Preview
private fun BlogPostItemPreview() {
    RadixWalletPreviewTheme {
        BlogPostItemView(
            BlogPost(
                name = "MVP Booster Grant Winners: RPFS, XRDegen, Liquify MVP Booster Grant Winners: RPFS, XRDegen, Liquify",
                image = "https://google.com".toUrl(),
                url = "https://google.com".toUrl()
            ),
            onClick = {}
        )
    }
}
