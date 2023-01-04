package com.babylon.wallet.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
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
}
