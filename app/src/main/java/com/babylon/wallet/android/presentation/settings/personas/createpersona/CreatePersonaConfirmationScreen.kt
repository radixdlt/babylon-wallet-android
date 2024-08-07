package com.babylon.wallet.android.presentation.settings.personas.createpersona

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
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
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

@Composable
fun CreatePersonaConfirmationScreen(
    viewModel: CreatePersonaConfirmationViewModel,
    modifier: Modifier = Modifier,
    finishPersonaCreation: () -> Unit,
    requestSource: CreatePersonaRequestSource
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CreatePersonaConfirmationContent(
        modifier = modifier,
        isFirstPersona = state.isFirstPersona,
        personaConfirmed = viewModel::personaConfirmed,
        requestSource = requestSource
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
    personaConfirmed: () -> Unit,
    requestSource: CreatePersonaRequestSource
) {
    BackHandler(enabled = true) { }

    Scaffold(
        modifier = modifier,
        contentColor = RadixTheme.colors.defaultBackground,
        contentWindowInsets = WindowInsets.statusBarsAndBanner.add(WindowInsets.navigationBars),
        bottomBar = {
            RadixBottomBar(
                text = stringResource(
                    id = R.string.createEntity_completion_goToDestination,
                    when (requestSource) {
                        CreatePersonaRequestSource.Settings -> stringResource(R.string.createEntity_completion_destinationPersonaList)
                        CreatePersonaRequestSource.DappRequest -> stringResource(R.string.createEntity_completion_destinationChoosePersonas)
                    }
                ),
                onClick = personaConfirmed,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingDefault
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
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
            Spacer(Modifier.weight(2f))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateAccountConfirmationContentPreview() {
    RadixWalletTheme {
        CreatePersonaConfirmationContent(
            modifier = Modifier,
            isFirstPersona = false,
            {},
            CreatePersonaRequestSource.Settings
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreateAccountConfirmationContentFirstPersonaPreview() {
    RadixWalletTheme {
        CreatePersonaConfirmationContent(
            modifier = Modifier,
            isFirstPersona = true,
            {},
            CreatePersonaRequestSource.DappRequest
        )
    }
}
