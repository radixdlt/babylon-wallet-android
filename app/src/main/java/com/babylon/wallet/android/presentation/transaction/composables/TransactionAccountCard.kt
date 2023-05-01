package com.babylon.wallet.android.presentation.transaction.composables

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.model.TokenUiModel
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Composable
fun TransactionAccountCard(
    tokens: ImmutableList<TokenUiModel>,
    modifier: Modifier = Modifier,
    appearanceId: Int,
    address: String,
    accountName: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(AccountGradientList[appearanceId % AccountGradientList.size]),
                    shape = RadixTheme.shapes.roundedRectTopMedium
                )
                .padding(RadixTheme.dimensions.paddingMedium),
            verticalAlignment = CenterVertically
        ) {
            Text(
                text = accountName,
                style = RadixTheme.typography.body1Header,
                maxLines = 1,
                modifier = Modifier.weight(1f, false),
                color = RadixTheme.colors.white
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
            ActionableAddressView(
                address = address,
                textStyle = RadixTheme.typography.body1Regular,
                textColor = RadixTheme.colors.white,
                iconColor = RadixTheme.colors.white
            )
        }

        tokens.forEachIndexed { index, token ->
            val lastItem = index == tokens.size - 1
            val shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape
            Row(
                verticalAlignment = CenterVertically,
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .background(
                        color = RadixTheme.colors.gray4,
                        shape = shape
                    )
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
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
                        model = rememberImageUrl(fromUrl = token.iconUrl, size = ImageSize.SMALL),
                        placeholder = placeholder,
                        fallback = placeholder,
                        error = placeholder,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RadixTheme.shapes.circle)
                    )
                }
                Text(
                    modifier = Modifier
                        .weight(1f),
                    text = token.symbol.orEmpty(),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier,
                        verticalAlignment = CenterVertically
                    ) {
                        if (token.guaranteedQuantity != null) {
                            Text(
                                modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                                text = stringResource(id = com.babylon.wallet.android.R.string.estimated),
                                style = RadixTheme.typography.body2Link,
                                color = RadixTheme.colors.gray1,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End
                            )
                        }
                        Text(
                            modifier = Modifier,
                            text = if (token.isTokenAmountVisible == true) token.tokenQuantityToDisplay else "",
                            style = RadixTheme.typography.secondaryHeader,
                            color = RadixTheme.colors.gray1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                    token.guaranteedQuantity?.let {
                        Row {
                            Text(
                                modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                                text = stringResource(id = com.babylon.wallet.android.R.string.guaranteed),
                                style = RadixTheme.typography.body2Regular,
                                color = RadixTheme.colors.gray2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End
                            )
                            Text(
                                modifier = Modifier,
                                text = token.guaranteedQuantityToDisplay,
                                style = RadixTheme.typography.body2HighImportance,
                                color = RadixTheme.colors.gray2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview("default")
@Preview("large font", fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun TransactionAccountCardPreview() {
    RadixWalletTheme {
        TransactionAccountCard(
            tokens = persistentListOf(
                TokenUiModel(
                    tokenQuantity = BigDecimal(689.203),
                    address = "d3d3nd32dko3dko3",
                    symbol = "XRD",
                    iconUrl = "",
                    isTokenAmountVisible = true,
                    guaranteedQuantity = BigDecimal(689.203)
                )
            ),
            modifier = Modifier,
            appearanceId = 0,
            address = "d3d3nd32dko3dko3",
            accountName = "My main account"
        )
    }
}
