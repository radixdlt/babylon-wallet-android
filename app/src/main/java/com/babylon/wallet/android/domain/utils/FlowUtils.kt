package com.babylon.wallet.android.domain.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

fun ticker(delay: Duration): Flow<Unit> {
    return flow {
        while (true) {
            emit(Unit)
            delay(delay)
        }
    }
}
