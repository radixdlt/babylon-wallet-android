package com.babylon.wallet.android.presentation.lockscreen

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.utils.AppLifecycleObserver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppLockActivity : FragmentActivity() {

    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        appLifecycleObserver.onTopResumedActivityChanged(this, isTopResumedActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RadixWalletTheme {
                AppLockScreen(onUnlock = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
                    } else {
                        overridePendingTransition(0, 0)
                    }
                    finish()
                })
            }
        }
    }
}
