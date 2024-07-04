package com.babylon.wallet.android.presentation.wallet.cards

import com.babylon.wallet.android.data.repository.homecards.HomeCardsRepository
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.wallet.WalletUiState
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.HomeCard
import com.radixdlt.sargon.HomeCardsManager
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class HomeCardsDelegate @Inject constructor(
    private val homeCardsManager: HomeCardsManager,
    private val homeCardsRepository: HomeCardsRepository
) : ViewModelDelegate<WalletUiState>() {

    override operator fun invoke(
        scope: CoroutineScope,
        state: MutableStateFlow<WalletUiState>
    ) {
        super.invoke(scope, state)
        initCards()
        observeCards()
    }

    fun onCardClose(card: HomeCard) {
        viewModelScope.launch {
            runCatching { homeCardsManager.cardDismissed(card) }
                .onFailure { Timber.d("Failed to dismiss home card. Error: $it") }
                .onSuccess { Timber.d("$card dismissed") }
        }
    }

    private fun initCards() {
        viewModelScope.launch {
            runCatching { homeCardsManager.walletStarted() }
                .onFailure {
                    if (it is CommonException.HomeCardsNotFound) {
                        Timber.d("There are no home cards")
                    } else {
                        Timber.d("Failed to notify HomeCardsManager about wallet start. Error: $it")
                    }
                }
                .onSuccess { Timber.d("Notified HomeCardsManager about wallet start") }
        }
    }

    private fun observeCards() {
        viewModelScope.launch {
            homeCardsRepository.observeHomeCards()
                .collect {
                    _state.update { state ->
                        state.copy(
                            cards = it.toPersistentList()
                        )
                    }
                }
        }
    }
}
