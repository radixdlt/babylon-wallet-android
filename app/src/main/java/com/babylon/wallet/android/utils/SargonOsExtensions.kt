package com.babylon.wallet.android.utils

import com.radixdlt.sargon.SargonOs
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

suspend fun <T> SargonOsManager.callSafely(
    dispatcher: CoroutineDispatcher,
    block: SargonOs.() -> T
): Result<T> = withContext(dispatcher) {
    runCatching { sargonOs.block() }
}
