package com.babylon.wallet.android.presentation.settings.personas.personaedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
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
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.persona.AddFieldSheet
import com.babylon.wallet.android.presentation.ui.composables.persona.PersonaDataFieldInput
import com.babylon.wallet.android.presentation.ui.composables.persona.RequiredPersonaInformationInfo
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaDataEntryId
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.core.sargon.IdentifiedEntry
import rdx.works.core.sargon.PersonaDataField

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
        state = state,
        onSave = viewModel::onSave,
        onAddFields = viewModel::onAddFields,
        onSelectionChanged = viewModel::onSelectionChanged,
        onDeleteField = viewModel::onDeleteField,
        onValueChanged = viewModel::onFieldValueChanged,
        onDisplayNameChanged = viewModel::onDisplayNameChanged,
        onFieldFocusChanged = viewModel::onFieldFocusChanged,
        setAddFieldSheetVisible = viewModel::setAddFieldSheetVisible
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonaEditContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: PersonaEditUiState,
    onSave: () -> Unit,
    onAddFields: () -> Unit,
    onSelectionChanged: (PersonaDataEntryId, Boolean) -> Unit,
    onDeleteField: (PersonaDataEntryId) -> Unit,
    onValueChanged: (PersonaDataEntryId, PersonaDataField) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onFieldFocusChanged: (PersonaDataEntryId, Boolean) -> Unit,
    setAddFieldSheetVisible: (Boolean) -> Unit
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

            state.wasEdited -> {
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
            message = {
                Text(
                    text = stringResource(
                        R.string.editPersona_closeConfirmationDialog_message
                    ),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.text
                )
            },
            confirmText = stringResource(
                id = R.string.editPersona_closeConfirmationDialog_discardChanges
            ),
            dismissText = stringResource(
                id = R.string.editPersona_closeConfirmationDialog_keepEditing
            ),
            confirmTextColor = RadixTheme.colors.error
        )
    }

    state.persona?.let { selectedPersona ->
        Scaffold(
            modifier = modifier,
            topBar = {
                Column {
                    RadixCenteredTopAppBar(
                        title = selectedPersona.displayName.value,
                        onBackClick = {
                            if (state.wasEdited) {
                                showCancelPrompt = true
                            } else {
                                onBackClick()
                            }
                        },
                        backIconType = BackIconType.Close,
                        windowInsets = WindowInsets.statusBarsAndBanner
                    )
                    HorizontalDivider(color = RadixTheme.colors.divider)
                }
            },
            bottomBar = {
                RadixBottomBar(
                    onClick = onSave,
                    enabled = state.saveButtonEnabled,
                    text = stringResource(id = R.string.common_save),
                    insets = WindowInsets.navigationBars.union(WindowInsets.ime)
                )
            },
            containerColor = RadixTheme.colors.background
        ) { padding ->
            PersonaDetailList(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                persona = selectedPersona,
                onAddField = {
                    setAddFieldSheetVisible(true)
                    scope.launch {
                        keyboardController?.hide()
                        bottomSheetState.show()
                    }
                },
                editedFields = state.currentFields,
                onDeleteField = onDeleteField,
                onValueChanged = onValueChanged,
                onDisplayNameChanged = onDisplayNameChanged,
                personaDisplayName = state.personaDisplayName,
                addButtonEnabled = state.fieldsToAdd.isNotEmpty(),
                onFieldFocusChanged = onFieldFocusChanged,
                dappContextEdit = state.dappContextEdit,
                missingFields = state.missingFields
            )
        }
    }

    if (state.persona == null) {
        FullscreenCircularProgressContent()
    }

    if (state.isAddFieldBottomSheetVisible) {
        DefaultModalSheetLayout(
            sheetState = bottomSheetState,
            sheetContent = {
                AddFieldSheet(
                    onBackClick = {
                        setAddFieldSheetVisible(false)
                        scope.launch {
                            bottomSheetState.hide()
                        }
                    },
                    fieldsToAdd = state.fieldsToAdd,
                    onAddFields = {
                        setAddFieldSheetVisible(false)
                        scope.launch { bottomSheetState.hide() }
                        onAddFields()
                    },
                    onSelectionChanged = onSelectionChanged,
                    anyFieldSelected = state.addFieldButtonEnabled
                )
            },
            onDismissRequest = {
                setAddFieldSheetVisible(false)
                scope.launch {
                    bottomSheetState.hide()
                }
            }
        )
    }
}

