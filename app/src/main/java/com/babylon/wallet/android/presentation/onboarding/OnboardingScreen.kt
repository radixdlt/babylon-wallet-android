package com.babylon.wallet.android.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onCreateNewWalletClick: () -> Unit,
    onRestoreFromBackupClick: () -> Unit
) {
    BackHandler { onBack() }

    val state by viewModel.state.collectAsStateWithLifecycle()

    OnboardingScreenContent(
        modifier = modifier,
        state = state,
        onProceedClick = onCreateNewWalletClick,
        onRestoreWalletClick = onRestoreFromBackupClick,
        onClaimedByAnotherDeviceWarningAcknowledged = viewModel::claimedByAnotherDeviceWarningAcknowledged
    )
}

@Composable
private fun OnboardingScreenContent(
    modifier: Modifier = Modifier,
    state: OnboardingViewModel.State,
    onProceedClick: () -> Unit,
    onRestoreWalletClick: () -> Unit,
    onClaimedByAnotherDeviceWarningAcknowledged: () -> Unit
) {
    if (state.isProfileClaimedByAnotherDeviceWarningVisible) {
        BasicPromptAlertDialog(
            finish = { accepted ->
                if (accepted) {
                    onClaimedByAnotherDeviceWarningAcknowledged()
                }
            },
            message = {
                Text(text = "The profile was claimed by another device") // TODO crowdin
            },
            dismissText = null
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
                Text(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .padding(
                            top = RadixTheme.dimensions.paddingLarge,
                            bottom = RadixTheme.dimensions.paddingDefault
                        ),
                    text = stringResource(id = R.string.onboarding_step1_title),
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.Center
                )

                Text(
                    modifier = Modifier.widthIn(max = 300.dp),
                    text = stringResource(id = R.string.onboarding_step1_subtitle),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.gray2,
                    textAlign = TextAlign.Center
                )
            }

            OnboardingGraphic(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            RadixPrimaryButton(
                text = stringResource(id = R.string.onboarding_newUser),
                onClick = onProceedClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
            )
            RadixTextButton(
                text = stringResource(id = R.string.onboarding_restoreFromBackup),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                onClick = onRestoreWalletClick,
                textStyle = RadixTheme.typography.body1StandaloneLink
            )
            Spacer(Modifier.height(RadixTheme.dimensions.paddingXXXXLarge))
        }
    }
}

@Preview
@Composable
fun OnboardingScreenPreview() {
    RadixWalletTheme {
        OnboardingScreenContent(
            state = OnboardingViewModel.State(
                isProfileClaimedByAnotherDeviceWarningVisible = false
            ),
            onProceedClick = {},
            onRestoreWalletClick = {},
            onClaimedByAnotherDeviceWarningAcknowledged = {}
        )
    }
}

@Preview
@Composable
fun OnboardingScreenWithClaimedByAnotherDeviceWarningPreview() {
    RadixWalletTheme {
        OnboardingScreenContent(
            state = OnboardingViewModel.State(
                isProfileClaimedByAnotherDeviceWarningVisible = true
            ),
            onProceedClick = {},
            onRestoreWalletClick = {},
            onClaimedByAnotherDeviceWarningAcknowledged = {}
        )
    }
}
