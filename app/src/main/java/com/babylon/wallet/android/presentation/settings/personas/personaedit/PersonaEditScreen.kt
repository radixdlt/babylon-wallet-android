package com.babylon.wallet.android.presentation.settings.personas.personaedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.LabelType
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.model.PersonaFieldWrapper
import com.babylon.wallet.android.presentation.model.toDisplayResource
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomPrimaryButton
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.persona.AddFieldSheet
import com.babylon.wallet.android.presentation.ui.composables.persona.PersonaDataFieldInput
import com.babylon.wallet.android.presentation.ui.composables.persona.RequiredPersonaInformationInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.PersonaDataEntryID

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
        modifier = modifier,
        persona = state.persona,
        onSave = viewModel::onSave,
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
        dappContextEdit = state.dappContextEdit,
        wasEdited = state.wasEdited,
        missingFields = state.missingFields
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonaEditContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    persona: Network.Persona?,
    onSave: () -> Unit,
    editedFields: ImmutableList<PersonaFieldWrapper>,
    fieldsToAdd: ImmutableList<PersonaFieldWrapper>,
    onAddFields: () -> Unit,
    onSelectionChanged: (PersonaDataEntryID, Boolean) -> Unit,
    onDeleteField: (PersonaDataEntryID) -> Unit,
    onValueChanged: (PersonaDataEntryID, PersonaData.PersonaDataField) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    addButtonEnabled: Boolean,
    personaDisplayName: PersonaDisplayNameFieldWrapper,
    saveButtonEnabled: Boolean,
    onFieldFocusChanged: (PersonaDataEntryID, Boolean) -> Unit,
    onPersonaDisplayNameFocusChanged: (Boolean) -> Unit,
    dappContextEdit: Boolean,
    wasEdited: Boolean,
    missingFields: ImmutableList<PersonaData.PersonaDataField.Kind>
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val bottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showCancelPrompt by remember { mutableStateOf(false) }
    BackHandler {
        when {
            bottomSheetState.isVisible -> {
                scope.launch {
                    bottomSheetState.hide()
                }
            }

            wasEdited -> {
                showCancelPrompt = true
            }

            else -> onBackClick()
        }
    }

    if (showCancelPrompt) {
        BasicPromptAlertDialog(
            finish = {
                if (it) {
                    onBackClick()
                }
                showCancelPrompt = false
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.editPersona_closeConfirmationDialog_message
                    ),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1
                )
            },
            confirmText = stringResource(
                id = R.string.editPersona_closeConfirmationDialog_discardChanges
            ),
            dismissText = stringResource(
                id = R.string.editPersona_closeConfirmationDialog_keepEditing
            )
        )
    }

    persona?.let { selectedPersona ->
        Scaffold(
            topBar = {
                Column {
                    RadixCenteredTopAppBar(
                        title = selectedPersona.displayName,
                        onBackClick = {
                            if (wasEdited) {
                                showCancelPrompt = true
                            } else {
                                onBackClick()
                            }
                        },
                        backIconType = BackIconType.Close,
                        windowInsets = WindowInsets.statusBars
                    )
                    HorizontalDivider(color = RadixTheme.colors.gray5)
                }
            },
            bottomBar = {
                BottomPrimaryButton(
                    onClick = onSave,
                    enabled = saveButtonEnabled,
                    text = stringResource(id = R.string.common_save),
                    modifier = Modifier
                        .imePadding()
                        .navigationBarsPadding()
                        .fillMaxWidth(),
                    buttonPadding = PaddingValues(horizontal = dimensions.paddingDefault),
                )
            },
            containerColor = RadixTheme.colors.defaultBackground
        ) { padding ->
            PersonaDetailList(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                persona = selectedPersona,
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
                dappContextEdit = dappContextEdit,
                missingFields = missingFields
            )
        }
    }

    if (persona == null) {
        FullscreenCircularProgressContent()
    }

    if (bottomSheetState.isVisible) {
        DefaultModalSheetLayout(
            modifier = modifier,
            sheetState = bottomSheetState,
            sheetContent = {
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
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    anyFieldSelected = addButtonEnabled
                )
            }
        )
    }
}

