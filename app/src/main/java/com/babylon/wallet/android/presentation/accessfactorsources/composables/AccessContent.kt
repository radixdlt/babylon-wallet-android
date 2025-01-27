package com.babylon.wallet.android.presentation.accessfactorsources.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun AccessContent(
    modifier: Modifier = Modifier,
    title: String,
    message: AnnotatedString,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                .size(81.dp),
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.ic_security_key
            ),
            contentDescription = null,
            tint = RadixTheme.colors.gray3
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = title,
            color = RadixTheme.colors.gray1,
            style = RadixTheme.typography.title,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = message,
            color = RadixTheme.colors.gray1,
            style = RadixTheme.typography.body1Regular,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        content()
    }
}

@Composable
fun AccessContentRetryButton(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    RadixTextButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
            .height(50.dp),
        text = stringResource(R.string.common_retry),
        enabled = isEnabled,
        onClick = onClick
    )
}
