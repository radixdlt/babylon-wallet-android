package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.dapp.selectpersona.PersonaUiModel
import rdx.works.profile.data.model.pernetwork.OnNetwork

@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries")
fun PersonaCard(modifier: Modifier, persona: PersonaUiModel, onSelectPersona: (OnNetwork.Persona) -> Unit) {
    val paddingDefault = RadixTheme.dimensions.paddingDefault
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(paddingDefault),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "",
                placeholder = painterResource(id = R.drawable.img_placeholder),
                fallback = painterResource(id = R.drawable.img_placeholder),
                error = painterResource(id = R.drawable.img_placeholder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RadixTheme.shapes.circle)
            )
            Text(
                modifier = Modifier.weight(1f),
                text = persona.persona.displayName,
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
                Divider(color = RadixTheme.colors.gray4)
                Spacer(modifier = Modifier.height(paddingDefault))
                Text(
                    modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.your_last_login, it),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(paddingDefault))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DAppLoginContentPreview() {
    RadixWalletTheme {
        PersonaCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            persona = PersonaUiModel(SampleDataProvider().samplePersona())
        ) {}
    }
}
