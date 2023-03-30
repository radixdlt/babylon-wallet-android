package com.babylon.wallet.android.presentation.settings.personaedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.model.PersonaFieldKindWrapper
import com.babylon.wallet.android.presentation.model.toDisplayResource
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.PersonaRoundedAvatar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.UnderlineTextButton
import com.babylon.wallet.android.presentation.ui.composables.persona.AddFieldSheet
import com.babylon.wallet.android.presentation.ui.composables.persona.PersonaPropertyInput
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network

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
        onValueChanged = viewModel::onFieldValueChanged,
        onDisplayNameChanged = viewModel::onDisplayNameChanged,
        addButtonEnabled = state.addFieldButtonEnabled,
        personaDisplayName = state.personaDisplayName,
        saveButtonEnabled = state.saveButtonEnabled,
        onFieldFocusChanged = viewModel::onFieldFocusChanged,
        onPersonaDisplayNameFocusChanged = viewModel::onPersonaDisplayNameFieldFocusChanged,
        dappContextEdit = state.dappContextEdit
    )
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun PersonaEditContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    persona: Network.Persona?,
    onSave: () -> Unit,
    onEditAvatar: () -> Unit,
    editedFields: ImmutableList<PersonaFieldKindWrapper>,
    fieldsToAdd: ImmutableList<PersonaFieldKindWrapper>,
    onAddFields: () -> Unit,
    onSelectionChanged: (Network.Persona.Field.Kind, Boolean) -> Unit,
    onDeleteField: (Network.Persona.Field.Kind) -> Unit,
    onValueChanged: (Network.Persona.Field.Kind, String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    addButtonEnabled: Boolean,
    personaDisplayName: PersonaDisplayNameFieldWrapper,
    saveButtonEnabled: Boolean,
    onFieldFocusChanged: (Network.Persona.Field.Kind, Boolean) -> Unit,
    onPersonaDisplayNameFocusChanged: (Boolean) -> Unit,
    dappContextEdit: Boolean
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
            anyFieldSelected = addButtonEnabled
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
                        addButtonEnabled = fieldsToAdd.isNotEmpty(),
                        onFieldFocusChanged = onFieldFocusChanged,
                        onPersonaDisplayNameFocusChanged = onPersonaDisplayNameFocusChanged,
                        dappContextEdit = dappContextEdit
                    )
                }
            }
        }
        if (persona == null) {
            FullscreenCircularProgressContent()
        }
    }
}

@Composable
private fun PersonaDetailList(
    modifier: Modifier = Modifier,
    onEditAvatar: () -> Unit,
    onAddField: () -> Unit,
    editedFields: ImmutableList<PersonaFieldKindWrapper>,
    onDeleteField: (Network.Persona.Field.Kind) -> Unit,
    onValueChanged: (Network.Persona.Field.Kind, String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    personaDisplayName: PersonaDisplayNameFieldWrapper,
    addButtonEnabled: Boolean,
    onFieldFocusChanged: (Network.Persona.Field.Kind, Boolean) -> Unit,
    onPersonaDisplayNameFocusChanged: (Boolean) -> Unit,
    dappContextEdit: Boolean
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
                value = personaDisplayName.value,
                leftLabel = stringResource(id = R.string.persona_label),
                error = if (personaDisplayName.shouldDisplayValidationError && personaDisplayName.valid == false) {
                    stringResource(id = R.string.empty_display_name)
                } else {
                    null
                },
                onFocusChanged = {
                    onPersonaDisplayNameFocusChanged(it.hasFocus)
                }
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
            val validationError = if (dappContextEdit) {
                stringResource(id = R.string.required_field_for_this_dapp)
            } else {
                stringResource(id = R.string.required_field)
            }
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
                    onFieldFocusChanged(field.kind, it.hasFocus)
                },
                onDeleteField = {
                    onDeleteField(field.kind)
                },
                required = field.required,
                error = if (field.shouldDisplayValidationError && field.valid == false) {
                    validationError
                } else {
                    null
                },
                phoneInput = field.isPhoneNumber()
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
            personaDisplayName = PersonaDisplayNameFieldWrapper("Persona"),
            saveButtonEnabled = false,
            onFieldFocusChanged = { _, _ -> },
            onPersonaDisplayNameFocusChanged = {},
            dappContextEdit = false
        )
    }
}
