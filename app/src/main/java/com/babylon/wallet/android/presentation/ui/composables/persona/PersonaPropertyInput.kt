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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun PersonaPropertyInput(
    modifier: Modifier,
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    onDeleteField: () -> Unit,
    onFocusChanged: ((FocusState) -> Unit)? = null,
    required: Boolean,
    phoneInput: Boolean = false,
    error: String? = null
) {
    val focusManager = LocalFocusManager.current
    RadixTextField(
        modifier = modifier,
        onValueChanged = onValueChanged,
        value = value,
        leftLabel = label,
        iconToTheRight = if (!required) {
            {
                IconButton(onClick = onDeleteField) {
                    Icon(
                        tint = RadixTheme.colors.gray1,
                        contentDescription = null,
                        painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_delete_outline)
                    )
                }
            }
        } else {
            null
        },
        onFocusChanged = onFocusChanged,
        keyboardActions = KeyboardActions(onNext = {
            focusManager.moveFocus(FocusDirection.Next)
        }),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
            keyboardType = if (phoneInput) {
                KeyboardType.Phone
            } else {
                KeyboardOptions.Default.keyboardType
            }
        ),
        rightLabel = if (required) stringResource(id = R.string.required_by_dapp) else null,
        error = error
    )
}
