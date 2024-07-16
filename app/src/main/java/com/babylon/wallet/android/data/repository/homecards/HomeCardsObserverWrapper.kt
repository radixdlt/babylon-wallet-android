package com.babylon.wallet.android.data.repository.homecards

import com.radixdlt.sargon.HomeCard
import com.radixdlt.sargon.HomeCardsObserver
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

interface HomeCardsObserverWrapper : HomeCardsObserver {

    fun observeHomeCards(): Flow<List<HomeCard>>
}

class HomeCardsObserverWrapperImpl @Inject constructor() : HomeCardsObserverWrapper {

    private val homeCardsFlow = MutableSharedFlow<List<HomeCard>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun observeHomeCards(): Flow<List<HomeCard>> = homeCardsFlow

    override fun handleCardsUpdate(cards: List<HomeCard>) {
        homeCardsFlow.tryEmit(cards)
    }
}
