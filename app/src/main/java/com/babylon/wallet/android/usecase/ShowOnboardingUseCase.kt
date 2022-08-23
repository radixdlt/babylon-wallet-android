package com.babylon.wallet.android.usecase

import com.babylon.wallet.android.data.DataStoreManager
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowOnboardingUseCase @Inject constructor(
    preferencesManager: DataStoreManager,
    @ApplicationScope applicationScope: CoroutineScope
) {

    val showOnboarding: StateFlow<Boolean> = preferencesManager.showOnboarding
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )
}
