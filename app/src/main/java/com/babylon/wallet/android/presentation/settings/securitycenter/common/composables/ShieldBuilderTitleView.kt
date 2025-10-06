package com.babylon.wallet.android.presentation.settings.securitycenter.common.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR

@Composable
fun ShieldBuilderTitleView(
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int,
    title: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            text = title,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.text,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
@Preview
private fun ShieldBuilderTitlePreview() {
    RadixWalletPreviewTheme {
        ShieldBuilderTitleView(
            imageRes = DSR.ic_regular_access_dark,
            title = "Regular Access"
        )
    }
}
