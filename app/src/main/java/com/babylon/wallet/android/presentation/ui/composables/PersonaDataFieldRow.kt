package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
            PersonaDataStringField(modifier = modifier, label = stringResource(id = field.kind.toDisplayResource()), value = field.value)
        }

        is PersonaData.PersonaDataField.Name -> {
            PersonaDataStringField(
                modifier = modifier,
                label = stringResource(id = field.kind.toDisplayResource()),
                value = field.fullName,
                singleLine = false
            )
        }

        is PersonaData.PersonaDataField.PhoneNumber -> {
            PersonaDataStringField(modifier = modifier, label = stringResource(id = field.kind.toDisplayResource()), value = field.value)
        }

        else -> {}
    }
}

@Composable
fun PersonaDataStringField(modifier: Modifier, label: String, value: String, singleLine: Boolean = true) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray2
        )
        Spacer(modifier = Modifier.height(dimensions.paddingSmall))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = value,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray1,
            overflow = TextOverflow.Ellipsis,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE
        )
    }
}
