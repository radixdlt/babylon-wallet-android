package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.cache.database.StateDao
import com.babylon.wallet.android.data.repository.stream.StreamRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class UpdateAccountFirstTransactionDateUseCase @Inject constructor(
    private val streamRepository: StreamRepository,
    private val stateDao: StateDao,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        accountAddress: String
    ): Result<Instant?> {
        return streamRepository.getAccountFirstTransactionDate(accountAddress).mapCatching { response ->
            response.items.firstOrNull()?.confirmedAt?.toInstant()
        }.onSuccess { firstTransactionDate ->
            withContext(dispatcher) {
                stateDao.updateAccountFirstTransactionDate(accountAddress, firstTransactionDate)
            }
        }
    }
}
