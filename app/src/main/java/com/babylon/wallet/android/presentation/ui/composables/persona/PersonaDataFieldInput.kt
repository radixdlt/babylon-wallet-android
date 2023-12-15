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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.LabelType
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.DefaultSelector
import com.babylon.wallet.android.presentation.ui.composables.SelectorItem
import kotlinx.collections.immutable.toPersistentList
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.PersonaData.PersonaDataField.Name

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
                modifier = modifier,
                required = required,
                onDeleteField = onDeleteField,
                onFocusChanged = onFocusChanged,
                phoneInput = phoneInput,
                error = error
            )
        }

        is Name -> {
            PersonaNameInput(
                onPersonaNameFieldChanged = onValueChanged,
                onDeleteField = onDeleteField,
                required = required,
                modifier = modifier,
                state = rememberPersonaNameInputState(field.family, field.given, field.nickname, field.variant),
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
                modifier = modifier,
                required = required,
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
    modifier: Modifier = Modifier,
    required: Boolean = false,
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
        leftLabel = LabelType.Default(label),
        iconToTheRight = if (onDeleteField != null) {
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
        rightLabel = if (required) LabelType.Default(stringResource(id = R.string.editPersona_requiredByDapp)) else null,
        error = error,
        singleLine = true
    )
}

@Composable
fun PersonaNameInput(
    onPersonaNameFieldChanged: (Name) -> Unit,
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
            Name(
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
            if (required) {
                Text(
                    text = stringResource(id = R.string.editPersona_requiredByDapp),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray2
                )
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
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.authorizedDapps_personaDetails_nameVariant),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))
            DefaultSelector(
                modifier = Modifier.fillMaxWidth(),
                items = Name.Variant.values().map { SelectorItem(it, it.description()) }.toPersistentList(),
                selectedItem = SelectorItem(state.variant, state.variant.description()),
                onItemSelected = { item ->
                    state.variant = item.item
                    nameChangedCallback()
                }
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            when (state.variant) {
                Name.Variant.Western -> {
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
                        modifier = Modifier.fillMaxWidth(),
                        onFocusChanged = onFocusChanged,
                        error = error
                    )
                }

                Name.Variant.Eastern -> {
                    PersonaDataStringInput(
                        label = stringResource(id = R.string.authorizedDapps_personaDetails_nameFamily),
                        value = state.family,
                        onValueChanged = {
                            state.family = it
                            nameChangedCallback()
                        },
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
private fun Name.Variant.description(): String {
    return when (this) {
        Name.Variant.Western -> stringResource(id = R.string.authorizedDapps_personaDetails_nameVariantWestern)
        Name.Variant.Eastern -> stringResource(id = R.string.authorizedDapps_personaDetails_nameVariantEastern)
    }
}

class PersonaNameInputState(
    familyName: String,
    givenName: String,
    nickname: String,
    variant: Name.Variant
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
                PersonaNameInputState(it[0], it[1], it[2], Name.Variant.valueOf(it[3]))
            }
        )
    }
}

@Composable
fun rememberPersonaNameInputState(
    familyName: String = "",
    givenName: String = "",
    nickname: String = "",
    variant: Name.Variant = Name.Variant.Western
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
            label = "Label",
            value = "Field",
            onValueChanged = {},
            required = false,
            onFocusChanged = {}
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
