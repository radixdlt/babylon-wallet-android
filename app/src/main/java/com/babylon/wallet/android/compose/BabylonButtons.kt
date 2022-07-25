package com.babylon.wallet.android.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp

@Composable
fun BabylonButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonClicked: () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = { buttonClicked() },
        enabled = enabled,
        // Custom colors for different states
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            disabledBackgroundColor = MaterialTheme.colors.onBackground
                .copy(alpha = 0.2f)
                .compositeOver(MaterialTheme.colors.background)
        )
    ) { Text(text = title, modifier = Modifier.padding(26.dp, 8.dp, 26.dp, 8.dp)) }
}