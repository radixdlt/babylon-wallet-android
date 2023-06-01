package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun DevelopmentPreviewWrapper(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val insets = WindowInsets.statusBars.asPaddingValues()
    Column(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(RadixTheme.colors.orange2)
                .padding(PaddingValues(RadixTheme.dimensions.paddingSmall))
        ) {
            Spacer(modifier = Modifier.height(insets.calculateTopPadding()))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.common_developerDisclaimerText),
                style = RadixTheme.typography.body2HighImportance,
                color = Color.Black,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            content()
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
