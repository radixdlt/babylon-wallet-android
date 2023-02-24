package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun StandardOneLineCard(image: String, title: String, modifier: Modifier = Modifier, showChevron: Boolean = true) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        AsyncImage(
            model = image,
            placeholder = painterResource(id = R.drawable.img_placeholder),
            fallback = painterResource(id = R.drawable.img_placeholder),
            error = painterResource(id = R.drawable.img_placeholder),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RadixTheme.shapes.circle)
        )
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (showChevron) {
            Icon(
                painter = painterResource(
                    id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right
                ),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )
        }
    }
}
