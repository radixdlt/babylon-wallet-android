package com.babylon.wallet.android.presentation.settings.personaedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.model.toDisplayResource
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.PersonaRoundedAvatar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.UnderlineTextButton
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.OnNetwork

@Composable
fun PersonaEditScreen(
    viewModel: PersonaEditViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                PersonaEditEvent.PersonaSaved -> onBackClick()
            }
        }
    }
    PersonaEditContent(
        onBackClick = onBackClick,
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        persona = state.persona,
        onSave = viewModel::onSave,
        onEditAvatar = {},
        editedFields = state.currentFields,
        fieldsToAdd = state.fieldsToAdd,
        onAddFields = viewModel::onAddFields,
        onSelectionChanged = viewModel::onSelectionChanged,
        onDeleteField = viewModel::onDeleteField,
        onValueChanged = viewModel::onValueChanged,
        onDisplayNameChanged = viewModel::onDisplayNameChanged,
        addButtonEnabled = state.addButtonEnabled,
        personaDisplayName = state.personaDisplayName,
        saveButtonEnabled = state.saveButtonEnabled
    )
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun PersonaEditContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    persona: OnNetwork.Persona?,
    onSave: () -> Unit,
    onEditAvatar: () -> Unit,
    editedFields: ImmutableList<PersonaFieldKindWrapper>,
    fieldsToAdd: ImmutableList<PersonaFieldKindWrapper>,
    onAddFields: () -> Unit,
    onSelectionChanged: (OnNetwork.Persona.Field.Kind, Boolean) -> Unit,
    onDeleteField: (OnNetwork.Persona.Field.Kind) -> Unit,
    onValueChanged: (OnNetwork.Persona.Field.Kind, String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    addButtonEnabled: Boolean,
    personaDisplayName: String?,
    saveButtonEnabled: Boolean
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = bottomSheetState.isVisible) {
        scope.launch {
            bottomSheetState.hide()
        }
    }
    DefaultModalSheetLayout(modifier = modifier, sheetState = bottomSheetState, sheetContent = {
        AddFieldSheet(
            onBackClick = {
                scope.launch {
                    bottomSheetState.hide()
                }
            },
            fieldsToAdd = fieldsToAdd,
            onAddFields = {
                scope.launch { bottomSheetState.hide() }
                onAddFields()
            },
            onSelectionChanged = onSelectionChanged,
            modifier = Modifier.fillMaxSize(),
            addButtonEnabled = addButtonEnabled
        )
    }) {
        persona?.let { persona ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        RadixTheme.colors.defaultBackground,
                        shape = RadixTheme.shapes.roundedRectTopMedium
                    )
                    .clip(shape = RadixTheme.shapes.roundedRectTopMedium)
            ) {
                Column(Modifier.fillMaxSize()) {
                    RadixCenteredTopAppBar(
                        title = persona.displayName,
                        onBackClick = onBackClick,
                        contentColor = RadixTheme.colors.gray1,
                        backIconType = BackIconType.Cancel,
                        actions = {
                            UnderlineTextButton(
                                text = stringResource(id = R.string.save),
                                onClick = onSave,
                                enabled = saveButtonEnabled
                            )
                        },
                    )
                    Divider(color = RadixTheme.colors.gray5)
                    PersonaDetailList(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        onEditAvatar = onEditAvatar,
                        onAddField = {
                            scope.launch {
                                keyboardController?.hide()
                                bottomSheetState.show()
                            }
                        },
                        editedFields = editedFields,
                        onDeleteField = onDeleteField,
                        onValueChanged = onValueChanged,
                        onDisplayNameChanged = onDisplayNameChanged,
                        personaDisplayName = personaDisplayName,
                        addButtonEnabled = fieldsToAdd.isNotEmpty()
                    )
                }
            }
        }
        if (persona == null) {
            FullscreenCircularProgressContent()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddFieldSheet(
    onBackClick: () -> Unit,
    fieldsToAdd: ImmutableList<PersonaFieldKindWrapper>,
    onAddFields: () -> Unit,
    onSelectionChanged: (OnNetwork.Persona.Field.Kind, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    addButtonEnabled: Boolean
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
            enabled = addButtonEnabled,
            text = stringResource(id = R.string.add),
            onClick = onAddFields,
        )
    }
}

@Composable
private fun SelectableFieldItem(
    onSelectionChanged: (OnNetwork.Persona.Field.Kind, Boolean) -> Unit,
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

@Composable
private fun PersonaDetailList(
    modifier: Modifier = Modifier,
    onEditAvatar: () -> Unit,
    onAddField: () -> Unit,
    editedFields: ImmutableList<PersonaFieldKindWrapper>,
    onDeleteField: (OnNetwork.Persona.Field.Kind) -> Unit,
    onValueChanged: (OnNetwork.Persona.Field.Kind, String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    personaDisplayName: String?,
    addButtonEnabled: Boolean
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = dimensions.paddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.imePadding()
    ) {
        item {
            PersonaRoundedAvatar(
                url = "",
                modifier = Modifier
                    .padding(vertical = dimensions.paddingDefault)
                    .size(104.dp)
            )
            UnderlineTextButton(text = stringResource(R.string.edit_avatar), onClick = onEditAvatar)
            Spacer(modifier = Modifier.height(dimensions.paddingSmall))
        }
        item {
            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.paddingDefault),
                onValueChanged = onDisplayNameChanged,
                value = personaDisplayName.orEmpty(),
                leftLabel = stringResource(id = R.string.persona_label),
            )
            Spacer(modifier = Modifier.height(dimensions.paddingXLarge))
            Divider(
                modifier = Modifier.padding(horizontal = dimensions.paddingDefault)
            )
        }
        item {
            Spacer(modifier = Modifier.height(dimensions.paddingDefault))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.paddingDefault),
                text = stringResource(R.string.the_following_information_can_be_seen),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.height(dimensions.paddingLarge))
        }
        items(editedFields) { field ->
            PersonaPropertyInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.paddingDefault),
                label = stringResource(id = field.kind.toDisplayResource()),
                value = field.value,
                onValueChanged = {
                    onValueChanged(field.kind, it)
                },
                onFocusChanged = {
                },
                onDeleteField = {
                    onDeleteField(field.kind)
                }
            )
            Spacer(modifier = Modifier.height(dimensions.paddingLarge))
        }
        item {
            Spacer(modifier = Modifier.height(dimensions.paddingSmall))
            RadixSecondaryButton(
                text = stringResource(id = R.string.add_a_field),
                onClick = onAddField,
                enabled = addButtonEnabled
            )
            Spacer(modifier = Modifier.height(dimensions.paddingLarge))
        }
    }
}

@Composable
fun PersonaPropertyInput(
    modifier: Modifier,
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    onDeleteField: () -> Unit,
    onFocusChanged: ((FocusState) -> Unit)? = null
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
fun DappDetailContentPreview() {
    RadixWalletTheme {
        PersonaEditContent(
            onBackClick = {},
            persona = null,
            onSave = {},
            onEditAvatar = {},
            editedFields = persistentListOf(),
            fieldsToAdd = persistentListOf(),
            onAddFields = {},
            onSelectionChanged = { _, _ -> },
            onDeleteField = {},
            onValueChanged = { _, _ -> },
            onDisplayNameChanged = {},
            addButtonEnabled = false,
            personaDisplayName = "Persona",
            saveButtonEnabled = false
        )
    }
}
