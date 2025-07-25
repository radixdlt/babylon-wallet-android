package com.babylon.wallet.android

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AnticipateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.babylon.wallet.android.LinkConnectionStatusObserver.LinkConnectionsStatus
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.edgeToEdge
import com.babylon.wallet.android.designsystem.theme.rememberRadixThemeConfig
import com.babylon.wallet.android.presentation.BalanceVisibilityObserver
import com.babylon.wallet.android.presentation.alerts.AlertHandler
import com.babylon.wallet.android.presentation.alerts.AlertUI
import com.babylon.wallet.android.presentation.alerts.AlertView
import com.babylon.wallet.android.presentation.lockscreen.AppLockActivity
import com.babylon.wallet.android.presentation.main.AppState
import com.babylon.wallet.android.presentation.main.MainViewModel
import com.babylon.wallet.android.presentation.ui.CustomCompositionProviders
import com.babylon.wallet.android.presentation.ui.composables.DevBannerState
import com.babylon.wallet.android.presentation.ui.composables.DevelopmentPreviewWrapper
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressViewEntryPoint
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.openInAppUrl
import com.radixdlt.sargon.os.driver.BiometricsHandler
import com.radixdlt.sargon.os.driver.OnBiometricsLifecycleCallbacks
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rdx.works.profile.cloudbackup.CloudBackupSyncExecutor
import javax.inject.Inject

// Extending from FragmentActivity because of Biometric
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

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

    // Automatic biometric handler that requests for biometrics when mnemonics are accessed from sargon os
    @Inject
    lateinit var biometricsHandler: BiometricsHandler

    @Inject
    lateinit var alertHandler: AlertHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            viewModel.state.value.initialAppState == AppState.Loading
        }
        setSplashExitAnimation(splashScreen)

        super.onCreate(savedInstanceState)
        biometricsHandler.register(
            activity = this,
            callbacks = object : OnBiometricsLifecycleCallbacks {
                override fun onBeforeBiometricsRequest() {
                    viewModel.onBeforeBiometricsRequest()
                }

                override fun onAfterBiometricsResult() {
                    viewModel.onAfterBiometricsResult()
                }
            }
        )
        cloudBackupSyncExecutor.startPeriodicChecks(lifecycleOwner = this)

        intent.data?.let {
            intent.replaceExtras(Bundle())
            viewModel.handleDeepLink(it)
        }

        setContent {
            val isDevBannerVisible by viewModel.isDevBannerVisible.collectAsStateWithLifecycle()

            val selectedTheme by viewModel.themeSelection.collectAsStateWithLifecycle()
            val themeConfig = rememberRadixThemeConfig(selectedTheme = selectedTheme)
            RadixWalletTheme(config = themeConfig) {
                SetupEdgeToEdge(isDevBannerVisible)

                val isVisible by balanceVisibilityObserver.isBalanceVisible.collectAsState(initial = true)
                CustomCompositionProviders(
                    isBalanceVisible = isVisible,
                    actionableAddressViewEntryPoint = actionableAddressViewEntryPoint
                ) {
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

                        HandleAlert()
                    }
                }
            }
        }

        monitorAdvancedLockState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppToForeground()
    }

    @Composable
    private fun SetupEdgeToEdge(isDevBannerVisible: Boolean) {
        val isDarkThemeEnabled = RadixTheme.config.isDarkTheme
        LaunchedEffect(isDarkThemeEnabled, isDevBannerVisible) {
            edgeToEdge(
                isDarkThemeEnabled = isDarkThemeEnabled,
                forceDarkStatusBar = isDevBannerVisible
            )
        }
    }

    @Composable
    private fun HandleAlert() {
        val alert = alertHandler.state().collectAsState().value
        val ui = AlertUI.from(alert) ?: return
        val context = LocalContext.current
        val toolbarColor = RadixTheme.colors.background.toArgb()

        AlertView(
            modifier = Modifier.throttleClickable {
                when (alert) {
                    is AlertHandler.State.NewBlogPost -> {
                        context.openInAppUrl(
                            url = alert.post.url.toString(),
                            toolbarColor = toolbarColor
                        )
                        alertHandler.reset()
                    }

                    AlertHandler.State.Idle -> {}
                }
            },
            ui = ui,
            autoDismiss = false,
            onDismiss = alertHandler::reset
        )
    }

    private fun monitorAdvancedLockState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { state ->
                    if (state.isAdvancedLockEnabled) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                    if (state.isAppLocked) {
                        startActivity(
                            Intent(this@MainActivity, AppLockActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            }
                        )
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

    companion object {
        private const val splashExitAnimDurationMs = 300L
    }
}
