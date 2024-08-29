package com.babylon.wallet.android.presentation.dialogs.lock

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppLockActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RadixWalletTheme {
                AppLockScreen(onUnlock = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
                    } else {
                        overridePendingTransition(0, 0)
                    }
                    finish()
                })
            }
        }
    }
}
