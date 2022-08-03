package com.babylon.wallet.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.babylon.wallet.android.presentation.ui.theme.BabylonWalletTheme
import com.babylon.wallet.android.presentation.wallet.WalletScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BabylonWalletTheme {
                WalletScreen()
            }
        }
    }
}
