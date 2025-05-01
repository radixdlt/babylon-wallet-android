package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
@Suppress("CyclomaticComplexMethod")
fun MnemonicWordTextField(
    modifier: Modifier,
    onValueChanged: (String) -> Unit,
    value: String,
    label: String,
    hint: String? = null,
    error: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    onFocusChanged: ((FocusState) -> Unit)? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = RadixTheme.typography.body1Regular,
    errorFixedSize: Boolean = false,
    enabled: Boolean = true,
    masked: Boolean = false,
    highlightField: Boolean = false,
    hasInitialFocus: Boolean = false,
    colors: MnemonicTextFieldColors = MnemonicTextFieldColors.default(),
) {
    var focused by remember { mutableStateOf(hasInitialFocus) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(hasInitialFocus) {
        if (hasInitialFocus) {
            focusRequester.requestFocus()
        }
    }
    Column(
        modifier = modifier
    ) {
        val selectionColors = TextSelectionColors(
            handleColor = RadixTheme.colors.text,
            backgroundColor = RadixTheme.colors.text.copy(alpha = 0.4f)
        )
        val textColor = when {
            error != null -> colors.errorTextColor
            !enabled -> colors.disabledTextColor
            else -> colors.textColor
        }
        val borderColor = when {
            enabled && highlightField -> colors.highlightedBorderColor
            error != null -> colors.errorBorderColor
            focused -> colors.focusedBorderColor
            !enabled -> colors.disabledBorderColor
            else -> colors.borderColor
        }
        val hintColor = when {
            error != null -> colors.errorHintColor
            !enabled -> colors.disabledHintColor
            else -> colors.hintColor
        }
        Text(
            text = label,
            style = RadixTheme.typography.body1HighImportance,
            color = hintColor
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        var textFieldValueState by remember(value) {
            mutableStateOf(
                TextFieldValue(
                    text = value,
                    selection = if (enabled) TextRange(value.length) else TextRange.Zero
                )
            )
        }
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            BasicTextField(
                modifier = Modifier
                    .onFocusChanged {
                        focused = it.hasFocus
                        onFocusChanged?.invoke(it)
                    }
                    .focusRequester(focusRequester),
                value = textFieldValueState,
                onValueChange = {
                    textFieldValueState = it
                    onValueChanged(it.text)
                },
                textStyle = textStyle.copy(color = textColor),
                keyboardActions = keyboardActions,
                keyboardOptions = keyboardOptions,
                singleLine = singleLine,
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = borderColor,
                                shape = RadixTheme.shapes.roundedRectSmall
                            )
                            .background(RadixTheme.colors.backgroundTertiary, RadixTheme.shapes.roundedRectSmall)
                            .padding(RadixTheme.dimensions.paddingMedium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (value.isEmpty() && hint != null) {
                            Text(
                                text = hint,
                                style = RadixTheme.typography.body1Regular,
                                color = hintColor
                            )
                        } else {
                            Box(modifier = Modifier.weight(1f)) {
                                innerTextField()
                            }
                            if (enabled && highlightField.not()) {
                                trailingIcon?.invoke()
                            }
                        }
                    }
                },
                enabled = enabled,
                visualTransformation = if (masked) MnemonicWordVisualTransformation() else VisualTransformation.None,
                cursorBrush = SolidColor(RadixTheme.colors.text)
            )
        }
        if (error != null || errorFixedSize) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXSmall))
            Row(
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (error != null) {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        painter = painterResource(id = R.drawable.ic_warning_error),
                        contentDescription = null,
                        tint = colors.statusMessageColor
                    )
                }
                Text(text = error.orEmpty(), style = RadixTheme.typography.body2Regular, color = colors.statusMessageColor)
            }
        }
    }
}

internal class MnemonicWordVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = buildAnnotatedString {
                repeat(transformationCharactersLength) {
                    append("\u2022")
                }
            },
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = 0

                override fun transformedToOriginal(offset: Int): Int = 0
            }
        )
    }

    companion object {
        private const val transformationCharactersLength: Int = 4
    }
}

data class MnemonicTextFieldColors(
    val textColor: Color,
    val errorTextColor: Color,
    val disabledTextColor: Color,
    val highlightedTextColor: Color,
    val borderColor: Color,
    val focusedBorderColor: Color,
    val errorBorderColor: Color,
    val disabledBorderColor: Color,
    val highlightedBorderColor: Color,
    val hintColor: Color,
    val errorHintColor: Color,
    val disabledHintColor: Color,
    val statusMessageColor: Color
) {

    companion object {

        @Composable
        fun default(): MnemonicTextFieldColors {
            return MnemonicTextFieldColors(
                textColor = RadixTheme.colors.text,
                errorTextColor = RadixTheme.colors.error,
                disabledTextColor = RadixTheme.colors.text, // TODO Theme
                highlightedTextColor = RadixTheme.colors.text,
                borderColor = RadixTheme.colors.backgroundTertiary,
                focusedBorderColor = RadixTheme.colors.border,
                errorBorderColor = RadixTheme.colors.error,
                disabledBorderColor = RadixTheme.colors.backgroundTertiary,
                highlightedBorderColor = RadixTheme.colors.gray1,
                hintColor = RadixTheme.colors.text,
                errorHintColor = RadixTheme.colors.text,
                disabledHintColor = RadixTheme.colors.text,
                statusMessageColor = RadixTheme.colors.error
            )
        }
    }
}

@Preview
@Composable
fun MnemonicWordTextFieldEmptyPreview() {
    RadixWalletTheme {
        MnemonicWordTextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChanged = {},
            value = "",
            label = "Word 1",
            hint = "Placeholder"
        )
    }
}

@Preview
@Composable
fun MnemonicWordTextFieldPreview() {
    RadixWalletTheme {
        MnemonicWordTextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChanged = {},
            value = "Zoo",
            label = "Word 1",
            hint = "Placeholder"
        )
    }
}

@Preview
@Composable
fun MnemonicWordTextFieldErrorPreview() {
    RadixWalletTheme {
        MnemonicWordTextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChanged = {},
            value = "Zoo",
            label = "Word 1",
            hint = "Placeholder",
            error = "Error!"
        )
    }
}

@Preview
@Composable
fun MnemonicWordTextFieldTrailing1Preview() {
    RadixWalletTheme {
        MnemonicWordTextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChanged = {},
            value = "Zoo",
            label = "Word 1",
            hint = "Placeholder",
            trailingIcon = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(
                        id = R.drawable.check_circle_outline
                    ),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
            }
        )
    }
}

@Preview
@Composable
fun MnemonicWordTextFieldTrailing2Preview() {
    RadixWalletTheme {
        MnemonicWordTextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChanged = {},
            value = "Zoo",
            label = "Word 1",
            hint = "Placeholder",
            error = "Error!",
            trailingIcon = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(
                        id = R.drawable.ic_close
                    ),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
            }
        )
    }
}
