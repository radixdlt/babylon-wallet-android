package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.model.toDisplayResource
import kotlinx.collections.immutable.ImmutableList
import rdx.works.profile.data.model.pernetwork.PersonaData

@Composable
fun RequiredPersonaInformationInfo(
    requiredFields: ImmutableList<PersonaData.PersonaDataField.Kind>,
    modifier: Modifier = Modifier
) {
    val text = stringResource(id = R.string.dAppRequest_personalDataBox_requiredInformation)
    val finalText = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append("$text ")
        }
        append(requiredFields.map { stringResource(id = it.toDisplayResource()) }.joinToString())
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Icon(
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
            ),
            contentDescription = null,
            tint = RadixTheme.colors.red1
        )
        androidx.compose.material3.Text(
            text = finalText,
            style = RadixTheme.typography.body1StandaloneLink,
            color = RadixTheme.colors.red1
        )
    }
}
