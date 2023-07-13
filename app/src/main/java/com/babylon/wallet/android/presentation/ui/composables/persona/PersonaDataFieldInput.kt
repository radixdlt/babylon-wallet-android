package com.babylon.wallet.android.presentation.ui.composables.persona

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.IconButton
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
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
                modifier = modifier,
                label = label,
                value = field.value,
                onValueChanged = {
                    onValueChanged(PersonaData.PersonaDataField.Email(it))
                },
                onDeleteField = onDeleteField,
                required = required,
                phoneInput = phoneInput,
                error = error,
                onFocusChanged = onFocusChanged
            )
        }

        is PersonaData.PersonaDataField.Name -> {
            PersonaNameInput(
                modifier = modifier,
                onDeleteField = onDeleteField,
                required = required,
                error = error,
                onFocusChanged = onFocusChanged,
                state = rememberPersonaNameInputState(field.family, field.given, field.middle.orEmpty(), field.variant),
                onPersonaNameFieldChanged = onValueChanged,
                label = label
            )
        }

        is PersonaData.PersonaDataField.PhoneNumber -> {
            PersonaDataStringInput(
                modifier = modifier,
                label = label,
                value = field.value,
                onValueChanged = {
                    onValueChanged(field)
                },
                onDeleteField = onDeleteField,
                required = required,
                phoneInput = phoneInput,
                error = error,
                onFocusChanged = onFocusChanged
            )
        }

        else -> {}
    }
}

@Composable
fun PersonaDataStringInput(
    modifier: Modifier,
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    onDeleteField: (() -> Unit)? = null,
    onFocusChanged: ((FocusState) -> Unit)? = null,
    required: Boolean,
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
    modifier: Modifier,
    onPersonaNameFieldChanged: (PersonaData.PersonaDataField.Name) -> Unit,
    onDeleteField: () -> Unit,
    required: Boolean,
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
                state.middle
            )
        )
    }
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Text(
                text = label,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
            Divider(modifier = Modifier.weight(1f), color = RadixTheme.colors.gray4)
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                when (state.variant) {
                    PersonaData.PersonaDataField.Name.Variant.Western -> {
                        PersonaDataStringInput(
                            modifier = Modifier.fillMaxWidth(),
                            label = stringResource(id = R.string.authorizedDapps_personaDetails_nameGiven),
                            value = state.given,
                            onValueChanged = {
                                state.given = it
                                nameChangedCallback()
                            },
                            required = required,
                            error = error,
                            onFocusChanged = onFocusChanged
                        )
                        PersonaDataStringInput(
                            modifier = Modifier.fillMaxWidth(),
                            label = stringResource(id = R.string.authorizedDapps_personaDetails_nameMiddle),
                            value = state.middle,
                            onValueChanged = {
                                state.middle = it
                                nameChangedCallback()
                            },
                            required = required,
                            error = error,
                            onFocusChanged = onFocusChanged
                        )
                        PersonaDataStringInput(
                            modifier = Modifier.fillMaxWidth(),
                            label = stringResource(id = R.string.authorizedDapps_personaDetails_nameFamily),
                            value = state.family,
                            onValueChanged = {
                                state.family = it
                                nameChangedCallback()
                            },
                            required = required,
                            error = error,
                            onFocusChanged = onFocusChanged
                        )
                    }

                    PersonaData.PersonaDataField.Name.Variant.Eastern -> {
                        PersonaDataStringInput(
                            modifier = Modifier.fillMaxWidth(),
                            label = stringResource(id = R.string.authorizedDapps_personaDetails_nameFamily),
                            value = state.family,
                            onValueChanged = {
                                state.family = it
                                nameChangedCallback()
                            },
                            required = required,
                            error = error,
                            onFocusChanged = onFocusChanged
                        )
                        PersonaDataStringInput(
                            modifier = Modifier.fillMaxWidth(),
                            label = stringResource(id = R.string.authorizedDapps_personaDetails_nameGiven),
                            value = state.given,
                            onValueChanged = {
                                state.given = it
                                nameChangedCallback()
                            },
                            required = required,
                            error = error,
                            onFocusChanged = onFocusChanged
                        )
                        PersonaDataStringInput(
                            modifier = Modifier.fillMaxWidth(),
                            label = stringResource(id = R.string.authorizedDapps_personaDetails_nameMiddle),
                            value = state.middle,
                            onValueChanged = {
                                state.middle = it
                                nameChangedCallback()
                            },
                            required = required,
                            error = error,
                            onFocusChanged = onFocusChanged
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    LabelRadioButtonHorizontal(
                        modifier = Modifier.weight(1f),
                        selected = state.variant == PersonaData.PersonaDataField.Name.Variant.Western,
                        label = stringResource(id = R.string.authorizedDapps_personaDetails_nameVariantWestern),
                        onClick = {
                            state.variant = PersonaData.PersonaDataField.Name.Variant.Western
                        }
                    )
                    LabelRadioButtonHorizontal(
                        modifier = Modifier.weight(1f),
                        selected = state.variant == PersonaData.PersonaDataField.Name.Variant.Eastern,
                        label = stringResource(id = R.string.authorizedDapps_personaDetails_nameVariantEastern),
                        onClick = {
                            state.variant = PersonaData.PersonaDataField.Name.Variant.Eastern
                        }
                    )
                }
            }
            IconButton(onClick = onDeleteField) {
                Icon(
                    tint = RadixTheme.colors.gray1,
                    contentDescription = null,
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_delete_outline)
                )
            }
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        Divider(modifier = Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
    }
}

@Composable
private fun LabelRadioButtonHorizontal(modifier: Modifier, selected: Boolean, label: String, onClick: () -> Unit) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected,
            colors = RadioButtonDefaults.colors(
                selectedColor = RadixTheme.colors.gray1,
                unselectedColor = RadixTheme.colors.gray3,
                disabledSelectedColor = Color.White
            ),
            onClick = onClick
        )
        Text(
            text = label,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1
        )
    }
}

class PersonaNameInputState(
    familyName: String,
    givenName: String,
    middleName: String,
    variant: PersonaData.PersonaDataField.Name.Variant
) {
    var family by mutableStateOf(familyName)
    var given by mutableStateOf(givenName)
    var middle by mutableStateOf(middleName)
    var variant by mutableStateOf(variant)

    companion object {
        val Saver: Saver<PersonaNameInputState, *> = listSaver(
            save = {
                listOf(it.family, it.given, it.middle, it.variant.name)
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
    middleName: String = "",
    variant: PersonaData.PersonaDataField.Name.Variant = PersonaData.PersonaDataField.Name.Variant.Western
): PersonaNameInputState {
    return rememberSaveable(saver = PersonaNameInputState.Saver) {
        PersonaNameInputState(familyName, givenName, middleName, variant)
    }
}
