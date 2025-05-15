package com.babylon.wallet.android.presentation.ui.composables.persona

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.ui.composables.RadixRadioButton
import com.babylon.wallet.android.presentation.ui.composables.RadixRadioButtonDefaults
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.radixdlt.sargon.Persona
import kotlinx.collections.immutable.ImmutableList
import rdx.works.core.sargon.PersonaDataField

@Composable
fun PersonaDetailCard(
    persona: PersonaUiModel,
    missingFields: ImmutableList<PersonaDataField.Kind>,
    onEditClick: (Persona) -> Unit,
    modifier: Modifier = Modifier,
    onSelectPersona: ((Persona) -> Unit)? = null
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingLarge
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Thumbnail.Persona(
                modifier = Modifier
                    .padding(vertical = RadixTheme.dimensions.paddingDefault)
                    .size(44.dp),
                persona = persona.persona
            )
            Text(
                text = persona.persona.displayName.value,
                style = RadixTheme.typography.header,
                color = RadixTheme.colors.text
            )
            if (onSelectPersona != null) {
                Spacer(modifier = Modifier.weight(1f))
                RadixRadioButton(
                    selected = persona.selected,
                    onClick = {
                        onSelectPersona(persona.persona)
                    },
                    colors = RadixRadioButtonDefaults.darkColors()
                )
            }
        }
        HorizontalDivider(color = RadixTheme.colors.divider)
        val personalInfo = persona.personalInfoFormatted()
        if (personalInfo.isNotEmpty()) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                text = personalInfo,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary
            )
        }
        if (missingFields.isNotEmpty()) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            RequiredPersonaInformationInfo(
                requiredFields = missingFields,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            )
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        if (missingFields.isNotEmpty()) {
            RadixPrimaryButton(
                text = stringResource(id = R.string.dAppRequest_personalDataBox_edit),
                onClick = {
                    onEditClick(persona.persona)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault)
            )
        } else {
            RadixSecondaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RadixTheme.dimensions.paddingDefault),
                onClick = {
                    onEditClick(persona.persona)
                },
                text = stringResource(id = R.string.dAppRequest_personalDataBox_edit)
            )
        }
    }
}
