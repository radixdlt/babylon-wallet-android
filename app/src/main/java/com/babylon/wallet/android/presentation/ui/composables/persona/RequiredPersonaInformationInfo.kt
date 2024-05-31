package com.babylon.wallet.android.presentation.ui.composables.persona

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.model.toDisplayResource
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import kotlinx.collections.immutable.ImmutableList
import rdx.works.core.sargon.PersonaDataField

@Composable
fun RequiredPersonaInformationInfo(
    requiredFields: ImmutableList<PersonaDataField.Kind>,
    modifier: Modifier = Modifier
) {
    val text = stringResource(id = R.string.dAppRequest_personalDataBox_requiredInformation)
    val finalText = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append("$text ")
        }
        append(requiredFields.map { stringResource(id = it.toDisplayResource()) }.joinToString())
    }
    WarningText(
        modifier = modifier,
        text = finalText
    )
}
