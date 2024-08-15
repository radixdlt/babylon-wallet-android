package com.babylon.wallet.android

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AnticipateInterpolator
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.babylon.wallet.android.LinkConnectionStatusObserver.LinkConnectionsStatus
import com.babylon.wallet.android.designsystem.theme.DefaultDarkScrim
import com.babylon.wallet.android.designsystem.theme.DefaultLightScrim
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.BalanceVisibilityObserver
import com.babylon.wallet.android.presentation.dialogs.lock.AppLockActivity
import com.babylon.wallet.android.presentation.main.AppState
import com.babylon.wallet.android.presentation.main.MainViewModel
import com.babylon.wallet.android.presentation.ui.CustomCompositionProviders
import com.babylon.wallet.android.presentation.ui.composables.DevBannerState
import com.babylon.wallet.android.presentation.ui.composables.DevelopmentPreviewWrapper
import com.babylon.wallet.android.presentation.ui.composables.LockScreenBackground
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressViewEntryPoint
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rdx.works.profile.cloudbackup.CloudBackupSyncExecutor
import javax.inject.Inject

// Extending from FragmentActivity because of Biometric
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private lateinit var windowManager: WindowManager

    private lateinit var privacyOverlay: ComposeView

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
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupPrivacyOverlay()
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            viewModel.state.value.initialAppState == AppState.Loading
        }
        setSplashExitAnimation(splashScreen)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(
                scrim = DefaultLightScrim,
                darkScrim = DefaultDarkScrim
            )
        )
        super.onCreate(savedInstanceState)
        cloudBackupSyncExecutor.startPeriodicChecks(lifecycleOwner = this)

        intent.data?.let {
            intent.replaceExtras(Bundle())
            viewModel.handleDeepLink(it)
        }
        setContent {
            RadixWalletTheme {
                SyncEdgeToEdgeSetupWithSelectedTheme()
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
                    ) {
                        WalletApp(
                            mainViewModel = viewModel,
                            onCloseApp = { finish() }
                        )
                    }
                }
            }
        }
        monitorLockState()
    }

    @Composable
    private fun SyncEdgeToEdgeSetupWithSelectedTheme() {
        val isSystemInDarkTheme = isSystemInDarkTheme()
        DisposableEffect(isSystemInDarkTheme) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                ) { isSystemInDarkTheme },
                navigationBarStyle = SystemBarStyle.light(
                    scrim = DefaultLightScrim,
                    darkScrim = DefaultDarkScrim
                ),
            )
            onDispose {}
        }
    }

    private fun setupPrivacyOverlay() {
        privacyOverlay = ComposeView(this).apply {
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
            setViewTreeLifecycleOwner(this@MainActivity)
        }
        privacyOverlay.setContent {
            RadixWalletTheme {
                LockScreenBackground()
            }
        }
    }

    private fun monitorLockState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { state ->
                    if (state.isAppLockEnabled) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                    if (state.isAppLocked) {
                        startActivity(
                            Intent(this@MainActivity, AppLockActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            }
                        )
                    }
                    try {
                        windowManager.removeView(privacyOverlay)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        /**
         * AppsFlyer DeepLinkListener.onDeepLinking is not called when
         * the app is running in the background and Application LaunchMode is not standard.
         * The following line corrects this, as per AppsFlyer docs:
         * https://dev.appsflyer.com/hc/docs/dl_android_unified_deep_linking
         */
        setIntent(intent)

        intent.data?.let {
            this.intent.replaceExtras(Bundle())
            viewModel.handleDeepLink(it)
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

    override fun onPause() {
        super.onPause()
        viewModel.onAppToBackground()
    }

    override fun onUserLeaveHint() {
        if (viewModel.state.value.isAppLockEnabled) {
            val params = WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
                token = privacyOverlay.applicationWindowToken
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                format = PixelFormat.TRANSLUCENT
                flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            }
            windowManager.addView(privacyOverlay, params)
        }
        super.onUserLeaveHint()
    }

    companion object {
        private const val splashExitAnimDurationMs = 300L
    }
}
