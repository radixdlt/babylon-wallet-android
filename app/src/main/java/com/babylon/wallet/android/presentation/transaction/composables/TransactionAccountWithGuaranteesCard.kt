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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.math.BigDecimal

@Composable
fun TransactionAccountWithGuaranteesCard(
    tokenAddress: String,
    isTokenXrd: Boolean,
    tokenIconUrl: String,
    tokenSymbol: String?,
    tokenEstimatedQuantity: String,
    tokenGuaranteedQuantity: String,
    modifier: Modifier = Modifier,
    appearanceId: Int,
    accountName: String,
    guaranteePercentValue: String,
    onGuaranteeValueChanged: (String) -> Unit
) {
    Column(
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(AccountGradientList[appearanceId]),
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
                address = tokenAddress,
                textStyle = RadixTheme.typography.body1Regular,
                textColor = RadixTheme.colors.white,
                iconColor = RadixTheme.colors.white
            )
        }

        Column(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .background(
                    color = RadixTheme.colors.white,
                    shape = RadixTheme.shapes.roundedRectBottomMedium
                )
                .padding(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingMedium
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                val placeholder = if (isTokenXrd) {
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
                        model = tokenIconUrl,
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
                    text = tokenSymbol.orEmpty(),
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                            text = stringResource(id = com.babylon.wallet.android.R.string.estimated),
                            style = RadixTheme.typography.body2Link,
                            color = RadixTheme.colors.gray1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                        Text(
                            modifier = Modifier,
                            text = tokenEstimatedQuantity,
                            style = RadixTheme.typography.secondaryHeader,
                            color = RadixTheme.colors.gray1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
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
                            text = tokenGuaranteedQuantity,
                            style = RadixTheme.typography.body2HighImportance,
                            color = RadixTheme.colors.gray2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Divider(Modifier.fillMaxWidth(), 1.dp, RadixTheme.colors.gray4)

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(2f),
                    text = stringResource(id = com.babylon.wallet.android.R.string.set_guaranteed_minimum),
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray1
                )

                IconButton(
                    modifier = Modifier.weight(0.7f),
                    onClick = {
                        val guaranteePercentDecimal = guaranteePercentValue.toBigDecimal()
                        onGuaranteeValueChanged(
                            guaranteePercentDecimal.minus(BigDecimal("0.1")).toString()
                        )
                    }
                ) {
                    Icon(
                        painterResource(
                            id = com.babylon.wallet.android.R.drawable.ic_minus
                        ),
                        tint = RadixTheme.colors.gray1,
                        contentDescription = "minus button"
                    )
                }

                RadixTextField(
                    modifier = Modifier.weight(1.1f),
                    onValueChanged = onGuaranteeValueChanged,
                    value = guaranteePercentValue,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.None,
                        keyboardType = KeyboardType.Number
                    )
                )

                IconButton(
                    modifier = Modifier.weight(0.7f),
                    onClick = {
                        val guaranteePercentDecimal = guaranteePercentValue.toBigDecimal()
                        onGuaranteeValueChanged(
                            guaranteePercentDecimal.plus(BigDecimal("0.1")).toString()
                        )
                    }
                ) {
                    Icon(
                        painterResource(
                            id = com.babylon.wallet.android.R.drawable.ic_plus
                        ),
                        tint = RadixTheme.colors.gray1,
                        contentDescription = "plus button"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    }
}

@Preview("default")
@Preview("large font", fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun TransactionAccountWithGuaranteesCardPreview() {
    RadixWalletTheme {
        TransactionAccountWithGuaranteesCard(
            tokenAddress = "d3d3nd32dko3dko3",
            isTokenXrd = true,
            tokenIconUrl = "",
            tokenSymbol = "XRD",
            tokenEstimatedQuantity = "689.203",
            tokenGuaranteedQuantity = "689.203",
            modifier = Modifier,
            appearanceId = 0,
            accountName = "My main account",
            guaranteePercentValue = "100",
            onGuaranteeValueChanged = {}
        )
    }
}
