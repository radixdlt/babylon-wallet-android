package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun RedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RadixTheme.shapes.roundedRectSmall,
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.White,
            containerColor = RadixTheme.colors.red1
        )
    ) {
        Text(
            text = text,
            style = RadixTheme.typography.body1Header,
            maxLines = 1,
        )
    }
}
