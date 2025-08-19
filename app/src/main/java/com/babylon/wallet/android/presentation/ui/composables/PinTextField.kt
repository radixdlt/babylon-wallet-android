package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme

@Composable
fun PinTextField(
    title: String,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    pinValue: String = "",
    onPinChange: ((String) -> Unit)? = null,
    onPinComplete: ((String) -> Unit)? = null,
    pinLength: Int = 6,
    imeAction: ImeAction = ImeAction.Done,
    isEnabled: Boolean = true
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = title,
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.text
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        PinTextField(
            modifier = textFieldModifier,
            pinValue = pinValue,
            onPinChange = onPinChange,
            onPinComplete = onPinComplete,
            pinLength = pinLength,
            imeAction = imeAction,
            isEnabled = isEnabled
        )
    }
}

@Composable
fun PinTextField(
    modifier: Modifier = Modifier,
    pinValue: String = "",
    onPinChange: ((String) -> Unit)? = null,
    onPinComplete: ((String) -> Unit)? = null,
    pinLength: Int = 6,
    imeAction: ImeAction = ImeAction.Done,
    isEnabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    var pinValueState by remember(pinValue) { mutableStateOf(pinValue) }

    BasicTextField(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .onFocusChanged {
                isFocused = it.hasFocus
            },
        value = pinValueState,
        cursorBrush = SolidColor(Color.Transparent),
        onValueChange = { newValue ->
            if (newValue.length <= pinLength && newValue.all { it.isDigit() }) {
                pinValueState = newValue
                onPinChange?.invoke(newValue)

                if (newValue.length == pinLength) {
                    onPinComplete?.invoke(newValue)
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = imeAction
        ),
        maxLines = 1,
        singleLine = true,
        enabled = isEnabled
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            repeat(pinLength) { index ->
                val char = pinValue.getOrNull(index)
                val isFilled = char != null
                val isNext = isFocused && index == pinValue.length

                Box(
                    modifier = Modifier
                        .size(
                            width = 42.dp,
                            height = 62.dp
                        )
                        .background(
                            color = RadixTheme.colors.backgroundSecondary,
                            shape = RadixTheme.shapes.roundedRectSmall
                        )
                        .border(
                            width = 1.dp,
                            color = if (isNext) {
                                RadixTheme.colors.text
                            } else {
                                RadixTheme.colors.backgroundTertiary
                            },
                            shape = RadixTheme.shapes.roundedRectSmall
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFilled) "*" else "",
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.text
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun PinTextFieldPreview() {
    RadixWalletPreviewTheme {
        PinTextField(
            onPinChange = {},
            onPinComplete = {}
        )
    }
}
