package com.babylon.wallet.android.domain.usecases.settings

import rdx.works.core.preferences.PreferencesManager
import javax.inject.Inject

class GetImportOlympiaSettingVisibilityUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager,
) {

    operator fun invoke() = preferencesManager.isImportFromOlympiaSettingDismissed
}
