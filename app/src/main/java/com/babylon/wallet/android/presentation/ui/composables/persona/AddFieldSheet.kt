package com.babylon.wallet.android.presentation.ui.composables.persona

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.model.PersonaFieldKindWrapper
import com.babylon.wallet.android.presentation.model.toDisplayResource
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.profile.data.model.pernetwork.Network

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddFieldSheet(
    onBackClick: () -> Unit,
    fieldsToAdd: ImmutableList<PersonaFieldKindWrapper>,
    onAddFields: () -> Unit,
    onSelectionChanged: (Network.Persona.Field.Kind, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    anyFieldSelected: Boolean
) {
    Column(modifier = modifier) {
        RadixCenteredTopAppBar(
            title = stringResource(id = R.string.add_a_field),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1,
        )
        Divider(color = RadixTheme.colors.gray5)
        LazyColumn(
            contentPadding = PaddingValues(vertical = dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.paddingDefault),
                    text = stringResource(R.string.select_from_the_following),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(dimensions.paddingSmall))
            }
            items(fieldsToAdd, key = { it.kind }) { field ->
                SelectableFieldItem(
                    onSelectionChanged = onSelectionChanged,
                    field = field,
                    modifier = Modifier
                        .animateItemPlacement()
                        .throttleClickable {
                            onSelectionChanged(field.kind, !field.selected)
                        }
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.paddingDefault)
                )
            }
        }
        Divider(color = RadixTheme.colors.gray5)
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingDefault),
            enabled = anyFieldSelected,
            text = stringResource(id = R.string.add),
            onClick = onAddFields,
        )
    }
}

@Composable
private fun SelectableFieldItem(
    onSelectionChanged: (Network.Persona.Field.Kind, Boolean) -> Unit,
    field: PersonaFieldKindWrapper,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = field.kind.toDisplayResource()),
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Checkbox(
            checked = field.selected,
            colors = CheckboxDefaults.colors(
                checkedColor = RadixTheme.colors.gray1,
                uncheckedColor = RadixTheme.colors.gray3,
                checkmarkColor = Color.White
            ),
            onCheckedChange = {
                onSelectionChanged(field.kind, it)
            }
        )
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
            fieldsToAdd = persistentListOf(PersonaFieldKindWrapper(Network.Persona.Field.Kind.GivenName)),
            anyFieldSelected = false
        )
    }
}
