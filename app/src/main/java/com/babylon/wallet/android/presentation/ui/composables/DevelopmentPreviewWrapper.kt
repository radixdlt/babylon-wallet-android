package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun DevelopmentPreviewWrapper(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    var isInDevMode by remember {
        mutableStateOf(false)
    }
    Box(modifier = modifier) {
        content(if (isInDevMode) PaddingValues(top = RadixTheme.dimensions.paddingLarge) else PaddingValues())

        if (isInDevMode) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.orange2)
                    .statusBarsPadding()
                    .height(RadixTheme.dimensions.paddingLarge),
                text = stringResource(R.string.common_developerDisclaimerText),
                style = RadixTheme.typography.body2HighImportance,
                color = Color.Black,
                textAlign = TextAlign.Center,
            )
        }

        // TODO to remove
        TextButton(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
            onClick = { isInDevMode = !isInDevMode }
        ) {
            Text(text = if (isInDevMode) "Dev" else "Prod")
        }
    }

}

@Preview(showBackground = true)
@Composable
fun DevelopmentBannerPreview() {
    RadixWalletTheme {
        DevelopmentPreviewWrapper(modifier = Modifier, content = {})
    }
}
