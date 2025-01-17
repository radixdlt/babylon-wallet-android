package com.babylon.wallet.android.presentation.ui.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.utils.Constants.DISPLAY_NAME_MAX_LENGTH

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    renameInput: RenameInput,
    @StringRes titleRes: Int? = null,
    @StringRes subtitleRes: Int? = null,
    @StringRes errorValidationMessageRes: Int? = null,
    @StringRes errorTooLongNameMessageRes: Int? = null,
    onNameChange: (String) -> Unit,
    onUpdateNameClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    DefaultModalSheetLayout(
        modifier = modifier,
        wrapContent = true,
        enableImePadding = true,
        sheetState = sheetState,
        sheetContent = {
            RenameContent(
                renameInput = renameInput,
                titleRes = titleRes,
                subtitleRes = subtitleRes,
                errorValidationMessageRes = errorValidationMessageRes,
                errorTooLongNameMessageRes = errorTooLongNameMessageRes,
                onNameChange = onNameChange,
                onUpdateNameClick = onUpdateNameClick,
                onDismiss = onDismiss
            )
        },
        onDismissRequest = onDismiss
    )
}

@Composable
private fun RenameContent(
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

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(id = R.string.empty),
            onBackClick = onDismiss,
            backIconType = BackIconType.Close,
            containerColor = RadixTheme.colors.defaultBackground,
            windowInsets = WindowInsets(0.dp)
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = titleRes?.let { stringResource(id = it) } ?: stringResource(R.string.empty),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = subtitleRes?.let { stringResource(id = it) } ?: stringResource(R.string.empty),
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
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
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))
        RadixBottomBar(
            onClick = onUpdateNameClick,
            text = stringResource(R.string.accountSettings_renameAccount_button),
            insets = WindowInsets(0.dp),
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

@Preview(showBackground = true)
@Composable
private fun RenameBottomSheetPreview() {
    RadixWalletPreviewTheme {
        RenameContent(
            renameInput = RenameInput(name = "name"),
            titleRes = R.string.linkedConnectors_renameConnector_title,
            subtitleRes = R.string.linkedConnectors_renameConnector_subtitle,
            onNameChange = {},
            onUpdateNameClick = {},
            onDismiss = {}
        )
    }
}
