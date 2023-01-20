package com.babylon.wallet.android.presentation.createpersona

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.SetStatusBarColor
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.createaccount.CreatedAccountCardStack

@Composable
fun CreatePersonaConfirmationScreen(
    viewModel: CreatePersonaConfirmationViewModel,
    modifier: Modifier = Modifier,
    finishPersonaCreation: () -> Unit
) {
    val personaState = viewModel.personaUiState

    SetStatusBarColor(color = RadixTheme.colors.orange2, useDarkIcons = !isSystemInDarkTheme())
    CreatePersonaConfirmationContent(
        modifier = modifier,
        personaName = personaState.personaName,
        personaId = personaState.personaAddressTruncated,
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
    personaName: String,
    personaId: String,
    personaConfirmed: () -> Unit
) {
    Column(
        modifier = modifier.background(RadixTheme.colors.defaultBackground)
            .navigationBarsPadding()
            .fillMaxSize()
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingDefault
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.2f))
        CreatedAccountCardStack(
            Modifier.fillMaxWidth(0.8f),
            accountName = personaName,
            accountAddress = personaId
        )
        Spacer(modifier = Modifier.weight(0.2f))
        Text(
            text = stringResource(id = R.string.congratulations),
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Text(
            text = stringResource(id = R.string.your_persona_has_been_created),
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.gray1
        )
        Spacer(Modifier.weight(0.6f))
        RadixPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.go_to_settings),
            onClick = personaConfirmed
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
            personaConfirmed = {},
            personaName = "My persona",
            personaId = "d32d32"
        )
    }
}
