package com.babylon.wallet.android.presentation.ui.composables.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.ui.composables.SecurityPromptLabel
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun PersonaCard(
    modifier: Modifier = Modifier,
    persona: Persona,
    showChevron: Boolean = true,
    elevation: Dp = 8.dp,
    onNavigateToSecurityCenter: (() -> Unit)? = null,
    securityPrompts: ImmutableList<SecurityPromptType>? = null
) {
    Column(
        modifier = modifier
            .shadow(elevation = elevation, shape = RadixTheme.shapes.roundedRectMedium)
            .clip(RadixTheme.shapes.roundedRectMedium)
            .fillMaxWidth()
            .background(RadixTheme.colors.white, shape = RadixTheme.shapes.roundedRectMedium)
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingDefault
            ),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Thumbnail.Persona(
                modifier = Modifier.size(44.dp),
                persona = persona
            )
            Text(
                modifier = Modifier.weight(1f),
                text = persona.displayName.value,
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (showChevron) {
                Icon(
                    painter = painterResource(
                        id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right
                    ),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray1
                )
            }
        }
        securityPrompts?.forEach {
            SecurityPromptLabel(
                modifier = Modifier
                    .fillMaxWidth()
                    .throttleClickable(
                        enabled = onNavigateToSecurityCenter != null
                    ) { onNavigateToSecurityCenter?.invoke() },
                text = it.toText()
            )
        }
    }
}

@Composable
private fun SecurityPromptType.toText() = when (this) {
    SecurityPromptType.WRITE_DOWN_SEED_PHRASE -> stringResource(id = R.string.securityProblems_no3_personas)
    SecurityPromptType.RECOVERY_REQUIRED -> stringResource(id = R.string.securityProblems_no9_walletSettingsPersonas)
    SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM -> stringResource(id = R.string.securityProblems_no5_personas)
    SecurityPromptType.WALLET_NOT_RECOVERABLE -> stringResource(id = R.string.securityProblems_no6_personas)
    SecurityPromptType.CONFIGURATION_BACKUP_NOT_UPDATED -> stringResource(id = R.string.securityProblems_no7_personas)
}

@Composable
fun PersonaSelectableCard(modifier: Modifier, persona: PersonaUiModel, onSelectPersona: (Persona) -> Unit) {
    val paddingDefault = RadixTheme.dimensions.paddingDefault
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(paddingDefault),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Thumbnail.Persona(
                modifier = Modifier.size(44.dp),
                persona = persona.persona
            )
            Text(
                modifier = Modifier.weight(1f),
                text = persona.persona.displayName.value,
                textAlign = TextAlign.Start,
                maxLines = 2,
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1
            )
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
        persona.lastUsedOn?.let {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = RadixTheme.colors.gray4)
                Spacer(modifier = Modifier.height(paddingDefault))
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.dAppRequest_login_lastLoginWasOn, it),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(paddingDefault))
            }
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun PersonaCardPreview() {
    RadixWalletTheme {
        PersonaCard(
            persona = Persona.sampleMainnet(),
            securityPrompts = listOf(
                SecurityPromptType.RECOVERY_REQUIRED,
                SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM
            ).toPersistentList()
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun PersonaSelectableCardPreview() {
    RadixWalletTheme {
        PersonaSelectableCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            persona = PersonaUiModel(Persona.sampleMainnet.other())
        ) {}
    }
}
