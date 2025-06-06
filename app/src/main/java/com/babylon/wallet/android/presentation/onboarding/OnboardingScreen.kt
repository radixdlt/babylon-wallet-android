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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel,
    onBack: () -> Unit,
    onCreateNewWalletClick: (Boolean) -> Unit,
    onShowEula: () -> Unit,
    onRestoreFromBackupClick: () -> Unit
) {
    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is OnboardingViewModel.OnboardingEvent.NavigateToCreateNewWallet -> onCreateNewWalletClick(event.isWithCloudBackupEnabled)
                is OnboardingViewModel.OnboardingEvent.NavigateToEula -> onShowEula()
            }
        }
    }

    OnboardingScreenContent(
        modifier = modifier,
        onProceedClick = viewModel::onCreateNewWalletClick,
        onRestoreWalletClick = onRestoreFromBackupClick,
    )
}

@Composable
private fun OnboardingScreenContent(
    modifier: Modifier = Modifier,
    onProceedClick: () -> Unit,
    onRestoreWalletClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = RadixTheme.colors.background
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
                    color = RadixTheme.colors.text,
                    textAlign = TextAlign.Center
                )

                Text(
                    modifier = Modifier.widthIn(max = 300.dp),
                    text = stringResource(id = R.string.onboarding_step1_subtitle),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.textSecondary,
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
            onProceedClick = {},
            onRestoreWalletClick = {},
        )
    }
}
