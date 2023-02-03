package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.dapp.login.PersonaUiModel

@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries")
fun PersonaCard(modifier: Modifier, persona: PersonaUiModel, onSelectPersona: (PersonaUiModel) -> Unit) {
    val paddingDefault = RadixTheme.dimensions.paddingDefault
    val hasLastUsedDate = persona.lastUsedOn != null
    val hasSharingSection = persona.sharedAccountNumber > 0
    ConstraintLayout(modifier) {
        val (image, title, radio, sharingSection, lastUsed) = createRefs()
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
                .constrainAs(image) {
                    top.linkTo(parent.top, paddingDefault)
                    start.linkTo(parent.start, paddingDefault)
                    if (hasLastUsedDate && !hasSharingSection) {
                        bottom.linkTo(lastUsed.top, paddingDefault)
                    }
                }
        )
        Text(
            modifier = Modifier.constrainAs(title) {
                top.linkTo(image.top)
                linkTo(start = image.end, end = radio.end, bias = 0f, startMargin = paddingDefault)
            },
            text = persona.persona.displayName,
            textAlign = TextAlign.Start,
            maxLines = 2,
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1
        )
        if (hasSharingSection) {
            Column(
                Modifier.constrainAs(sharingSection) {
                    top.linkTo(title.bottom)
                    if (hasLastUsedDate) {
                        bottom.linkTo(lastUsed.top, paddingDefault)
                    }
                    linkTo(start = image.end, end = radio.end, bias = 0f, startMargin = paddingDefault)
                }
            ) {
                Text(
                    text = stringResource(id = R.string.sharing),
                    textAlign = TextAlign.Start,
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.gray2
                )
                Text(
                    text = pluralStringResource(
                        id = R.plurals.number_of_accounts,
                        persona.sharedAccountNumber,
                        persona.sharedAccountNumber
                    ),
                    textAlign = TextAlign.Start,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2
                )
            }
        }
        RadioButton(
            modifier = Modifier.constrainAs(radio) {
                end.linkTo(parent.end, paddingDefault)
                if (hasLastUsedDate) {
                    top.linkTo(parent.top)
                    bottom.linkTo(lastUsed.top)
                } else {
                    centerVerticallyTo(parent)
                }
            },
            selected = persona.selected,
            onClick = {
                onSelectPersona(persona)
            },
            colors = RadioButtonDefaults.colors(
                selectedColor = RadixTheme.colors.gray1,
                unselectedColor = RadixTheme.colors.gray4
            ),
        )
        persona.lastUsedOn?.let {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(lastUsed) {
                        centerHorizontallyTo(parent)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.fillToConstraints
                    }
            ) {
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
            persona = PersonaUiModel(SampleDataProvider().samplePersona()),
            onSelectPersona = {}
        )
    }
}
