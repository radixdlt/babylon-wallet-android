package com.babylon.wallet.android.presentation.account

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.model.TokenUiModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.math.BigDecimal

@Composable
fun TokenItemCard(
    token: TokenUiModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingMedium
            ),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            val placeholder = if (token.isXrd()) {
                painterResource(id = R.drawable.ic_xrd_token)
            } else {
                rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb()))
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(RadixTheme.colors.gray3, shape = RadixTheme.shapes.circle)
            ) {
                AsyncImage(
                    model = token.iconUrl,
                    placeholder = placeholder,
                    fallback = placeholder,
                    error = placeholder,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RadixTheme.shapes.circle)
                )
                if (token.iconUrl == null) {
                    Text(
                        text = token.symbol.orEmpty(),
                        style = RadixTheme.typography.body1HighImportance,
                        modifier = Modifier.align(
                            Alignment.Center
                        )
                    )
                }
            }
            Text(
                modifier = Modifier
                    .weight(0.3f),
                text = token.symbol.orEmpty(),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                modifier = Modifier.weight(0.7f),
                text = token.tokenQuantityToDisplay,
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
    }
}

@Preview("default")
@Preview("large font", fontScale = 2f)
@Composable
fun TokenItemCardPreview() {
    RadixWalletTheme {
        TokenItemCard(
            token = TokenUiModel(
                id = "id",
                name = "name",
                symbol = "symbol",
                tokenQuantity = BigDecimal(1234.5678),
                tokenValue = "token value",
                iconUrl = "icon url",
                description = null,
                address = ""
            )
        )
    }
}

@Preview("default with long name and long values")
@Preview("large font with long name and long values", fontScale = 2f)
@Composable
fun TokenItemCardWithLongNameAndLongValuesPreview() {
    RadixWalletTheme {
        TokenItemCard(
            token = TokenUiModel(
                id = "id",
                name = "a very long name that might cause troubles",
                symbol = "XRD",
                tokenQuantity = BigDecimal(1234567.890123),
                tokenValue = "299238528240295320u9532",
                iconUrl = null,
                description = null,
                address = ""
            )
        )
    }
}
