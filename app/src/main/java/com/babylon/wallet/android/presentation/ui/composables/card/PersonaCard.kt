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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixCheckboxDefaults
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.usecases.securityproblems.SecurityPromptType
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.ui.composables.PromptLabel
import com.babylon.wallet.android.presentation.ui.composables.RadixRadioButton
import com.babylon.wallet.android.presentation.ui.composables.RadixRadioButtonDefaults
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

// TODO Theme
@Composable
fun SimplePersonaCard(
    modifier: Modifier = Modifier,
    persona: Persona,
) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Thumbnail.Persona(
                modifier = Modifier.size(54.dp),
                persona = persona
            )
            Text(
                modifier = Modifier.weight(1f),
                text = persona.displayName.value,
                textAlign = TextAlign.Start,
                maxLines = 2,
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.text
            )
        }
    }
}

@Composable
fun SimplePersonaCardWithShadow(
    modifier: Modifier = Modifier,
    persona: Persona,
) {
    Column(modifier) {
        Row(
            Modifier
                .defaultCardShadow(elevation = 6.dp)
                .background(
                    brush = SolidColor(RadixTheme.colors.backgroundSecondary),
                    shape = RadixTheme.shapes.roundedRectMedium
                )
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                .clip(RadixTheme.shapes.roundedRectMedium)
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Thumbnail.Persona(
                modifier = Modifier.size(54.dp),
                persona = persona
            )
            Text(
                modifier = Modifier.weight(1f),
                text = persona.displayName.value,
                textAlign = TextAlign.Start,
                maxLines = 2,
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.text
            )
        }
    }
}

@Composable
fun PersonaCard(
    modifier: Modifier = Modifier,
    persona: Persona,
    showChevron: Boolean = true,
    onNavigateToSecurityCenter: (() -> Unit)? = null,
    securityPrompts: ImmutableList<SecurityPromptType>? = null
) {
    Column(
        modifier = modifier
            .defaultCardShadow()
            .fillMaxWidth()
            .background(RadixTheme.colors.background, shape = RadixTheme.shapes.roundedRectMedium)
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
                color = RadixTheme.colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (showChevron) {
                Icon(
                    painter = painterResource(
                        id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right
                    ),
                    contentDescription = null,
                    tint = RadixTheme.colors.text
                )
            }
        }
        securityPrompts?.forEach {
            PromptLabel(
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
    SecurityPromptType.RECOVERY_REQUIRED -> stringResource(id = R.string.securityProblems_no9_personas)
    SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM -> stringResource(id = R.string.securityProblems_no5_personas)
    SecurityPromptType.WALLET_NOT_RECOVERABLE -> stringResource(id = R.string.securityProblems_no6_personas)
    SecurityPromptType.CONFIGURATION_BACKUP_NOT_UPDATED -> stringResource(id = R.string.securityProblems_no7_personas)
}

@Composable
fun PersonaSelectableCard(modifier: Modifier, persona: PersonaUiModel, onSelectPersona: (Persona) -> Unit) {
    val paddingDefault = RadixTheme.dimensions.paddingDefault
    Column(modifier) {
        SimplePersonaSelectionCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingDefault),
            persona = persona.persona,
            checked = persona.selected,
            isSingleChoice = true,
            onSelectPersona = onSelectPersona
        )
        persona.lastUsedOn?.let {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = RadixTheme.colors.divider)
                Spacer(modifier = Modifier.height(paddingDefault))
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.dAppRequest_login_lastLoginWasOn, it),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(paddingDefault))
            }
        }
    }
}

@Composable
fun SimplePersonaSelectionCard(
    modifier: Modifier,
    persona: Persona,
    checked: Boolean,
    isSingleChoice: Boolean,
    onSelectPersona: (Persona) -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumbnail.Persona(
            modifier = Modifier.size(44.dp),
            persona = persona
        )
        Text(
            modifier = Modifier.weight(1f),
            text = persona.displayName.value,
            textAlign = TextAlign.Start,
            maxLines = 2,
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.text
        )
        if (isSingleChoice) {
            RadixRadioButton(
                selected = checked,
                onClick = { onSelectPersona(persona) },
                colors = RadixRadioButtonDefaults.darkColors(),
            )
        } else {
            Checkbox(
                checked = checked,
                colors = RadixCheckboxDefaults.colors(),
                onCheckedChange = null
            )
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun SimplePersonaCardPreview() {
    RadixWalletTheme {
        SimplePersonaCard(persona = Persona.sampleMainnet())
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun SimplePersonaCardWithShadowPreview() {
    RadixWalletTheme {
        SimplePersonaCardWithShadow(persona = Persona.sampleMainnet())
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun PersonaCardPreview() {
    RadixWalletTheme {
        PersonaCard(persona = Persona.sampleMainnet())
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun PersonaCardWithAllSecurityProblemsPreview() {
    RadixWalletTheme {
        PersonaCard(
            persona = Persona.sampleMainnet(),
            securityPrompts = listOf(
                SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM,
                SecurityPromptType.WALLET_NOT_RECOVERABLE,
                SecurityPromptType.RECOVERY_REQUIRED,
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
