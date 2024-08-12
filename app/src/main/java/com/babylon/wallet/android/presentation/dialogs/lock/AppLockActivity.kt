package com.babylon.wallet.android.presentation.dialogs.lock

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import com.babylon.wallet.android.designsystem.theme.NavigationBarDefaultScrim
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppLockActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = NavigationBarDefaultScrim.toArgb(),
                darkScrim = NavigationBarDefaultScrim.toArgb()
            )
        )
        super.onCreate(savedInstanceState)
        setContent {
            RadixWalletTheme {
                AppLockScreen(onUnlock = {
                    finish()
                })
            }
        }
    }
}