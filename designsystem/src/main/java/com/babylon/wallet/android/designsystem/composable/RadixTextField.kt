package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadixTextField(
    modifier: Modifier,
    onValueChanged: (String) -> Unit,
    value: String,
    leftLabel: String? = null,
    hint: String? = null,
    error: String? = null,
    rightLabel: String? = null,
    optionalHint: String? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            leftLabel?.let { label ->
                Text(
                    text = label,
                    style = RadixTheme.typography.body1HighImportance,
                    color = if (error != null) RadixTheme.colors.red1 else RadixTheme.colors.gray1
                )
            }
            rightLabel?.let { hint ->
                Text(text = hint, style = RadixTheme.typography.body1Regular, color = RadixTheme.colors.gray2)
            }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChanged,
            shape = RadixTheme.shapes.roundedRectSmall,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = RadixTheme.colors.gray4,
                placeholderColor = RadixTheme.colors.gray2,
                textColor = RadixTheme.colors.gray1,
                focusedBorderColor = RadixTheme.colors.gray1,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = RadixTheme.colors.red1
            ),
            placeholder = {
                hint?.let { Text(text = it) }
            },
            isError = error != null
        )
        if (error != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    painter = painterResource(id = R.drawable.ic_warning_error),
                    contentDescription = "",
                    tint = RadixTheme.colors.red1
                )
                Text(text = error, style = RadixTheme.typography.body2Regular, color = RadixTheme.colors.red1)
            }
        } else {
            optionalHint?.let { hint ->
                Text(text = hint, style = RadixTheme.typography.body2Regular, color = RadixTheme.colors.gray2)
            }
        }
    }
}

@Preview
@Composable
fun RadixTextField() {
    BabylonWalletTheme {
        RadixTextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChanged = {},
            value = "",
            leftLabel = "Label",
            hint = "Placeholder",
            optionalHint = "This is a hint text, It should be short and sweet"
        )
    }
}

@Preview
@Composable
fun RadixTextFieldFilled() {
    BabylonWalletTheme {
        RadixTextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChanged = {},
            value = "Input Text",
            leftLabel = "Label",
            hint = "Placeholder",
            rightLabel = "7/10",
            optionalHint = "This is a hint text, It should be short and sweet"
        )
    }
}

@Preview
@Composable
fun RadixTextErrorField() {
    BabylonWalletTheme {
        RadixTextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChanged = {},
            value = "",
            leftLabel = "Label",
            hint = "Placeholder",
            error = "Error",
            optionalHint = "This is a hint text, It should be short and sweet"
        )
    }
}