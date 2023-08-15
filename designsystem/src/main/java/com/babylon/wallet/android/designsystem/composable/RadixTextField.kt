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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun RadixTextField(
    modifier: Modifier,
    onValueChanged: (String) -> Unit,
    value: String,
    colors: TextFieldColors? = null,
    hint: String? = null,
    hintColor: Color? = RadixTheme.colors.defaultText,
    error: String? = null,
    leftLabel: String? = null,
    leftLabelContent: @Composable (() -> Unit) = {
        Text(
            text = leftLabel.orEmpty(),
            style = RadixTheme.typography.body1HighImportance,
            color = if (error != null) RadixTheme.colors.red1 else RadixTheme.colors.gray1
        )
    },
    rightLabel: String? = null,
    optionalHint: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    iconToTheRight: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    onFocusChanged: ((FocusState) -> Unit)? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = RadixTheme.typography.body1Regular,
    errorFixedSize: Boolean = false
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                leftLabelContent.invoke()
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
                colors = colors ?: OutlinedTextFieldDefaults.colors(
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
                    hint?.let {
                        Text(
                            text = it,
                            style = RadixTheme.typography.body1Regular,
                            color = hintColor ?: RadixTheme.colors.gray1
                        )
                    }
                },
                trailingIcon = trailingIcon,
                isError = error != null,
                singleLine = singleLine,
                textStyle = textStyle,
                keyboardActions = keyboardActions,
                keyboardOptions = keyboardOptions,

            )
            iconToTheRight?.let { icon ->
                Box {
                    icon()
                }
            }
        }
        if (error != null || errorFixedSize) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (error != null) {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        painter = painterResource(id = R.drawable.ic_warning_error),
                        contentDescription = null,
                        tint = RadixTheme.colors.red1
                    )
                }
                Text(text = error.orEmpty(), style = RadixTheme.typography.body2Regular, color = RadixTheme.colors.red1)
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
            hint = "Placeholder",
            error = "Error",
            optionalHint = "This is a hint text, It should be short and sweet"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RadixTextFieldWithIcon() {
    RadixWalletTheme {
        RadixTextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChanged = { },
            value = "casino",
            trailingIcon = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(
                        id = R.drawable.check_circle_outline
                    ),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
            },
            errorFixedSize = true,
            singleLine = true
        )
    }
}
