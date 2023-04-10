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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.math.BigDecimal

@Composable
fun TransactionAccountCard(
    token: TokenUiModel,
    modifier: Modifier = Modifier,
    appearanceId: Int,
    accountName: String
) {
    Column(
        modifier = modifier
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
            verticalAlignment = Alignment.CenterVertically
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
                address = token.address,
                textStyle = RadixTheme.typography.body1Regular,
                textColor = RadixTheme.colors.gray1,
                iconColor = RadixTheme.colors.gray2
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .background(
                    color = RadixTheme.colors.gray4,
                    shape = RadixTheme.shapes.roundedRectBottomMedium
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
            // TODO no fiat value for now
//            Column(
//                modifier = Modifier.weight(0.7f),
//                horizontalAlignment = Alignment.End
//            ) {
            Text(
                modifier = Modifier.weight(0.7f),
                text = token.tokenQuantityToDisplay,
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
//                Text(
//                    modifier = Modifier.weight(1f),
//                    text = token.tokenValue.orEmpty(),
//                    style = RadixTheme.typography.body2HighImportance,
//                    color = RadixTheme.colors.gray2,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis,
//                    textAlign = TextAlign.End
//                )
//            }
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
            token = TokenUiModel(
                "",
                "",
                "",
                "XRD",
                BigDecimal(689.203),
                "1023",
                "",
                "d3d3nd32dko3dko3",
                mapOf()
            ),
            modifier = Modifier,
            appearanceId = 0,
            accountName = "My main account"
        )
    }
}
