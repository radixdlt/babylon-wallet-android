package com.babylon.wallet.android.domain.usecases.settings

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.invoke
import rdx.works.core.di.IoDispatcher
import rdx.works.core.preferences.PreferencesManager
import javax.inject.Inject

class MarkImportOlympiaWalletCompleteUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke() {
        ioDispatcher.invoke {
            preferencesManager.markImportFromOlympiaComplete()
        }
    }
}
