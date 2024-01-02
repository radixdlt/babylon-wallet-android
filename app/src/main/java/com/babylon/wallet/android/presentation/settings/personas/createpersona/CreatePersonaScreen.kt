package com.babylon.wallet.android.presentation.settings.personas.createpersona

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.LabelType
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.model.PersonaFieldWrapper
import com.babylon.wallet.android.presentation.model.toDisplayResource
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.persona.AddFieldSheet
import com.babylon.wallet.android.presentation.ui.composables.persona.PersonaDataFieldInput
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.PersonaDataEntryID

@Composable
fun CreatePersonaScreen(
    viewModel: CreatePersonaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onContinueClick: (
        personaId: String
    ) -> Unit = { _: String -> },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) {
        FullscreenCircularProgressContent()
    } else {
        CreatePersonaContent(
            state = state,
            onPersonaNameChange = viewModel::onDisplayNameChanged,
            onPersonaCreateClick = viewModel::onPersonaCreateClick,
            onBackClick = onBackClick,
            modifier = modifier,
            onSelectionChanged = viewModel::onSelectionChanged,
            onAddFields = viewModel::onAddFields,
            onDeleteField = viewModel::onDeleteField,
            onValueChanged = viewModel::onFieldValueChanged,
            onFieldFocusChanged = viewModel::onFieldFocusChanged,
            onPersonaDisplayNameFocusChanged = viewModel::onPersonaDisplayNameFieldFocusChanged,
            onAddFieldSheetVisible = viewModel::setAddFieldSheetVisible
        )
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is CreatePersonaEvent.Complete -> onContinueClick(
                    event.personaId
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePersonaContent(
    state: CreatePersonaViewModel.CreatePersonaUiState,
    onPersonaNameChange: (String) -> Unit,
    onPersonaCreateClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier,
    onSelectionChanged: (PersonaDataEntryID, Boolean) -> Unit,
    onAddFields: () -> Unit,
    onDeleteField: (PersonaDataEntryID) -> Unit,
    onValueChanged: (PersonaDataEntryID, PersonaData.PersonaDataField) -> Unit,
    onFieldFocusChanged: (PersonaDataEntryID, Boolean) -> Unit,
    onPersonaDisplayNameFocusChanged: (Boolean) -> Unit,
    onAddFieldSheetVisible: (Boolean) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
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
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            Column {
                val context = LocalContext.current

                HorizontalDivider(color = RadixTheme.colors.gray5)
                RadixPrimaryButton(
                    text = stringResource(id = R.string.createPersona_saveAndContinueButtonTitle),
                    onClick = {
                        context.findFragmentActivity()?.let { activity ->
                            activity.biometricAuthenticate { authenticatedSuccessfully ->
                                if (authenticatedSuccessfully) {
                                    onPersonaCreateClick()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensions.paddingDefault)
                        .navigationBarsPadding()
                        .imePadding(),
                    enabled = state.continueButtonEnabled
                )
            }
        },
        containerColor = RadixTheme.colors.defaultBackground
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
            onPersonaDisplayNameFocusChanged = onPersonaDisplayNameFocusChanged,
            onFieldFocusChanged = onFieldFocusChanged
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
                        .fillMaxSize()
                        .navigationBarsPadding(),
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
    onPersonaNameChange: (String) -> Unit,
    personaName: PersonaDisplayNameFieldWrapper,
    currentFields: ImmutableList<PersonaFieldWrapper>,
    onValueChanged: (PersonaDataEntryID, PersonaData.PersonaDataField) -> Unit,
    onDeleteField: (PersonaDataEntryID) -> Unit,
    addButtonEnabled: Boolean,
    modifier: Modifier = Modifier,
    onAddFieldClick: () -> Unit,
    onFieldFocusChanged: (PersonaDataEntryID, Boolean) -> Unit,
    onPersonaDisplayNameFocusChanged: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(
            horizontal = dimensions.paddingLarge,
            vertical = dimensions.paddingDefault
        )
    ) {
        item {
            Text(
                text = stringResource(id = R.string.createPersona_introduction_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
//            Spacer(modifier = Modifier.height(dimensions.paddingSmall))
//            InfoLink(
//                stringResource(R.string.createPersona_introduction_learnAboutPersonas),
//                modifier = Modifier
//                    .padding(horizontal = dimensions.paddingDefault)
//            )
            Spacer(modifier = Modifier.height(30.dp))
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
                onFocusChanged = {
                    onPersonaDisplayNameFocusChanged(it.hasFocus)
                },
                error = if (personaName.shouldDisplayValidationError && personaName.valid == false) {
                    stringResource(id = R.string.createPersona_emptyDisplayName)
                } else {
                    null
                },
            )
            Spacer(modifier = Modifier.height(dimensions.paddingMedium))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.createPersona_explanation_thisWillBeShared),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.height(dimensions.paddingDefault))
        }
        items(currentFields, key = { it.id }) { field ->
            PersonaDataFieldInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItemPlacement(),
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
                    stringResource(id = R.string.createPersona_requiredField)
                } else {
                    null
                },
            )
            Spacer(modifier = Modifier.height(dimensions.paddingLarge))
        }
        item {
            HorizontalDivider(color = RadixTheme.colors.gray5)
            Spacer(modifier = Modifier.height(dimensions.paddingDefault))
            Text(
                text = stringResource(id = R.string.createPersona_explanation_someDappsMayRequest),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
            Spacer(Modifier.height(30.dp))
            RadixSecondaryButton(
                text = stringResource(id = R.string.editPersona_addAField),
                onClick = onAddFieldClick,
                enabled = addButtonEnabled
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateAccountContentPreview() {
    RadixWalletTheme {
        CreatePersonaContent(
            state = CreatePersonaViewModel.CreatePersonaUiState(
                currentFields = persistentListOf(),
                fieldsToAdd = persistentListOf(),
                personaDisplayName = PersonaDisplayNameFieldWrapper("Name"),
                continueButtonEnabled = false,
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
            onPersonaDisplayNameFocusChanged = {},
            onAddFieldSheetVisible = {}
        )
    }
}
