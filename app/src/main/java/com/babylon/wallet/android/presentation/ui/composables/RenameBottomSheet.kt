package com.babylon.wallet.android.presentation.ui.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.utils.Constants.DISPLAY_NAME_MAX_LENGTH

@Composable
fun RenameBottomSheet(
    modifier: Modifier = Modifier,
    renameInput: RenameInput,
    @StringRes titleRes: Int? = null,
    @StringRes subtitleRes: Int? = null,
    @StringRes errorValidationMessageRes: Int? = null,
    @StringRes errorTooLongNameMessageRes: Int? = null,
    onNameChange: (String) -> Unit,
    onUpdateNameClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val inputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        inputFocusRequester.requestFocus()
    }

    BottomSheetDialogWrapper(
        modifier = modifier,
        addScrim = true,
        showDragHandle = true,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = titleRes?.let {
                    stringResource(id = it)
                } ?: stringResource(R.string.empty),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = subtitleRes?.let {
                    stringResource(id = it)
                } ?: stringResource(R.string.empty),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                    .focusRequester(focusRequester = inputFocusRequester),
                onValueChanged = onNameChange,
                keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words),
                value = renameInput.name,
                singleLine = true,
                error = if (renameInput.isNameTooLong) {
                    errorTooLongNameMessageRes?.let {
                        stringResource(it)
                    }
                } else if (renameInput.isNameValid.not()) {
                    errorValidationMessageRes?.let {
                        stringResource(it)
                    }
                } else {
                    null
                },
                hintColor = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
        }

        RadixBottomBar(
            onClick = onUpdateNameClick,
            text = stringResource(R.string.accountSettings_renameAccount_button),
            insets = WindowInsets.navigationBars.union(WindowInsets.ime),
            enabled = renameInput.isNameValid && renameInput.isNameTooLong.not(),
            isLoading = renameInput.isUpdating
        )
    }
}

open class RenameInput(
    open val name: String = "",
    open val isUpdating: Boolean = false
) {

    val isNameValid: Boolean
        get() = name.isNotBlank() && name.count() <= DISPLAY_NAME_MAX_LENGTH

    val isNameTooLong: Boolean
        get() = name.count() > DISPLAY_NAME_MAX_LENGTH
}

@Preview
@Composable
private fun RenameBottomSheetPreview() {
    RadixWalletPreviewTheme {
        RenameBottomSheet(
            renameInput = RenameInput(),
            titleRes = R.string.linkedConnectors_renameConnector_title,
            subtitleRes = R.string.linkedConnectors_renameConnector_subtitle,
            onNameChange = {},
            onUpdateNameClick = {},
            onDismiss = {}
        )
    }
}
