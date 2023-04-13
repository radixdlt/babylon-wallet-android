package com.babylon.wallet.android.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import rdx.works.core.preferences.PreferencesManager
import java.time.Instant
import javax.inject.Inject

class GetLastBackupInstantUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager
) {

    operator fun invoke(): Flow<Instant?> = preferencesManager.lastBackupInstant
}
