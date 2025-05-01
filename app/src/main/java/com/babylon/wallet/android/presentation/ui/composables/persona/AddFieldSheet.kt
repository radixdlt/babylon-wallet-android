package com.babylon.wallet.android.presentation.ui.composables.persona

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.Gray1
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.presentation.model.PersonaFieldWrapper
import com.babylon.wallet.android.presentation.model.empty
import com.babylon.wallet.android.presentation.model.toDisplayResource
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.presentation.ui.none
import com.radixdlt.sargon.PersonaDataEntryId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.sargon.PersonaDataField

@Composable
fun AddFieldSheet(
    onBackClick: () -> Unit,
    fieldsToAdd: ImmutableList<PersonaFieldWrapper>,
    onAddFields: () -> Unit,
    onSelectionChanged: (PersonaDataEntryId, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    anyFieldSelected: Boolean
) {
    Scaffold(modifier = modifier, topBar = {
        Column {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.editPersona_addAField_title),
                onBackClick = onBackClick,
                backIconType = BackIconType.Close,
                windowInsets = WindowInsets.none
            )
            HorizontalDivider(color = RadixTheme.colors.divider)
        }
    }, bottomBar = {
        RadixBottomBar(
            insets = WindowInsets.none,
            onClick = onAddFields,
            text = stringResource(id = R.string.editPersona_addAField_add),
            enabled = anyFieldSelected
        )
    }, containerColor = RadixTheme.colors.background, content = {
        LazyColumn(
            contentPadding = PaddingValues(vertical = dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(it)
                .fillMaxWidth()
        ) {
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.paddingDefault),
                    text = stringResource(R.string.editPersona_addAField_subtitle),
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(dimensions.paddingDefault))
            }
            items(fieldsToAdd, key = { it.id }) { field ->
                SelectableFieldItem(
                    onSelectionChanged = onSelectionChanged,
                    field = field,
                    modifier = Modifier
                        .throttleClickable {
                            onSelectionChanged(field.id, !field.selected)
                        }
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.paddingDefault)
                )
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.paddingDefault),
                    color = RadixTheme.colors.divider
                )
            }
        }
    })
}

@Composable
private fun SelectableFieldItem(
    onSelectionChanged: (PersonaDataEntryId, Boolean) -> Unit,
    field: PersonaFieldWrapper,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = field.entry.value.kind.toDisplayResource()),
            style = RadixTheme.typography.body1Link,
            color = RadixTheme.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        AddFieldCheckbox(checked = field.selected) {
            onSelectionChanged(field.id, it)
        }
    }
}

@Composable
private fun AddFieldCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = CheckboxDefaults.colors(
            checkedColor = RadixTheme.colors.text,
            uncheckedColor = RadixTheme.colors.textSecondary,
            checkmarkColor = if (RadixTheme.config.isDarkTheme) {
                Gray1
            } else {
                White
            }
        )
    )
}

@Preview(showBackground = true)
@Composable
fun CreateAccountContentPreview() {
    RadixWalletTheme {
        AddFieldSheet(
            onBackClick = {},
            onAddFields = {},
            onSelectionChanged = { _, _ -> },
            fieldsToAdd = persistentListOf(
                PersonaFieldWrapper(entry = PersonaDataField.Kind.Name.empty()),
                PersonaFieldWrapper(entry = PersonaDataField.Kind.EmailAddress.empty()),
                PersonaFieldWrapper(entry = PersonaDataField.Kind.PhoneNumber.empty())
            ),
            anyFieldSelected = false
        )
    }
}
