package com.babylon.wallet.android.presentation

import androidx.compose.runtime.compositionLocalOf
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.isBalanceVisible
import javax.inject.Inject

class BalanceVisibilityObserver @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
) {

    val isBalanceVisible = getProfileUseCase.isBalanceVisible
}

@Suppress("CompositionLocalAllowlist")
val LocalBalanceVisibility = compositionLocalOf { true }
