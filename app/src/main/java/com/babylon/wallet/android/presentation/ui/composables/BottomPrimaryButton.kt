package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun BottomPrimaryButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    text: String,
    buttonPadding: PaddingValues = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault)
) {
    Column(modifier = modifier.background(RadixTheme.colors.defaultBackground)) {
        HorizontalDivider(color = RadixTheme.colors.gray5)
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        RadixPrimaryButton(
            text = text,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(buttonPadding),
            enabled = enabled
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    }
}
