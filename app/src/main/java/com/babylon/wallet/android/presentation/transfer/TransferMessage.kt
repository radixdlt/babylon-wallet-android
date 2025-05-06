package com.babylon.wallet.android.presentation.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.composable.RadixTextFieldDefaults
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.PreviewBackgroundType
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme

@Composable
fun TransferMessage(
    modifier: Modifier = Modifier,
    message: String,
    onMessageChanged: (String) -> Unit,
    onMessageClose: () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier
                .padding(
                    horizontal = RadixTheme.dimensions.paddingMedium,
                    vertical = RadixTheme.dimensions.paddingXXSmall
                ),
            text = stringResource(id = R.string.assetTransfer_transactionMessage).uppercase(),
            style = RadixTheme.typography.body1Link,
            color = RadixTheme.colors.textSecondary,
            overflow = TextOverflow.Ellipsis,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = RadixTheme.colors.divider,
                    shape = RadixTheme.shapes.roundedRectMedium
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.background)
                    .padding(RadixTheme.dimensions.paddingXXSmall),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMessageClose) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "clear",
                        tint = RadixTheme.colors.icon
                    )
                }
            }

            HorizontalDivider(Modifier.fillMaxWidth(), 1.dp, RadixTheme.colors.divider)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = RadixTheme.colors.backgroundSecondary,
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
                    hint = stringResource(id = R.string.assetTransfer_header_addMessageButton),
                    colors = RadixTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransferMessagePreview() {
    RadixWalletPreviewTheme(backgroundType = PreviewBackgroundType.PRIMARY) {
        TransferMessage(
            message = "",
            onMessageChanged = {},
            onMessageClose = {}
        )
    }
}
