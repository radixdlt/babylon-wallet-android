package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.model.TokenUiModel
import com.babylon.wallet.android.presentation.transaction.TransactionAccountItemUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun DepositAccountContent(
    depositingAccounts: ImmutableList<TransactionAccountItemUiModel>,
    modifier: Modifier = Modifier
) {
    if (depositingAccounts.isNotEmpty()) {
        Row {
            Text(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                text = stringResource(id = R.string.depositing).uppercase(),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2,
                overflow = TextOverflow.Ellipsis,
            )
            StrokeLine()
        }

        Column(
            modifier = modifier
                .padding(vertical = RadixTheme.dimensions.paddingSmall)
                .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
                .background(
                    color = Color.White,
                    shape = RadixTheme.shapes.roundedRectDefault
                )
                .padding(RadixTheme.dimensions.paddingMedium)
        ) {
            depositingAccounts.forEachIndexed { index, account ->
                val lastItem = index == depositingAccounts.size - 1

                TransactionAccountCard(
                    appearanceId = account.appearanceID,
                    token = TokenUiModel(
                        iconUrl = "",
                        name = "",
                        description = "",
                        id = "",
                        symbol = account.tokenSymbol,
                        tokenQuantity = account.tokenQuantity.toBigDecimal(),
                        tokenValue = account.fiatAmount,
                        address = account.address,
                        metadata = emptyMap()
                    ),
                    accountName = account.displayName
                )

                if (!lastItem) {
                    Spacer(
                        modifier = Modifier
                            .height(RadixTheme.dimensions.paddingMedium)
                    )
                }
            }
        }
    }
}

@Composable
fun StrokeLine(
    modifier: Modifier = Modifier,
    height: Dp = 24.dp
) {
    val strokeColor = RadixTheme.colors.gray3
    val strokeWidth = with(LocalDensity.current) { 2.dp.toPx() }
    val strokeInterval = with(LocalDensity.current) { 6.dp.toPx() }
    val lineHeight = with(LocalDensity.current) { height.toPx() }
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeInterval, strokeInterval), 0f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        drawLine(
            color = strokeColor,
            start = Offset(width - 150f, 0f),
            end = Offset(width - 150f, lineHeight),
            strokeWidth = strokeWidth,
            pathEffect = pathEffect
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DepositAccountContentPreview() {
    DepositAccountContent(
        persistentListOf(
            TransactionAccountItemUiModel(
                "account_tdx_19jd32jd3928jd3892jd329",
                "My main account",
                "XRD",
                "200",
                "$1234",
                1,
                ""
            )
        )
    )
}
