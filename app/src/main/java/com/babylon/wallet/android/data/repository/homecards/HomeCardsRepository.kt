package com.babylon.wallet.android.data.repository.homecards

import com.radixdlt.sargon.HomeCard
import com.radixdlt.sargon.HomeCardsManager
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

interface HomeCardsRepository {

    fun observeHomeCards(): Flow<List<HomeCard>>

    suspend fun cardDismissed(card: HomeCard)
}

class HomeCardsRepositoryImpl @Inject constructor(
    private val homeCardsManager: HomeCardsManager,
    private val homeCardsObserver: HomeCardsObserverWrapper
) : HomeCardsRepository {

    override fun observeHomeCards(): Flow<List<HomeCard>> = homeCardsObserver.observeHomeCards()

    override suspend fun cardDismissed(card: HomeCard) {
        runCatching { homeCardsManager.cardDismissed(card) }
            .onFailure { Timber.d("Failed to dismiss home card. Error: $it") }
            .onSuccess { Timber.d("$card dismissed") }
    }
}
