package com.babylon.wallet.android.designsystem.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
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
    hintColor: Color? = RadixTheme.colors.defaultText,
    error: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    onFocusChanged: ((FocusState) -> Unit)? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = RadixTheme.typography.body1Regular,
    errorFixedSize: Boolean = false,
    enabled: Boolean = true,
    highlightField: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Text(
            text = label,
            style = RadixTheme.typography.body1HighImportance,
            color = hintColor ?: RadixTheme.colors.gray1
        )
        val selectionColors = TextSelectionColors(
            handleColor = RadixTheme.colors.gray1,
            backgroundColor = RadixTheme.colors.gray1.copy(alpha = 0.4f)
        )
        val textColor = when {
            error != null -> RadixTheme.colors.red1
            !enabled -> RadixTheme.colors.gray2
            else -> RadixTheme.colors.gray1
        }
        val borderColor = when {
            enabled && highlightField -> RadixTheme.colors.green1
            error != null -> RadixTheme.colors.red1
            focused -> RadixTheme.colors.gray1
            else -> RadixTheme.colors.gray4
        }
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            BasicTextField(
                modifier = Modifier.onFocusChanged {
                    focused = it.hasFocus
                    onFocusChanged?.invoke(it)
                },
                value = value,
                onValueChange = onValueChanged,
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
                            .background(RadixTheme.colors.gray5, RadixTheme.shapes.roundedRectSmall)
                            .padding(RadixTheme.dimensions.paddingMedium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (value.isEmpty() && hint != null) {
                            Text(
                                text = hint,
                                style = RadixTheme.typography.body1Regular,
                                color = hintColor ?: RadixTheme.colors.gray1
                            )
                        } else {
                            Box(modifier = Modifier.weight(1f)) {
                                innerTextField()
                            }
                            if (enabled) {
                                trailingIcon?.invoke()
                            }
                        }
                    }
                },
                enabled = enabled,
                visualTransformation = if (enabled) VisualTransformation.None else MnemonicWordVisualTransformation()
            )
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
            offsetMapping = OffsetMapping.Identity
        )
    }

    companion object {
        private const val transformationCharactersLength: Int = 4
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
