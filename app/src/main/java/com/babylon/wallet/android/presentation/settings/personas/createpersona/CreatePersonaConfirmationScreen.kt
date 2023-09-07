package com.babylon.wallet.android.presentation.settings.personas.createpersona

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun CreatePersonaConfirmationScreen(
    viewModel: CreatePersonaConfirmationViewModel,
    modifier: Modifier = Modifier,
    finishPersonaCreation: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CreatePersonaConfirmationContent(
        modifier = modifier,
        isFirstPersona = state.isFirstPersona,
        personaConfirmed = viewModel::personaConfirmed
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is CreatePersonaConfirmationEvent.FinishPersonaCreation -> finishPersonaCreation()
            }
        }
    }
}

@Composable
fun CreatePersonaConfirmationContent(
    modifier: Modifier,
    isFirstPersona: Boolean,
    personaConfirmed: () -> Unit
) {
    Column(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground)
            .navigationBarsPadding()
            .statusBarsPadding()
            .fillMaxSize()
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingDefault
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.2f))
        Text(
            text = stringResource(id = R.string.createEntity_completion_title),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Text(
            text = stringResource(
                id =
                if (isFirstPersona) {
                    R.string.createPersona_completion_subtitleFirst
                } else {
                    R.string.createPersona_completion_subtitleNotFirst
                }
            ),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Text(
            text = stringResource(id = R.string.createPersona_completion_explanation),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(0.6f))
        RadixPrimaryButton(
            text = stringResource(R.string.createEntity_completion_destinationChoosePersonas),
            onClick = personaConfirmed,
            modifier = Modifier.fillMaxWidth()
        )
    }
    BackHandler(enabled = true) { }
}

@Preview(showBackground = true)
@Composable
fun CreateAccountConfirmationContentPreview() {
    RadixWalletTheme {
        CreatePersonaConfirmationContent(
            modifier = Modifier,
            isFirstPersona = false
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
fun CreateAccountConfirmationContentFirstPersonaPreview() {
    RadixWalletTheme {
        CreatePersonaConfirmationContent(
            modifier = Modifier,
            isFirstPersona = true
        ) {}
    }
}
