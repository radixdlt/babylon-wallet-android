package com.babylon.wallet.android.presentation.ui.composables.persona

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import rdx.works.profile.data.model.pernetwork.PersonaData

@Composable
fun PersonaDataFieldInput(
    modifier: Modifier,
    label: String,
    field: PersonaData.PersonaDataField?,
    onValueChanged: (PersonaData.PersonaDataField) -> Unit,
    onDeleteField: () -> Unit,
    onFocusChanged: ((FocusState) -> Unit)? = null,
    required: Boolean,
    phoneInput: Boolean = false,
    error: String? = null
) {
    when (field) {
        is PersonaData.PersonaDataField.Email -> {
            PersonaDataStringInput(
                label = label,
                value = field.value,
                onValueChanged = {
                    onValueChanged(PersonaData.PersonaDataField.Email(it))
                },
                required = required,
                modifier = modifier,
                onDeleteField = onDeleteField,
                onFocusChanged = onFocusChanged,
                phoneInput = phoneInput,
                error = error
            )
        }

        is PersonaData.PersonaDataField.Name -> {
            PersonaNameInput(
                onPersonaNameFieldChanged = onValueChanged,
                onDeleteField = onDeleteField,
                required = required,
                modifier = modifier,
                state = rememberPersonaNameInputState(field.family, field.given, field.nickname.orEmpty(), field.variant),
                onFocusChanged = onFocusChanged,
                error = error,
                label = label
            )
        }

        is PersonaData.PersonaDataField.PhoneNumber -> {
            PersonaDataStringInput(
                label = label,
                value = field.value,
                onValueChanged = {
                    onValueChanged(PersonaData.PersonaDataField.PhoneNumber(it))
                },
                required = required,
                modifier = modifier,
                onDeleteField = onDeleteField,
                onFocusChanged = onFocusChanged,
                phoneInput = phoneInput,
                error = error
            )
        }

        else -> {}
    }
}

@Composable
fun PersonaDataStringInput(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    required: Boolean,
    modifier: Modifier = Modifier,
    onDeleteField: (() -> Unit)? = null,
    onFocusChanged: ((FocusState) -> Unit)? = null,
    phoneInput: Boolean = false,
    error: String? = null
) {
    val focusManager = LocalFocusManager.current
    RadixTextField(
        modifier = modifier,
        onValueChanged = onValueChanged,
        value = value,
        leftLabel = label,
        iconToTheRight = if (!required && onDeleteField != null) {
            {
                IconButton(onClick = onDeleteField) {
                    Icon(
                        tint = RadixTheme.colors.gray1,
                        contentDescription = null,
                        painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_delete_outline)
                    )
                }
            }
        } else {
            null
        },
        onFocusChanged = onFocusChanged,
        keyboardActions = KeyboardActions(onNext = {
            focusManager.moveFocus(FocusDirection.Next)
        }),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
            keyboardType = if (phoneInput) {
                KeyboardType.Phone
            } else {
                KeyboardOptions.Default.keyboardType
            }
        ),
        rightLabel = if (required) stringResource(id = R.string.editPersona_requiredByDapp) else null,
        error = error
    )
}

