package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.presentation.model.fullName
import com.babylon.wallet.android.presentation.model.toDisplayResource
import rdx.works.core.sargon.PersonaDataField

@Composable
fun PersonaDataFieldRow(
    modifier: Modifier,
    field: PersonaDataField,
    labelStyle: TextStyle = RadixTheme.typography.body1Regular
) {
    when (field) {
        is PersonaDataField.Email -> {
            PersonaDataStringField(
                modifier = modifier,
                label = stringResource(id = field.kind.toDisplayResource()),
                value = field.value,
                labelStyle = labelStyle
            )
        }

        is PersonaDataField.Name -> {
            PersonaDataStringField(
                modifier = modifier,
                label = stringResource(id = field.kind.toDisplayResource()),
                value = field.fullName,
                singleLine = false,
                labelStyle = labelStyle
            )
        }

        is PersonaDataField.PhoneNumber -> {
            PersonaDataStringField(
                modifier = modifier,
                label = stringResource(id = field.kind.toDisplayResource()),
                value = field.value,
                labelStyle = labelStyle
            )
        }

        else -> {}
    }
}

@Composable
fun PersonaDataStringField(
    modifier: Modifier,
    label: String,
    value: String,
    labelStyle: TextStyle = RadixTheme.typography.body1Regular,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        PersonaDataSectionHeader(
            text = label,
            textStyle = labelStyle
        )
        Spacer(modifier = Modifier.height(dimensions.paddingSmall))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = value,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.text,
            overflow = TextOverflow.Ellipsis,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE
        )
    }
}

@Composable
fun PersonaDataSectionHeader(
    modifier: Modifier = Modifier,
    text: String,
    textStyle: TextStyle = RadixTheme.typography.body1Regular
) {
    Text(
        modifier = modifier,
        text = text,
        style = textStyle,
        color = RadixTheme.colors.textSecondary
    )
}
