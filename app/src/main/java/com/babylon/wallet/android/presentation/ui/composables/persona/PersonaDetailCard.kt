package com.babylon.wallet.android.presentation.ui.composables.persona

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.model.toDisplayResource
import com.babylon.wallet.android.presentation.ui.composables.PersonaRoundedAvatar
import com.babylon.wallet.android.utils.setSpanForPlaceholder
import kotlinx.collections.immutable.ImmutableList
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun PersonaDetailCard(
    persona: PersonaUiModel,
    missingFields: ImmutableList<Network.Persona.Field.ID>,
    onEditClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSelectPersona: ((Network.Persona) -> Unit)? = null
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            PersonaRoundedAvatar(
                url = "",
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingDefault)
                    .size(44.dp)
            )
            Text(
                text = persona.persona.displayName,
                style = RadixTheme.typography.header,
                color = RadixTheme.colors.gray1
            )
            if (onSelectPersona != null) {
                Spacer(modifier = Modifier.weight(1f))
                RadioButton(
                    selected = persona.selected,
                    onClick = {
                        onSelectPersona(persona.persona)
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = RadixTheme.colors.gray1,
                        unselectedColor = RadixTheme.colors.gray4
                    ),
                )
            }
        }
        Divider(color = RadixTheme.colors.gray4)
        val personalInfo = persona.personalInfoFormatted()
        if (personalInfo.isNotEmpty()) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                text = personalInfo,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        }
        if (missingFields.isNotEmpty()) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            RequiredInformationInfo(
                requiredFields = missingFields,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            )
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        if (missingFields.isNotEmpty()) {
            RadixPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                onClick = {
                    onEditClick(persona.persona.address)
                },
                text = stringResource(id = R.string.dAppRequest_personalDataBox_edit)
            )
        } else {
            RadixSecondaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                onClick = {
                    onEditClick(persona.persona.address)
                },
                text = stringResource(id = R.string.dAppRequest_personalDataBox_edit)
            )
        }
    }
}

@Composable
private fun RequiredInformationInfo(
    requiredFields: ImmutableList<Network.Persona.Field.ID>,
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
