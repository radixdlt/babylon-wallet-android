package com.babylon.wallet.android.presentation

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.map
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class BalanceVisibilityObserver @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
) {

    val isBalanceVisible = getProfileUseCase.flow.map { it.appPreferences.display.isCurrencyAmountVisible }
}

@Suppress("CompositionLocalAllowlist")
val LocalBalanceVisibility = compositionLocalOf { true }
