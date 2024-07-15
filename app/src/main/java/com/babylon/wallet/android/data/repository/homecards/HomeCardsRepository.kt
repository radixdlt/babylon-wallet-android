package com.babylon.wallet.android.data.repository.homecards

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.radixdlt.sargon.HomeCard
import com.radixdlt.sargon.HomeCardsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

interface HomeCardsRepository {

    fun observeHomeCards(): Flow<List<HomeCard>>

    suspend fun bootstrap()

    suspend fun deferredDeepLinkReceived(value: String)

    suspend fun walletCreated()

    suspend fun cardDismissed(card: HomeCard)

    suspend fun walletReset()
}

class HomeCardsRepositoryImpl @Inject constructor(
    private val homeCardsManager: HomeCardsManager,
    private val homeCardsObserver: HomeCardsObserverWrapper,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : HomeCardsRepository {

    override fun observeHomeCards(): Flow<List<HomeCard>> {
        return homeCardsObserver.observeHomeCards()
    }

    override suspend fun bootstrap() {
        withContext(defaultDispatcher) {
            runCatching { homeCardsManager.bootstrap() }
                .onFailure { Timber.w("HomeCardsManager init error: ${it.message}") }
                .onSuccess { Timber.d("Successfully initialized HomeCardsManager") }
        }
    }

    override suspend fun deferredDeepLinkReceived(value: String) {
        withContext(defaultDispatcher) {
            runCatching { homeCardsManager.deferredDeepLinkReceived(value) }
                .onFailure { Timber.w("Failed to notify HomeCardsManager about deep link receiving. Error: $it") }
                .onSuccess { Timber.d("Notified HomeCardsManager about deep link receiving") }
        }
    }

    override suspend fun walletCreated() {
        withContext(defaultDispatcher) {
            runCatching { homeCardsManager.walletCreated() }
                .onFailure { Timber.w("Failed to notify HomeCardsManager about wallet creation. Error: $it") }
                .onSuccess { Timber.d("Notified HomeCardsManager about wallet creation") }
        }
    }

    override suspend fun cardDismissed(card: HomeCard) {
        withContext(defaultDispatcher) {
            runCatching { homeCardsManager.cardDismissed(card) }
                .onFailure { Timber.w("Failed to dismiss home card. Error: $it") }
                .onSuccess { Timber.d("$card dismissed") }
        }
    }

    override suspend fun walletReset() {
        withContext(defaultDispatcher) {
            runCatching { homeCardsManager.walletReset() }
                .onFailure { Timber.w("Failed to notify HomeCardsManager about wallet reset. Error: $it") }
                .onSuccess { Timber.d("Notified HomeCardsManager about wallet reset") }
        }
    }
}
