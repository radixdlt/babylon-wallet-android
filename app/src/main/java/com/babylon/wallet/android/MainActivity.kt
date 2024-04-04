package com.babylon.wallet.android

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.babylon.wallet.android.LinkConnectionStatusObserver.LinkConnectionsStatus
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.BalanceVisibilityObserver
import com.babylon.wallet.android.presentation.main.AppState
import com.babylon.wallet.android.presentation.main.MainViewModel
import com.babylon.wallet.android.presentation.ui.CustomCompositionProviders
import com.babylon.wallet.android.presentation.ui.composables.DevBannerState
import com.babylon.wallet.android.presentation.ui.composables.DevelopmentPreviewWrapper
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressViewEntryPoint
import dagger.hilt.android.AndroidEntryPoint
import rdx.works.profile.cloudbackup.CloudBackupSyncExecutor
import javax.inject.Inject

// Extending from FragmentActivity because of Biometric
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var linkConnectionStatusObserver: LinkConnectionStatusObserver

    @Inject
    lateinit var balanceVisibilityObserver: BalanceVisibilityObserver

    @Inject
    lateinit var cloudBackupSyncExecutor: CloudBackupSyncExecutor

    // The actual ActionableAddressViewEntryPoint that is used in the app.
    // During development we use a mock ActionableAddressViewEntryPoint in order to have previews.
    @Inject
    lateinit var actionableAddressViewEntryPoint: ActionableAddressViewEntryPoint

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            viewModel.state.value.initialAppState == AppState.Loading
        }
        setSplashExitAnimation(splashScreen)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        cloudBackupSyncExecutor.startPeriodicChecks(lifecycleOwner = this)

        setContent {
            RadixWalletTheme {
                val isVisible by balanceVisibilityObserver.isBalanceVisible.collectAsState(initial = true)

                CustomCompositionProviders(
                    isBalanceVisible = isVisible,
                    actionableAddressViewEntryPoint = actionableAddressViewEntryPoint
                ) {
                    val isDevBannerVisible by viewModel.isDevBannerVisible.collectAsState(initial = true)
                    val devBannerState by remember(isDevBannerVisible) {
                        derivedStateOf { DevBannerState(isVisible = isDevBannerVisible) }
                    }

                    var linkConnectionsStatus: LinkConnectionsStatus? = null
                    if (BuildConfig.EXPERIMENTAL_FEATURES_ENABLED) {
                        val isLinkConnectionsStatusEnabled by linkConnectionStatusObserver.isEnabled.collectAsState()
                        if (isLinkConnectionsStatusEnabled) {
                            linkConnectionsStatus = linkConnectionStatusObserver.currentStatus.collectAsState().value
                        }
                    }

                    DevelopmentPreviewWrapper(
                        devBannerState = devBannerState,
                        linkConnectionsStatus = linkConnectionsStatus
                    ) { padding ->
                        WalletApp(
                            modifier = Modifier.padding(padding),
                            mainViewModel = viewModel,
                            onCloseApp = { finish() }
                        )
                    }
                }
            }
        }
        // ATTENTION: This was auto-generated to handle app links.
        val appLinkIntent: Intent = intent
        val appLinkAction: String? = appLinkIntent.action
        val appLinkData: Uri? = appLinkIntent.data
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
