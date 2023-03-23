package com.babylon.wallet.android.presentation.ui.composables.persona

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun PersonaPropertyInput(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    onDeleteField: () -> Unit,
    onFocusChanged: ((FocusState) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    RadixTextField(
        modifier = modifier,
        onValueChanged = onValueChanged,
        value = value,
        leftLabel = label,
        iconToTheRight = {
            IconButton(onClick = onDeleteField) {
                Icon(
                    tint = RadixTheme.colors.gray1,
                    contentDescription = null,
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_delete_outline)
                )
            }
        },
        onFocusChanged = onFocusChanged,
        keyboardActions = KeyboardActions(onNext = {
            focusManager.moveFocus(FocusDirection.Next)
        }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
}

@Preview(showBackground = true)
@Composable
fun PersonaPropertyInputPreview() {
    RadixWalletTheme {
        PersonaPropertyInput(
            label = "Test label",
            value = "Value",
            onValueChanged = {},
            onDeleteField = {},
            onFocusChanged = {}
        )
    }
}
