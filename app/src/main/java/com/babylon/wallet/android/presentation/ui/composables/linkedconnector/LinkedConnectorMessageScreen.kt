package com.babylon.wallet.android.presentation.ui.composables.linkedconnector

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.utils.formattedSpans

@Composable
fun LinkedConnectorMessageScreen(
    title: String,
    message: String,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit,
    modifier: Modifier = Modifier,
    isInProgress: Boolean = false,
    positiveButton: String = stringResource(id = R.string.linkedConnectors_nameNewConnector_saveLinkButtonTitle),
    negativeButton: String = stringResource(id = R.string.common_cancel),
) {
    Column(
        modifier = modifier.then(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
        )
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Image(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.icon_desktop_connection_large
            ),
            contentDescription = null
        )

        Spacer(modifier = Modifier.weight(2f))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.text,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = RadixTheme.dimensions.paddingLarge,
                    end = RadixTheme.dimensions.paddingLarge,
                    bottom = RadixTheme.dimensions.paddingLarge
                ),
            text = message.formattedSpans(SpanStyle(fontWeight = FontWeight.Bold)),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.text,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(8f))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadixSecondaryButton(
                modifier = Modifier.weight(1f),
                text = negativeButton,
                onClick = onNegativeClick
            )

            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))

            RadixPrimaryButton(
                text = positiveButton,
                onClick = onPositiveClick,
                modifier = Modifier.weight(1.5f),
                isLoading = isInProgress
            )
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
    }
}

@Composable
@Preview(showBackground = true)
private fun LinkedConnectorMessagePreview() {
    RadixWalletTheme {
        LinkedConnectorMessageScreen(
            title = "Re-link Connector",
            message = """
            Radix Connector now supports linking multiple phones with one browser.

            To support this feature, we've had to disconnect your existing links – please re-link your Connector(s).
            """.trimIndent(),
            isInProgress = false,
            onPositiveClick = {},
            onNegativeClick = {}
        )
    }
}
