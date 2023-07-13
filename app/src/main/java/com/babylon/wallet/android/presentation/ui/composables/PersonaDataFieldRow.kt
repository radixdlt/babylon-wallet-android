package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.presentation.model.toDisplayResource
import rdx.works.profile.data.model.pernetwork.PersonaData

@Composable
fun PersonaDataFieldRow(modifier: Modifier, field: PersonaData.PersonaDataField) {
    when (field) {
        is PersonaData.PersonaDataField.Email -> {
            PersonaDataFieldString(modifier = modifier, label = stringResource(id = field.kind.toDisplayResource()), value = field.value)
        }

        is PersonaData.PersonaDataField.Name -> {
            PersonaDataFieldName(modifier = modifier, label = stringResource(id = field.kind.toDisplayResource()), field = field)
        }

        is PersonaData.PersonaDataField.PhoneNumber -> {
            PersonaDataFieldString(modifier = modifier, label = stringResource(id = field.kind.toDisplayResource()), value = field.value)
        }

        else -> {}
    }
}

@Composable
fun PersonaDataFieldString(modifier: Modifier, label: String, value: String) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray2
        )
        Spacer(modifier = Modifier.height(dimensions.paddingSmall))
        Text(
            text = value,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PersonaDataFieldName(
    modifier: Modifier,
    field: PersonaData.PersonaDataField.Name,
    label: String
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.paddingSmall)
        ) {
            Text(
                text = label,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
            Divider(modifier = Modifier.weight(1f), color = RadixTheme.colors.gray4)
        }
        Spacer(modifier = Modifier.height(dimensions.paddingSmall))
        Column(modifier = modifier) {
            when (field.variant) {
                PersonaData.PersonaDataField.Name.Variant.Western -> {
                    PersonaDataFieldString(
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(id = R.string.authorizedDapps_personaDetails_nameGiven),
                        value = field.given
                    )
                    field.middle?.let {
                        PersonaDataFieldString(
                            modifier = Modifier.fillMaxWidth(),
                            label = stringResource(id = R.string.authorizedDapps_personaDetails_nameMiddle),
                            value = it
                        )
                    }
                    PersonaDataFieldString(
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(id = R.string.authorizedDapps_personaDetails_nameFamily),
                        value = field.family
                    )
                }

                PersonaData.PersonaDataField.Name.Variant.Eastern -> {
                    PersonaDataFieldString(
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(id = R.string.authorizedDapps_personaDetails_nameFamily),
                        value = field.family
                    )
                    PersonaDataFieldString(
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(id = R.string.authorizedDapps_personaDetails_nameGiven),
                        value = field.given
                    )
                    field.middle?.let {
                        PersonaDataFieldString(
                            modifier = Modifier.fillMaxWidth(),
                            label = stringResource(id = R.string.authorizedDapps_personaDetails_nameMiddle),
                            value = it
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(dimensions.paddingSmall))
        Divider(modifier = Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
    }
}
