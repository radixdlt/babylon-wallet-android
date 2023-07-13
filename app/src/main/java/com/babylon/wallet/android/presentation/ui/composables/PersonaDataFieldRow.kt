package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.presentation.model.fullName
import com.babylon.wallet.android.presentation.model.toDisplayResource
import rdx.works.profile.data.model.pernetwork.PersonaData

@Composable
fun PersonaDataFieldRow(modifier: Modifier, field: PersonaData.PersonaDataField) {
    when (field) {
        is PersonaData.PersonaDataField.Email -> {
            PersonaDataFieldString(modifier = modifier, label = stringResource(id = field.kind.toDisplayResource()), value = field.value)
        }

        is PersonaData.PersonaDataField.Name -> {
            PersonaDataFieldString(modifier = modifier, label = stringResource(id = field.kind.toDisplayResource()), value = field.fullName)
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

