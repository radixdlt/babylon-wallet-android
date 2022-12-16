package com.babylon.wallet.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.navigation.NavigationHost
import com.babylon.wallet.android.presentation.navigation.Screen
import com.google.accompanist.pager.ExperimentalPagerApi
import dagger.hilt.android.AndroidEntryPoint

@ExperimentalLifecycleComposeApi
@OptIn(ExperimentalPagerApi::class)
@AndroidEntryPoint
// Extending from FragmentActivity because of Biometric
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContent {
            BabylonWalletTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()

                // TODO when we import splash screen, it should be used here instead of FullscreenCircularProgress
                if (state.loading) {
                    FullscreenCircularProgressContent()
                } else if (state.showOnboarding) {
                    NavigationHost(
                        Screen.OnboardingDestination.route
                    )
                } else {
                    if (state.hasProfile) {
                        NavigationHost(
                            Screen.WalletDestination.route
                        )
                    } else {
                        NavigationHost(
                            Screen.CreateAccountDestination.route()
                        )
                    }
                }
            }
        }
    }
}
