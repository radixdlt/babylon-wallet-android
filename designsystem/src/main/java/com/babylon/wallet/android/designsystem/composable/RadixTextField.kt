package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.Gray3
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixThemeConfig
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.themedColorTint

@Composable
fun RadixTextField(
    modifier: Modifier = Modifier,
    onValueChanged: (String) -> Unit,
    value: String,
    colors: TextFieldColors? = null,
    hint: String? = null,
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
    enabled: Boolean = true,
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
                colors = colors ?: RadixTextFieldDefaults.colors(),
                placeholder = {
                    hint?.let {
                        Text(
                            text = it,
                            style = RadixTheme.typography.body1Regular,
                            maxLines = if (singleLine) 1 else Int.MAX_VALUE
                        )
                    }
                },
                visualTransformation = visualTransformation,
                trailingIcon = trailingIcon,
                isError = error != null || errorHighlight,
                singleLine = singleLine,
                textStyle = textStyle,
                enabled = enabled,
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
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (error != null) {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        painter = painterResource(id = R.drawable.ic_warning_error),
                        contentDescription = null,
                        tint = RadixTheme.colors.error
                    )
                }
                Text(
                    text = error.orEmpty(),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.error
                )
            }
        } else {
            optionalHint?.let { hint ->
                Text(
                    text = hint,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.textSecondary
                )
            }
        }
    }
}

object RadixTextFieldDefaults {
    @Composable
    fun colors(
        focusedContainerColor: Color = RadixTheme.colors.textFieldBackground,
        unfocusedContainerColor: Color = RadixTheme.colors.textFieldBackground,
        errorContainerColor: Color = RadixTheme.colors.textFieldBackground,
        focusedPlaceholderColor: Color = Gray3,
        unfocusedPlaceholderColor: Color = Gray3,
        errorPlaceholderColor: Color = Gray3,
        focusedTextColor: Color = RadixTheme.colors.text,
        unfocusedTextColor: Color = RadixTheme.colors.text,
        errorTextColor: Color = RadixTheme.colors.text,
        focusedBorderColor: Color = RadixTheme.colors.textFieldFocusedBorder,
        unfocusedBorderColor: Color = RadixTheme.colors.textFieldBorder,
        errorBorderColor: Color = RadixTheme.colors.error,
        disabledContainerColor: Color = RadixTheme.colors.backgroundTertiary,
        disabledBorderColor: Color = RadixTheme.colors.backgroundTertiary,
        disabledPlaceholderColor: Color = Gray3,
        disabledTextColor: Color = Gray3,
        cursorColor: Color = RadixTheme.colors.text,
        selectionColors: TextSelectionColors = TextSelectionColors(
            RadixTheme.colors.text,
            LocalTextSelectionColors.current.backgroundColor
        )
    ) = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = focusedContainerColor,
        unfocusedContainerColor = unfocusedContainerColor,
        errorContainerColor = errorContainerColor,
        focusedPlaceholderColor = focusedPlaceholderColor,
        unfocusedPlaceholderColor = unfocusedPlaceholderColor,
        errorPlaceholderColor = errorPlaceholderColor,
        focusedTextColor = focusedTextColor,
        unfocusedTextColor = unfocusedTextColor,
        errorTextColor = errorTextColor,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        errorBorderColor = errorBorderColor,
        cursorColor = cursorColor,
        selectionColors = selectionColors,
        disabledContainerColor = disabledContainerColor,
        disabledBorderColor = disabledBorderColor,
        disabledPlaceholderColor = disabledPlaceholderColor,
        disabledTextColor = disabledTextColor,
    )
}

@Composable
private fun TopLabelRow(
    leftLabel: LabelType?,
    rightLabel: LabelType?,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)) {
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
                    style = RadixTheme.typography.body1Link,
                    color = if (isError) RadixTheme.colors.error else RadixTheme.colors.text
                )
            }

            else -> {}
        }
        when (rightLabel) {
            is LabelType.Default -> {
                Text(
                    modifier = Modifier.weight(1f),
                    text = rightLabel.value,
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.textSecondary,
                    textAlign = TextAlign.End
                )
            }

            is LabelType.Custom -> {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    rightLabel.content()
                }
            }

            else -> {
            }
        }
    }
}

sealed interface LabelType {
    data class Default(val value: String) : LabelType
    data class Custom(val content: @Composable () -> Unit) : LabelType
}

@Preview
@Composable
fun RadixTextFieldPreviewLight() {
    RadixWalletTheme {
        Box(
            modifier = Modifier
                .background(RadixTheme.colors.background)
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            RadixTextField(
                modifier = Modifier.fillMaxWidth(),
                onValueChanged = {},
                value = "",
                hint = "Placeholder",
                optionalHint = "This is a hint text, It should be short and sweet"
            )
        }
    }
}

@Preview
@Composable
fun RadixTextFieldPreviewDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        Box(
            modifier = Modifier
                .background(RadixTheme.colors.background)
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            RadixTextField(
                modifier = Modifier.fillMaxWidth(),
                onValueChanged = {},
                value = "",
                hint = "Placeholder",
                optionalHint = "This is a hint text, It should be short and sweet"
            )
        }
    }
}

@Preview
@Composable
fun RadixTextFieldFilledLight() {
    RadixWalletTheme {
        Box(
            modifier = Modifier
                .background(RadixTheme.colors.background)
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
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
}

@Preview
@Composable
fun RadixTextFieldFilledDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        Box(
            modifier = Modifier
                .background(RadixTheme.colors.background)
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
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
}

@Preview
@Composable
fun RadixTextErrorFieldLight() {
    RadixWalletTheme {
        Box(
            modifier = Modifier
                .background(RadixTheme.colors.background)
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
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
}

@Preview
@Composable
fun RadixTextErrorFieldDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        Box(
            modifier = Modifier
                .background(RadixTheme.colors.background)
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
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
}

@Preview
@Composable
fun RadixTextFieldWithIconLight() {
    RadixWalletTheme {
        Box(
            modifier = Modifier
                .background(RadixTheme.colors.background)
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
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
                        tint = themedColorTint()
                    )
                },
                errorFixedSize = true,
                singleLine = true
            )
        }
    }
}

@Preview
@Composable
fun RadixTextFieldWithIconDark() {
    RadixWalletTheme(config = RadixThemeConfig(isDarkTheme = true)) {
        Box(
            modifier = Modifier
                .background(RadixTheme.colors.background)
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
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
                        tint = themedColorTint()
                    )
                },
                errorFixedSize = true,
                singleLine = true
            )
        }
    }
}
