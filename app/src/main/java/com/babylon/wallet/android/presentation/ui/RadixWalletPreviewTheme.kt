package com.babylon.wallet.android.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixThemeConfig
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ProvideMockActionableAddressViewEntryPoint
import com.babylon.wallet.android.presentation.ui.composables.card.ProvideMockFactorSourceCardViewEntryPoint

@Composable
fun RadixWalletPreviewTheme(
    modifier: Modifier = Modifier,
    enableDarkTheme: Boolean = isSystemInDarkTheme(),
    backgroundType: PreviewBackgroundType = PreviewBackgroundType.NONE,
    content: @Composable () -> Unit,
) {
    ProvideMockActionableAddressViewEntryPoint {
        ProvideMockFactorSourceCardViewEntryPoint {
            RadixWalletTheme(
                config = RadixThemeConfig(
                    isDarkTheme = enableDarkTheme
                ),
                content = {
                    if (backgroundType != PreviewBackgroundType.NONE) {
                        Box(
                            modifier = modifier.background(
                                color = when (backgroundType) {
                                    PreviewBackgroundType.PRIMARY -> RadixTheme.colors.background
                                    PreviewBackgroundType.SECONDARY -> RadixTheme.colors.backgroundSecondary
                                    PreviewBackgroundType.NONE -> error("Not Possible")
                                }
                            )
                        ) {
                            content()
                        }
                    } else {
                        content()
                    }
                }
            )
        }
    }
}

enum class PreviewBackgroundType {
    PRIMARY,
    SECONDARY,
    NONE
}
