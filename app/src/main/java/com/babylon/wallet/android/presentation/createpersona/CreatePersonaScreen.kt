package com.babylon.wallet.android.presentation.createpersona

import android.graphics.drawable.ColorDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.model.PersonaFieldKindWrapper
import com.babylon.wallet.android.presentation.model.toDisplayResource
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.UnderlineTextButton
import com.babylon.wallet.android.presentation.ui.composables.persona.AddFieldSheet
import com.babylon.wallet.android.presentation.ui.composables.persona.PersonaPropertyInput
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun CreatePersonaScreen(
    viewModel: CreatePersonaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onContinueClick: (
        personaId: String
    ) -> Unit = { _: String -> },
) {
    if (viewModel.state.loading) {
        FullscreenCircularProgressContent()
    } else {
        val state = viewModel.state

        CreatePersonaContent(
            onPersonaNameChange = viewModel::onDisplayNameChanged,
            onPersonaCreateClick = viewModel::onPersonaCreateClick,
            personaName = state.personaDisplayName,
            continueButtonEnabled = state.continueButtonEnabled,
            onBackClick = onBackClick,
            isDeviceSecure = state.isDeviceSecure,
            modifier = modifier,
            fieldsToAdd = state.fieldsToAdd,
            currentFields = state.currentFields,
            anyFieldSelected = state.anyFieldSelected,
            onSelectionChanged = viewModel::onSelectionChanged,
            onAddFields = viewModel::onAddFields,
            onDeleteField = viewModel::onDeleteField,
            onValueChanged = viewModel::onFieldValueChanged
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

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun CreatePersonaContent(
    onPersonaNameChange: (String) -> Unit,
    onPersonaCreateClick: () -> Unit,
    personaName: String,
    continueButtonEnabled: Boolean,
    onBackClick: () -> Unit,
    isDeviceSecure: Boolean,
    modifier: Modifier,
    fieldsToAdd: ImmutableList<PersonaFieldKindWrapper>,
    currentFields: ImmutableList<PersonaFieldKindWrapper>,
    anyFieldSelected: Boolean,
    onSelectionChanged: (Network.Persona.Field.Kind, Boolean) -> Unit,
    onAddFields: () -> Unit,
    onDeleteField: (Network.Persona.Field.Kind) -> Unit,
    onValueChanged: (Network.Persona.Field.Kind, String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = bottomSheetState.isVisible) {
        scope.launch {
            bottomSheetState.hide()
            keyboardController?.hide()
        }
    }
    DefaultModalSheetLayout(
        modifier = modifier
            .navigationBarsPadding()
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize(),
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
                modifier = Modifier.fillMaxSize(),
                anyFieldSelected = anyFieldSelected
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            var showNotSecuredDialog by remember { mutableStateOf(false) }
            val context = LocalContext.current

            IconButton(onClick = onBackClick) {
                Icon(
                    painterResource(id = R.drawable.ic_arrow_back),
                    tint = RadixTheme.colors.gray1,
                    contentDescription = "navigate back"
                )
            }
            CreatePersonaContentList(
                onPersonaNameChange = onPersonaNameChange,
                personaName = personaName,
                currentFields = currentFields,
                onValueChanged = onValueChanged,
                onDeleteField = onDeleteField,
                addButtonEnabled = fieldsToAdd.isNotEmpty(),
                modifier = Modifier.weight(1f),
                onAddFieldClick = {
                    scope.launch {
                        bottomSheetState.show()
                    }
                },
                onEditAvatar = {}
            )
            Spacer(modifier = Modifier.height(dimensions.paddingDefault))
            Divider(color = RadixTheme.colors.gray5)
            RadixPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensions.paddingDefault)
                    .imePadding(),
                onClick = {
                    if (isDeviceSecure) {
                        context.findFragmentActivity()?.let { activity ->
                            activity.biometricAuthenticate(true) { authenticatedSuccessfully ->
                                if (authenticatedSuccessfully) {
                                    onPersonaCreateClick()
                                }
                            }
                        }
                    } else {
                        showNotSecuredDialog = true
                    }
                },
                enabled = continueButtonEnabled,
                text = stringResource(id = com.babylon.wallet.android.R.string.save_and_continue)
            )
            if (showNotSecuredDialog) {
                NotSecureAlertDialog(finish = {
                    showNotSecuredDialog = false
                    if (it) {
                        onPersonaCreateClick()
                    }
                })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CreatePersonaContentList(
    onPersonaNameChange: (String) -> Unit,
    personaName: String,
    currentFields: ImmutableList<PersonaFieldKindWrapper>,
    onValueChanged: (Network.Persona.Field.Kind, String) -> Unit,
    onDeleteField: (Network.Persona.Field.Kind) -> Unit,
    addButtonEnabled: Boolean,
    modifier: Modifier = Modifier,
    onAddFieldClick: () -> Unit,
    onEditAvatar: () -> Unit
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
                text = stringResource(id = com.babylon.wallet.android.R.string.create_a_persona),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(dimensions.paddingSmall))
            InfoLink(
                stringResource(com.babylon.wallet.android.R.string.learn_about_personas),
                modifier = Modifier
                    .padding(horizontal = dimensions.paddingDefault)
            )
            Spacer(modifier = Modifier.height(30.dp))
            AsyncImage(
                model = "",
                placeholder = rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb())),
                fallback = rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb())),
                error = rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb())),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RadixTheme.shapes.circle)
            )
            UnderlineTextButton(text = stringResource(com.babylon.wallet.android.R.string.edit_avatar), onClick = onEditAvatar)
            Spacer(modifier = Modifier.height(dimensions.paddingLarge))
            RadixTextField(
                modifier = Modifier.fillMaxWidth(),
                onValueChanged = onPersonaNameChange,
                value = personaName,
                leftLabel = stringResource(id = com.babylon.wallet.android.R.string.persona_label),
                hint = stringResource(id = com.babylon.wallet.android.R.string.hint_persona_name)
            )
            Spacer(modifier = Modifier.height(dimensions.paddingMedium))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = com.babylon.wallet.android.R.string.this_will_be_shared_with_dapps),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.height(dimensions.paddingDefault))
        }
        items(currentFields, key = { it.kind }) { field ->
            PersonaPropertyInput(
                modifier = Modifier.fillMaxWidth().animateItemPlacement(),
                label = stringResource(id = field.kind.toDisplayResource()),
                value = field.value,
                onValueChanged = {
                    onValueChanged(field.kind, it)
                },
                onDeleteField = {
                    onDeleteField(field.kind)
                },
                required = !field.required
            )
            Spacer(modifier = Modifier.height(dimensions.paddingLarge))
        }
        item {
            Divider(color = RadixTheme.colors.gray5)
            Spacer(modifier = Modifier.height(dimensions.paddingDefault))
            Text(
                text = stringResource(id = com.babylon.wallet.android.R.string.some_dapps_may_request),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
            Spacer(Modifier.height(30.dp))
            RadixSecondaryButton(
                text = stringResource(id = com.babylon.wallet.android.R.string.add_a_field),
                onClick = onAddFieldClick,
                enabled = addButtonEnabled
            )
        }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun CreateAccountContentPreview() {
    RadixWalletTheme {
        CreatePersonaContent(
            onPersonaNameChange = {},
            onPersonaCreateClick = {},
            personaName = "Name",
            continueButtonEnabled = false,
            onBackClick = {},
            isDeviceSecure = true,
            modifier = Modifier,
            fieldsToAdd = persistentListOf(),
            currentFields = persistentListOf(),
            anyFieldSelected = false,
            onSelectionChanged = { _, _ -> },
            onAddFields = {},
            onDeleteField = {}
        ) { _, _ -> }
    }
}
