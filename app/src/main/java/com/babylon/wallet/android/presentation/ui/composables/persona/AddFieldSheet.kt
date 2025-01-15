package com.babylon.wallet.android.presentation.ui.composables.persona

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.model.PersonaFieldWrapper
import com.babylon.wallet.android.presentation.model.empty
import com.babylon.wallet.android.presentation.model.toDisplayResource
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.presentation.ui.none
import com.radixdlt.sargon.PersonaDataEntryId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.sargon.PersonaDataField

@OptIn(ExperimentalFoundationApi::class)
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
                contentColor = RadixTheme.colors.gray1,
                backIconType = BackIconType.Close
            )
            HorizontalDivider(color = RadixTheme.colors.gray4)
        }
    }, bottomBar = {
        RadixBottomBar(
            insets = WindowInsets.none,
            onClick = onAddFields,
            text = stringResource(id = R.string.editPersona_addAField_add),
            enabled = anyFieldSelected
        )
    }, containerColor = RadixTheme.colors.defaultBackground, content = {
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
                    color = RadixTheme.colors.gray2
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
                    color = RadixTheme.colors.gray4
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
            color = RadixTheme.colors.gray1,
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
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(1.dp, color = RadixTheme.colors.gray1, shape = RadixTheme.shapes.roundedRectXSmall)
            .background(if (checked) RadixTheme.colors.gray1 else Color.Transparent, RadixTheme.shapes.roundedRectXSmall)
            .toggleable(value = checked, onValueChange = onCheckedChange)
    ) {
        AnimatedVisibility(visible = checked, enter = fadeIn(), exit = fadeOut()) {
            Icon(
                modifier = Modifier.padding(dimensions.paddingXXXSmall),
                painter = painterResource(id = DSR.ic_check),
                contentDescription = null,
                tint = RadixTheme.colors.white
            )
        }
    }
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
