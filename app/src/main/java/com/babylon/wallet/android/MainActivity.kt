package com.babylon.wallet.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.ui.theme.BabylonWalletTheme
import com.babylon.wallet.android.usecase.ShowOnboardingUseCase
import com.google.accompanist.pager.ExperimentalPagerApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@ExperimentalLifecycleComposeApi
@OptIn(ExperimentalPagerApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var showOnboardingUseCase: ShowOnboardingUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BabylonWalletTheme {
                val showOnboarding = showOnboardingUseCase.showOnboarding.collectAsStateWithLifecycle()
                if (showOnboarding.value) {
                    NavigationHost(
                        Screen.OnboardingDestination.route
                    )
                } else {
                    NavigationHost(
                        Screen.WalletDestination.route
                    )
                }
            }
        }
    }
}
