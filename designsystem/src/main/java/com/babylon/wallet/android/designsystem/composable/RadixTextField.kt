package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

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
    optionalHint: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    iconToTheRight: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    onFocusChanged: ((FocusState) -> Unit)? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged {
                        onFocusChanged?.invoke(it)
                    },
                value = value,
                onValueChange = onValueChanged,
                shape = RadixTheme.shapes.roundedRectSmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = RadixTheme.colors.gray5,
                    focusedPlaceholderColor = RadixTheme.colors.gray2,
                    focusedTextColor = RadixTheme.colors.gray1,
                    focusedBorderColor = RadixTheme.colors.gray1,
                    unfocusedBorderColor = RadixTheme.colors.gray3,
                    errorBorderColor = RadixTheme.colors.red1,
                    cursorColor = RadixTheme.colors.gray1,
                    selectionColors = TextSelectionColors(
                        RadixTheme.colors.gray1,
                        LocalTextSelectionColors.current.backgroundColor
                    )
                ),
                placeholder = {
                    hint?.let { Text(text = it, style = RadixTheme.typography.body1Regular) }
                },
                trailingIcon = trailingIcon,
                isError = error != null,
                singleLine = singleLine,
                textStyle = RadixTheme.typography.body1Regular,
                keyboardActions = keyboardActions,
                keyboardOptions = keyboardOptions
            )
            iconToTheRight?.let { icon ->
                Box {
                    icon()
                }
            }
        }
        if (error != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    painter = painterResource(id = R.drawable.ic_warning_error),
                    contentDescription = null,
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
fun RadixTextFieldPreview() {
    RadixWalletTheme {
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
    RadixWalletTheme {
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
    RadixWalletTheme {
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
