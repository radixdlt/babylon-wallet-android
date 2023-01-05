package com.babylon.wallet.android

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import dagger.hilt.android.AndroidEntryPoint

// Extending from FragmentActivity because of Biometric
@ExperimentalLifecycleComposeApi
@OptIn(ExperimentalPagerApi::class)
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            viewModel.state.value.loading
        }
        setSplashExitAnimation(splashScreen)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContent {
            RadixWalletTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                WalletApp(
                    showOnboarding = state.showOnboarding,
                    hasProfile = state.hasProfile,
                    oneOffEvent = viewModel.oneOffEvent
                )
            }
        }
    }

    private fun setSplashExitAnimation(splashScreen: SplashScreen) {
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val fadeIn = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.ALPHA,
                1f,
                0f
            )
            fadeIn.interpolator = AnticipateInterpolator()
            fadeIn.duration = splashExitAnimDurationMs
            fadeIn.doOnEnd { splashScreenView.remove() }
            fadeIn.start()
        }
    }

    companion object {
        private const val splashExitAnimDurationMs = 300L
    }
}
