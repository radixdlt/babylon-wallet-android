package com.babylon.wallet.android.presentation.settings.personas.createpersona

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.LabelType
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.model.PersonaFieldWrapper
import com.babylon.wallet.android.presentation.model.toDisplayResource
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.persona.AddFieldSheet
import com.babylon.wallet.android.presentation.ui.composables.persona.PersonaDataFieldInput
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.PersonaDataEntryId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.core.sargon.PersonaDataField

@Composable
fun CreatePersonaScreen(
    modifier: Modifier = Modifier,
    viewModel: CreatePersonaViewModel,
    onInfoClick: (GlossaryItem) -> Unit,
    onContinueClick: () -> Unit = {},
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CreatePersonaContent(
        modifier = modifier,
        state = state,
        onPersonaNameChange = viewModel::onDisplayNameChanged,
        onPersonaCreateClick = viewModel::onPersonaCreateClick,
        onSelectionChanged = viewModel::onSelectionChanged,
        onAddFields = viewModel::onAddFields,
        onDeleteField = viewModel::onDeleteField,
        onValueChanged = viewModel::onFieldValueChanged,
        onFieldFocusChanged = viewModel::onFieldFocusChanged,
        onAddFieldSheetVisible = viewModel::setAddFieldSheetVisible,
        onMessageShown = viewModel::onMessageShown,
        onInfoClick = onInfoClick,
        onBackClick = onBackClick,
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                CreatePersonaViewModel.Event.NavigateToCompletion -> onContinueClick()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePersonaContent(
    modifier: Modifier,
    state: CreatePersonaViewModel.State,
    onPersonaNameChange: (String) -> Unit,
    onPersonaCreateClick: () -> Unit,
    onSelectionChanged: (PersonaDataEntryId, Boolean) -> Unit,
    onAddFields: () -> Unit,
    onDeleteField: (PersonaDataEntryId) -> Unit,
    onValueChanged: (PersonaDataEntryId, PersonaDataField) -> Unit,
    onFieldFocusChanged: (PersonaDataEntryId, Boolean) -> Unit,
    onAddFieldSheetVisible: (Boolean) -> Unit,
    onMessageShown: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )
    val bottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = bottomSheetState.isVisible) {
        onAddFieldSheetVisible(false)
        scope.launch {
            bottomSheetState.hide()
            keyboardController?.hide()
        }
    }

    Scaffold(
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onPersonaCreateClick,
                text = stringResource(id = R.string.createPersona_saveAndContinueButtonTitle),
                isLoading = state.isPersonaCreating,
                enabled = state.isContinueButtonEnabled,
                insets = WindowInsets.navigationBars.union(WindowInsets.ime)
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        CreatePersonaContentList(
            onPersonaNameChange = onPersonaNameChange,
            personaName = state.personaDisplayName,
            currentFields = state.currentFields,
            onValueChanged = onValueChanged,
            onDeleteField = onDeleteField,
            addButtonEnabled = state.fieldsToAdd.isNotEmpty(),
            modifier = Modifier.padding(padding),
            onAddFieldClick = {
                onAddFieldSheetVisible(true)
                scope.launch {
                    bottomSheetState.show()
                }
            },
            onFieldFocusChanged = onFieldFocusChanged,
            onInfoClick = onInfoClick
        )
    }

    if (state.isAddFieldBottomSheetVisible) {
        DefaultModalSheetLayout(
            modifier = modifier,
            sheetState = bottomSheetState,
            sheetContent = {
                AddFieldSheet(
                    onBackClick = {
                        onAddFieldSheetVisible(false)
                        scope.launch {
                            bottomSheetState.hide()
                        }
                    },
                    fieldsToAdd = state.fieldsToAdd,
                    onAddFields = {
                        onAddFieldSheetVisible(false)
                        scope.launch { bottomSheetState.hide() }
                        onAddFields()
                    },
                    onSelectionChanged = onSelectionChanged,
                    modifier = Modifier
                        .fillMaxSize(),
                    anyFieldSelected = state.anyFieldSelected
                )
            },
            onDismissRequest = {
                onAddFieldSheetVisible(false)
                scope.launch {
                    bottomSheetState.hide()
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CreatePersonaContentList(
    modifier: Modifier = Modifier,
    personaName: PersonaDisplayNameFieldWrapper,
    currentFields: ImmutableList<PersonaFieldWrapper>,
    onValueChanged: (PersonaDataEntryId, PersonaDataField) -> Unit,
    onDeleteField: (PersonaDataEntryId) -> Unit,
    addButtonEnabled: Boolean,
    onPersonaNameChange: (String) -> Unit,
    onAddFieldClick: () -> Unit,
    onFieldFocusChanged: (PersonaDataEntryId, Boolean) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(
            start = dimensions.paddingLarge,
            end = dimensions.paddingLarge,
            bottom = dimensions.paddingDefault
        )
    ) {
        item {
            Text(
                text = stringResource(id = R.string.createPersona_introduction_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text
            )
            InfoButton(
                modifier = Modifier.padding(
                    horizontal = dimensions.paddingDefault,
                    vertical = dimensions.paddingDefault
                ),
                text = stringResource(id = R.string.infoLink_title_personasLearnAbout),
                onClick = {
                    onInfoClick(GlossaryItem.personas)
                }
            )
            Spacer(modifier = Modifier.height(dimensions.paddingSemiLarge))
            Thumbnail.Persona(
                modifier = Modifier.size(90.dp),
                persona = null
            )
//            UnderlineTextButton(
//                text = stringResource(R.string.authorizedDapps_personaDetails_editAvatarButtonTitle),
//                onClick = onEditAvatar
//            )
            Spacer(modifier = Modifier.height(dimensions.paddingLarge))
            RadixTextField(
                modifier = Modifier.fillMaxWidth(),
                onValueChanged = onPersonaNameChange,
                value = personaName.value,
                leftLabel = LabelType.Default(
                    stringResource(
                        id = R.string.authorizedDapps_personaDetails_personaLabelHeading
                    )
                ),
                hint = stringResource(id = R.string.createPersona_nameNewPersona_placeholder),
                error = if (personaName.wasEdited) {
                    when (personaName.validationState) {
                        PersonaDisplayNameFieldWrapper.ValidationState.Empty -> stringResource(id = R.string.createPersona_emptyDisplayName)
                        PersonaDisplayNameFieldWrapper.ValidationState.TooLong -> stringResource(id = R.string.error_personaLabel_tooLong)
                        else -> null
                    }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(dimensions.paddingMedium))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensions.paddingXLarge),
                text = stringResource(id = R.string.createPersona_explanation_thisWillBeShared),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary
            )
            HorizontalDivider(
                color = RadixTheme.colors.divider,
                modifier = Modifier.padding(bottom = dimensions.paddingSemiLarge)
            )
            Text(
                modifier = Modifier.padding(bottom = dimensions.paddingSemiLarge),
                text = stringResource(id = R.string.createPersona_explanation_someDappsMayRequest),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(dimensions.paddingXXLarge))
        }
        itemsIndexed(currentFields, key = { _, field -> field.id }) { index, field ->
            val spacerHeight = if (currentFields.lastIndex == index) {
                dimensions.paddingXXXLarge
            } else {
                dimensions.paddingLarge
            }
            PersonaDataFieldInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null
                    ),
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
                    stringResource(id = R.string.createPersona_requiredField)
                } else {
                    null
                },
            )
            Spacer(modifier = Modifier.height(spacerHeight))
        }
        item {
            RadixSecondaryButton(
                modifier = Modifier.widthIn(min = 200.dp),
                text = stringResource(id = R.string.editPersona_addAField),
                onClick = onAddFieldClick,
                enabled = addButtonEnabled
            )
        }
    }
}

@Preview
@Composable
fun CreateAccountContentPreviewLight() {
    RadixWalletPreviewTheme {
        CreatePersonaContent(
            state = CreatePersonaViewModel.State(
                currentFields = persistentListOf(),
                fieldsToAdd = persistentListOf(),
                personaDisplayName = PersonaDisplayNameFieldWrapper("Name"),
                anyFieldSelected = false,
                isAddFieldBottomSheetVisible = false
            ),
            onPersonaNameChange = {},
            onPersonaCreateClick = {},
            onBackClick = {},
            modifier = Modifier,
            onSelectionChanged = { _, _ -> },
            onAddFields = {},
            onDeleteField = {},
            onValueChanged = { _, _ -> },
            onFieldFocusChanged = { _, _ -> },
            onAddFieldSheetVisible = {},
            onMessageShown = {},
            onInfoClick = {}
        )
    }
}

@Preview
@Composable
fun CreateAccountContentPreview() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        CreatePersonaContent(
            state = CreatePersonaViewModel.State(
                currentFields = persistentListOf(),
                fieldsToAdd = persistentListOf(),
                personaDisplayName = PersonaDisplayNameFieldWrapper("Name"),
                anyFieldSelected = false,
                isAddFieldBottomSheetVisible = false
            ),
            onPersonaNameChange = {},
            onPersonaCreateClick = {},
            onBackClick = {},
            modifier = Modifier,
            onSelectionChanged = { _, _ -> },
            onAddFields = {},
            onDeleteField = {},
            onValueChanged = { _, _ -> },
            onFieldFocusChanged = { _, _ -> },
            onAddFieldSheetVisible = {},
            onMessageShown = {},
            onInfoClick = {}
        )
    }
}
