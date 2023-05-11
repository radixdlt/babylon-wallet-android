package com.babylon.wallet.android.presentation.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun TransferMessage(
    modifier: Modifier = Modifier,
    message: String,
    onMessageChanged: (String) -> Unit,
    onMessageClose: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = RadixTheme.colors.gray4,
                shape = RadixTheme.shapes.roundedRectMedium
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingXSmall),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMessageClose) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "clear"
                )
            }
        }

        Divider(Modifier.fillMaxWidth(), 1.dp, RadixTheme.colors.gray4)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = RadixTheme.colors.gray5,
                    shape = RadixTheme.shapes.roundedRectBottomMedium
                )
                .padding(RadixTheme.dimensions.paddingSmall),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadixTextField(
                modifier = Modifier,
                onValueChanged = onMessageChanged,
                value = message,
                hint = "Add a message",
                hintColor = RadixTheme.colors.gray2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransferMessagePreview() {
    RadixWalletTheme {
        TransferMessage(
            modifier = Modifier
                .padding(10.dp)
                .background(color = Color.Gray),
            message = "",
            onMessageChanged = {},
            onMessageClose = {}
        )
    }
}
