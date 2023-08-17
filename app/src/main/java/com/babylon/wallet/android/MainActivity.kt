package com.babylon.wallet.android

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.main.AppState
import com.babylon.wallet.android.presentation.main.MainViewModel
import com.babylon.wallet.android.presentation.ui.composables.DevelopmentPreviewWrapper
import dagger.hilt.android.AndroidEntryPoint

// Extending from FragmentActivity because of Biometric
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            viewModel.state.value.initialAppState == AppState.Loading
        }
        setSplashExitAnimation(splashScreen)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            RadixWalletTheme {
                DevelopmentPreviewWrapper {
                    WalletApp(mainViewModel = viewModel, onCloseApp = {
                        finishAffinity()
                    })
                }
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

    override fun onResume() {
        super.onResume()
        viewModel.checkMnemonicIntegrity()
    }

    companion object {
        private const val splashExitAnimDurationMs = 300L
    }
}
