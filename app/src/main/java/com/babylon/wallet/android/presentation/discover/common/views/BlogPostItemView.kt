package com.babylon.wallet.android.presentation.discover.common.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.BlogPost

@Composable
fun BlogPostItemView(
    item: BlogPost,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
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
                    .clip(shape = RadixTheme.shapes.roundedRectTopMedium),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.image)
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

            Text(
                modifier = Modifier.padding(
                    start = RadixTheme.dimensions.paddingMedium,
                    end = RadixTheme.dimensions.paddingSmall,
                    top = RadixTheme.dimensions.paddingDefault
                ),
                text = item.name,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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