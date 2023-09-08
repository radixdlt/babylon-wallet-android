package com.babylon.wallet.android

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.main.AppState
import com.babylon.wallet.android.presentation.main.MainViewModel
import com.babylon.wallet.android.presentation.ui.composables.DevBannerState
import com.babylon.wallet.android.presentation.ui.composables.DevelopmentPreviewWrapper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
                val isDevBannerVisible by viewModel.isDevBannerVisible.collectAsState(initial = true)
                val devBannerState by remember(isDevBannerVisible) {
                    derivedStateOf { DevBannerState(isVisible = isDevBannerVisible) }
                }

                DevelopmentPreviewWrapper(devBannerState = devBannerState) { padding ->
                    WalletApp(
                        modifier = Modifier.padding(padding),
                        mainViewModel = viewModel,
                        onCloseApp = { finish() }
                    )

                    // TODO ONLY FOR TESTING PURPOSES
                    val isLive by viewModel.networkInfoRepository.isMainnetLive.collectAsState()
                    val scope = rememberCoroutineScope()
                    TextButton(
                        modifier = Modifier.padding(top = 60.dp),
                        onClick = {
                            scope.launch { viewModel.networkInfoRepository.isMainnetLive.update { !it } }
                        }
                    ) {
                        Text(
                            text = if (isLive) "\uD83D\uDFE2 Online" else "\uD83D\uDD34 Offline",
                            color = Color.Black
                        )
                    }
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
            fadeIn.doOnEnd {
                splashScreenView.remove()
            }
            fadeIn.start()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppToForeground()
    }

    companion object {
        private const val splashExitAnimDurationMs = 300L
    }
}