@Composable
private fun PersonaDetailList(
    modifier: Modifier = Modifier,
    persona: Persona,
    onAddField: () -> Unit,
    editedFields: ImmutableList<PersonaFieldWrapper>,
    onDeleteField: (PersonaDataEntryId) -> Unit,
    onValueChanged: (PersonaDataEntryId, PersonaDataField) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    personaDisplayName: PersonaDisplayNameFieldWrapper,
    addButtonEnabled: Boolean,
    onFieldFocusChanged: (PersonaDataEntryId, Boolean) -> Unit,
    dappContextEdit: Boolean,
    missingFields: ImmutableList<PersonaDataField.Kind>
) {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = dimensions.paddingLarge),
        modifier = modifier
    ) {
        item {
            Thumbnail.Persona(
                modifier = Modifier
                    .padding(vertical = dimensions.paddingLarge)
                    .size(104.dp),
                persona = persona
            )
//            UnderlineTextButton(
//                text = stringResource(R.string.authorizedDapps_personaDetails_editAvatarButtonTitle),
//                onClick = onEditAvatar
//            )
            HorizontalDivider(color = RadixTheme.colors.divider)
        }
        item {
            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensions.paddingXLarge),
                onValueChanged = onDisplayNameChanged,
                value = personaDisplayName.value,
                leftLabel = LabelType.Default(stringResource(id = R.string.authorizedDapps_personaDetails_personaLabelHeading)),
                error = if (personaDisplayName.wasEdited) {
                    when (personaDisplayName.validationState) {
                        PersonaDisplayNameFieldWrapper.ValidationState.Empty -> stringResource(id = R.string.createPersona_emptyDisplayName)
                        PersonaDisplayNameFieldWrapper.ValidationState.TooLong -> stringResource(id = R.string.error_personaLabel_tooLong)
                        else -> null
                    }
                } else {
                    null
                }
            )
            HorizontalDivider(color = RadixTheme.colors.divider)
        }
        item {
            Spacer(modifier = Modifier.height(dimensions.paddingLarge))
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(R.string.editPersona_sharedInformationHeading),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.textSecondary
            )
            if (missingFields.isNotEmpty()) {
                Spacer(modifier = Modifier.height(dimensions.paddingDefault))
                RequiredPersonaInformationInfo(
                    requiredFields = missingFields,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(dimensions.paddingLarge))
            HorizontalDivider(color = RadixTheme.colors.divider)
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
                    .padding(vertical = dimensions.paddingXXLarge),
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
                error = if (field.shouldDisplayValidationError && field.isValid == false) {
                    validationError
                } else {
                    null
                },
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth(),
                color = RadixTheme.colors.divider
            )
        }
        item {
            RadixSecondaryButton(
                modifier = Modifier
                    .padding(vertical = dimensions.paddingLarge)
                    .widthIn(min = 200.dp),
                text = stringResource(id = R.string.editPersona_addAField),
                onClick = onAddField,
                enabled = addButtonEnabled
            )
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun DappDetailContentPreview() {
    RadixWalletTheme {
        PersonaEditContent(
            onBackClick = {},
            state = PersonaEditUiState(
                persona = Persona.sampleMainnet(),
                currentFields = persistentListOf(
                    PersonaFieldWrapper(
                        entry = IdentifiedEntry.Companion.init(
                            PersonaDataField.Name(
                                variant = PersonaDataField.Name.Variant.Western,
                                given = "John",
                                family = "Smith",
                                nickname = "JS"
                            )
                        )
                    ),
                    PersonaFieldWrapper(
                        entry = IdentifiedEntry.Companion.init(PersonaDataField.Email("test@test.pl"))
                    ),
                    PersonaFieldWrapper(
                        entry = IdentifiedEntry.Companion.init(PersonaDataField.PhoneNumber("123456789"))
                    )
                ),
                fieldsToAdd = persistentListOf(),
                personaDisplayName = PersonaDisplayNameFieldWrapper("Persona"),
                addFieldButtonEnabled = false,
                saveButtonEnabled = false,
                dappContextEdit = false,
                wasEdited = false,
                missingFields = persistentListOf()
            ),
            onSave = {},
            onAddFields = {},
            onSelectionChanged = { _, _ -> },
            onDeleteField = {},
            onValueChanged = { _, _ -> },
            onDisplayNameChanged = {},
            onFieldFocusChanged = { _, _ -> },
            setAddFieldSheetVisible = {}
        )
    }
}