@Composable
private fun PersonaDetailList(
    modifier: Modifier = Modifier,
    persona: Network.Persona,
    onAddField: () -> Unit,
    editedFields: ImmutableList<PersonaFieldWrapper>,
    onDeleteField: (PersonaDataEntryID) -> Unit,
    onValueChanged: (PersonaDataEntryID, PersonaData.PersonaDataField) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    personaDisplayName: PersonaDisplayNameFieldWrapper,
    addButtonEnabled: Boolean,
    onFieldFocusChanged: (PersonaDataEntryID, Boolean) -> Unit,
    onPersonaDisplayNameFocusChanged: (Boolean) -> Unit,
    dappContextEdit: Boolean,
    missingFields: ImmutableList<PersonaData.PersonaDataField.Kind>
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = dimensions.paddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        item {
            Thumbnail.Persona(
                modifier = Modifier
                    .padding(vertical = dimensions.paddingDefault)
                    .size(104.dp),
                persona = persona
            )
//            UnderlineTextButton(
//                text = stringResource(R.string.authorizedDapps_personaDetails_editAvatarButtonTitle),
//                onClick = onEditAvatar
//            )
            Spacer(modifier = Modifier.height(dimensions.paddingSmall))
        }
        item {
            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.paddingDefault),
                onValueChanged = onDisplayNameChanged,
                value = personaDisplayName.value,
                leftLabel = LabelType.Default(stringResource(id = R.string.authorizedDapps_personaDetails_personaLabelHeading)),
                error = if (personaDisplayName.shouldDisplayValidationError && personaDisplayName.valid == false) {
                    stringResource(id = R.string.createPersona_emptyDisplayName)
                } else {
                    null
                },
                onFocusChanged = {
                    onPersonaDisplayNameFocusChanged(it.hasFocus)
                }
            )
            Spacer(modifier = Modifier.height(dimensions.paddingXLarge))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = dimensions.paddingDefault)
            )
        }
        item {
            Spacer(modifier = Modifier.height(dimensions.paddingDefault))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.paddingDefault),
                text = stringResource(R.string.editPersona_sharedInformationHeading),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
            if (missingFields.isNotEmpty()) {
                RequiredPersonaInformationInfo(
                    requiredFields = missingFields,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensions.paddingDefault)
                )
            }
            Spacer(modifier = Modifier.height(dimensions.paddingLarge))
        }
        items(editedFields) { field ->
            val validationError = if (dappContextEdit) {
                stringResource(id = R.string.editPersona_error_requiredByDapp)
            } else {
                stringResource(id = R.string.createPersona_requiredField)
            }
            PersonaDataFieldInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.paddingDefault),
                label = stringResource(id = field.entry.value.kind.toDisplayResource()),
                field = field.entry.value,
                onValueChanged = {
                    onValueChanged(field.id, it)
                },
                onDeleteField = {
                    onDeleteField(field.id)
                },
                onFocusChanged = {
                    onFieldFocusChanged(field.id, it.hasFocus)
                },
                required = field.required,
                phoneInput = field.isPhoneNumber(),
                error = if (field.shouldDisplayValidationError && field.valid == false) {
                    validationError
                } else {
                    null
                },
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.paddingDefault, vertical = dimensions.paddingLarge),
                color = RadixTheme.colors.gray4
            )
        }
        item {
            Spacer(modifier = Modifier.height(dimensions.paddingSmall))
            RadixSecondaryButton(
                text = stringResource(id = R.string.editPersona_addAField),
                onClick = onAddField,
                enabled = addButtonEnabled
            )
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
            dappContextEdit = false,
            wasEdited = false,
            missingFields = persistentListOf()
        )
    }
}