@Composable
fun PersonaNameInput(
    onPersonaNameFieldChanged: (PersonaData.PersonaDataField.Name) -> Unit,
    onDeleteField: () -> Unit,
    required: Boolean,
    modifier: Modifier = Modifier,
    state: PersonaNameInputState = rememberPersonaNameInputState(),
    onFocusChanged: ((FocusState) -> Unit)? = null,
    error: String? = null,
    label: String
) {
    val nameChangedCallback = {
        onPersonaNameFieldChanged(
            PersonaData.PersonaDataField.Name(
                state.variant,
                state.given,
                state.family,
                state.nickname
            )
        )
    }
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
            IconButton(onClick = onDeleteField) {
                Icon(
                    tint = RadixTheme.colors.gray1,
                    contentDescription = null,
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_delete_outline)
                )
            }
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.authorizedDapps_personaDetails_nameVariant),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))
            NameOrderSelector(modifier = Modifier.fillMaxWidth(), selectedVariant = state.variant, onVariantChanged = { newVariant ->
                state.variant = newVariant
            })
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            when (state.variant) {
                PersonaData.PersonaDataField.Name.Variant.Western -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                    ) {
                        PersonaDataStringInput(
                            label = stringResource(id = R.string.authorizedDapps_personaDetails_givenName),
                            value = state.given,
                            onValueChanged = {
                                state.given = it
                                nameChangedCallback()
                            },
                            required = required,
                            modifier = Modifier.weight(1f),
                            onFocusChanged = onFocusChanged,
                            error = error
                        )
                        PersonaDataStringInput(
                            label = stringResource(id = R.string.authorizedDapps_personaDetails_nickname),
                            value = state.nickname,
                            onValueChanged = {
                                state.nickname = it
                                nameChangedCallback()
                            },
                            required = required,
                            modifier = Modifier.weight(1f),
                            onFocusChanged = onFocusChanged,
                            error = error
                        )
                    }
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    PersonaDataStringInput(
                        label = stringResource(id = R.string.authorizedDapps_personaDetails_nameFamily),
                        value = state.family,
                        onValueChanged = {
                            state.family = it
                            nameChangedCallback()
                        },
                        required = required,
                        modifier = Modifier.fillMaxWidth(),
                        onFocusChanged = onFocusChanged,
                        error = error
                    )
                }

                PersonaData.PersonaDataField.Name.Variant.Eastern -> {
                    PersonaDataStringInput(
                        label = stringResource(id = R.string.authorizedDapps_personaDetails_nameFamily),
                        value = state.family,
                        onValueChanged = {
                            state.family = it
                            nameChangedCallback()
                        },
                        required = required,
                        modifier = Modifier.fillMaxWidth(),
                        onFocusChanged = onFocusChanged,
                        error = error
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                    ) {
                        PersonaDataStringInput(
                            label = stringResource(id = R.string.authorizedDapps_personaDetails_givenName),
                            value = state.given,
                            onValueChanged = {
                                state.given = it
                                nameChangedCallback()
                            },
                            required = required,
                            modifier = Modifier.weight(1f),
                            onFocusChanged = onFocusChanged,
                            error = error
                        )
                        PersonaDataStringInput(
                            label = stringResource(id = R.string.authorizedDapps_personaDetails_nickname),
                            value = state.nickname,
                            onValueChanged = {
                                state.nickname = it
                                nameChangedCallback()
                            },
                            required = required,
                            modifier = Modifier.weight(1f),
                            onFocusChanged = onFocusChanged,
                            error = error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NameOrderSelector(
    modifier: Modifier,
    selectedVariant: PersonaData.PersonaDataField.Name.Variant,
    onVariantChanged: (PersonaData.PersonaDataField.Name.Variant) -> Unit
) {
    var isMenuExpanded by remember {
        mutableStateOf(false)
    }
    Box {
        Row(
            modifier = modifier
                .throttleClickable {
                    isMenuExpanded = true
                }
                .background(RadixTheme.colors.gray5, shape = RadixTheme.shapes.roundedRectSmall)
                .border(1.dp, RadixTheme.colors.gray3, RadixTheme.shapes.roundedRectSmall)
                .padding(RadixTheme.dimensions.paddingDefault),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(
                    id = if (selectedVariant == PersonaData.PersonaDataField.Name.Variant.Eastern) {
                        R.string.authorizedDapps_personaDetails_nameVariantEastern
                    } else {
                        R.string.authorizedDapps_personaDetails_nameVariantWestern
                    }
                ),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_down),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )
        }

        DropdownMenu(
            modifier = Modifier.background(RadixTheme.colors.gray5),
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false }
        ) {
            PersonaData.PersonaDataField.Name.Variant.values().forEach { variant ->
                DropdownMenuItem(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(
                                id = when (variant) {
                                    PersonaData.PersonaDataField.Name.Variant.Western -> R.string.authorizedDapps_personaDetails_nameVariantWestern
                                    PersonaData.PersonaDataField.Name.Variant.Eastern -> R.string.authorizedDapps_personaDetails_nameVariantEastern
                                }
                            ),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.defaultText
                        )
                    },
                    onClick = {
                        isMenuExpanded = false
                        onVariantChanged(variant)
                    },
                    contentPadding = PaddingValues(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingXSmall
                    )
                )
            }
        }
    }
}

class PersonaNameInputState(
    familyName: String,
    givenName: String,
    nickname: String,
    variant: PersonaData.PersonaDataField.Name.Variant
) {
    var family by mutableStateOf(familyName)
    var given by mutableStateOf(givenName)
    var nickname by mutableStateOf(nickname)
    var variant by mutableStateOf(variant)

    companion object {
        val Saver: Saver<PersonaNameInputState, *> = listSaver(
            save = {
                listOf(it.family, it.given, it.nickname, it.variant.name)
            },
            restore = {
                PersonaNameInputState(it[0], it[1], it[2], PersonaData.PersonaDataField.Name.Variant.valueOf(it[3]))
            }
        )
    }
}

@Composable
fun rememberPersonaNameInputState(
    familyName: String = "",
    givenName: String = "",
    nickname: String = "",
    variant: PersonaData.PersonaDataField.Name.Variant = PersonaData.PersonaDataField.Name.Variant.Western
): PersonaNameInputState {
    return rememberSaveable(saver = PersonaNameInputState.Saver) {
        PersonaNameInputState(familyName, givenName, nickname, variant)
    }
}

@Composable
@Preview(showBackground = true)
fun PersonaDataStringInputPreview() {
    RadixWalletTheme {
        PersonaDataStringInput(
            value = "Field",
            onFocusChanged = {},
            label = "Label",
            required = false,
            onValueChanged = {}
        )
    }
}

@Composable
@Preview(showBackground = true)
fun PersonaNameInputPreview() {
    RadixWalletTheme {
        PersonaNameInput(
            label = "Name",
            onFocusChanged = {},
            required = false,
            onPersonaNameFieldChanged = {},
            onDeleteField = {}
        )
    }
}
