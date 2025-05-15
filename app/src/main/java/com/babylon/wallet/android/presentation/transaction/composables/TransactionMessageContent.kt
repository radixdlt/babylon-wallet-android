package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.bubbleShape
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow

@Composable
fun ColumnScope.TransactionMessageContent(
    transactionMessage: String,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val bubbleShape = remember {
        bubbleShape(
            density = density
        )
    }

    Text(
        modifier = Modifier
            .padding(RadixTheme.dimensions.paddingDefault),
        text = stringResource(id = R.string.interactionReview_messageHeading).uppercase(),
        style = RadixTheme.typography.body1Link,
        color = RadixTheme.colors.textSecondary,
        overflow = TextOverflow.Ellipsis,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultCardShadow(
                elevation = 6.dp,
                shape = bubbleShape
            )
            .background(
                color = RadixTheme.colors.background,
                shape = bubbleShape
            )
            .padding(
                vertical = RadixTheme.dimensions.paddingMedium,
                horizontal = RadixTheme.dimensions.paddingDefault
            )
    ) {
        Text(
            text = transactionMessage,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionMessageContentPreview() {
    RadixWalletTheme {
        Column {
            TransactionMessageContent(transactionMessage = "Message")
        }
    }
}
