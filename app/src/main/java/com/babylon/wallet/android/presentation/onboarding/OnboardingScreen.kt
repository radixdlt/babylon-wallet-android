package com.babylon.wallet.android.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

@ExperimentalAnimationApi
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onCreateNewWalletClick: () -> Unit,
    onRestoreFromBackupClick: () -> Unit
) {
    BackHandler { onBack() }

    OnboardingScreenContent(
        onProceedClick = viewModel::onCreateNewWalletClick,
        onRestoreWalletClick = onRestoreFromBackupClick,
        modifier = modifier
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is OnboardingViewModel.OnBoardingEvent.CreateNewWallet -> {
                    onCreateNewWalletClick()
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreenContent(
    onProceedClick: () -> Unit,
    onRestoreWalletClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.weight(1f))

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
                onClick = onRestoreWalletClick
            )
            Spacer(Modifier.height(RadixTheme.dimensions.paddingXXLarge))
        }
    }
}

@Preview
@Composable
fun OnboardingScreenPreview() {
    RadixWalletTheme {
        OnboardingScreenContent(
            onProceedClick = {},
            onRestoreWalletClick = {}
        )
    }
}
