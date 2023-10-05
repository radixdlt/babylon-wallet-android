package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
    errorHighlight: Boolean = false,
    leftLabel: LabelType? = null,
    rightLabel: LabelType? = null,
    optionalHint: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    iconToTheRight: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    onFocusChanged: ((FocusState) -> Unit)? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = RadixTheme.typography.body1Regular,
    errorFixedSize: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val isError = error != null
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)) {
        if (leftLabel != null || rightLabel != null) {
            TopLabelRow(leftLabel, rightLabel, isError)
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
                    unfocusedBorderColor = RadixTheme.colors.gray4,
                    errorBorderColor = RadixTheme.colors.red1,
                    cursorColor = RadixTheme.colors.gray1,
                    selectionColors = TextSelectionColors(
                        RadixTheme.colors.gray1,
                        LocalTextSelectionColors.current.backgroundColor
                    ),
                    unfocusedContainerColor = RadixTheme.colors.gray5
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
                visualTransformation = visualTransformation,
                trailingIcon = trailingIcon,
                isError = error != null || errorHighlight,
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

@Composable
private fun TopLabelRow(
    leftLabel: LabelType?,
    rightLabel: LabelType?,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        when (leftLabel) {
            is LabelType.Custom -> {
                Column(modifier = Modifier.weight(1f)) {
                    leftLabel.content()
                }
            }

            is LabelType.Default -> {
                Text(
                    modifier = Modifier.weight(1f),
                    text = leftLabel.value,
                    style = RadixTheme.typography.body1HighImportance,
                    color = if (isError) RadixTheme.colors.red1 else RadixTheme.colors.gray1
                )
            }

            else -> {}
        }
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))
        when (rightLabel) {
            is LabelType.Default -> {
                Text(
                    modifier = Modifier.weight(1f),
                    text = rightLabel.value,
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray2,
                    textAlign = TextAlign.End
                )
            }

            else -> {}
        }
    }
}

sealed interface LabelType {
    data class Default(val value: String) : LabelType
    data class Custom(val content: @Composable () -> Unit) : LabelType
}

@Preview(showBackground = true)
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

@Preview(showBackground = true)
@Composable
fun RadixTextFieldFilled() {
    RadixWalletTheme {
        RadixTextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChanged = {},
            value = "Input Text",
            hint = "Placeholder",
            leftLabel = LabelType.Default("Left Label"),
            rightLabel = LabelType.Default("Right label"),
            optionalHint = "This is a hint text, It should be short and sweet"
        )
    }
}

@Preview(showBackground = true)
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
            rightLabel = LabelType.Default("Abc"),
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
