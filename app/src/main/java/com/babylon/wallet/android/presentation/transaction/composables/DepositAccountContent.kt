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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.hasCustomizableGuarantees
import kotlinx.collections.immutable.ImmutableList

@Composable
fun DepositAccountContent(
    modifier: Modifier = Modifier,
    to: ImmutableList<AccountWithTransferableResources>,
    promptForGuarantees: () -> Unit,
    showStrokeLine: Boolean
) {
    if (to.isNotEmpty()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
                    .padding(bottom = RadixTheme.dimensions.paddingSmall),
                text = stringResource(id = R.string.transactionReview_depositsHeading).uppercase(),
                style = RadixTheme.typography.body1Link,
                color = RadixTheme.colors.gray2,
                overflow = TextOverflow.Ellipsis,
            )
            if (showStrokeLine) {
                StrokeLine()
            }
        }

        Column(
            modifier = modifier
                .shadow(6.dp, RadixTheme.shapes.roundedRectDefault)
                .background(
                    color = Color.White,
                    shape = RadixTheme.shapes.roundedRectDefault
                )
                .padding(RadixTheme.dimensions.paddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            to.onEachIndexed { index, accountEntry ->
                val lastItem = index == to.size - 1
                TransactionAccountCard(
                    account = accountEntry
                )

                if (!lastItem) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                }
            }

            if (to.hasCustomizableGuarantees()) {
                RadixTextButton(
                    modifier = Modifier
                        .padding(top = RadixTheme.dimensions.paddingXSmall),
                    text = stringResource(id = R.string.transactionReview_customizeGuaranteesButtonTitle),
                    onClick = promptForGuarantees
                )
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
