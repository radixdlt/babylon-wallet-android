package com.babylon.wallet.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.domain.usecase.ShowOnboardingUseCase
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.navigation.Screen
import com.google.accompanist.pager.ExperimentalPagerApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@ExperimentalLifecycleComposeApi
@OptIn(ExperimentalPagerApi::class)
@AndroidEntryPoint
// Extending from FragmentActivity because of Biometric
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var showOnboardingUseCase: ShowOnboardingUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContent {
            BabylonWalletTheme {
                val showOnboarding = showOnboardingUseCase.showOnboarding.collectAsStateWithLifecycle().value
                if (showOnboarding) {
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
