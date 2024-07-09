package com.babylon.wallet.android.presentation.wallet.cards

import com.babylon.wallet.android.data.repository.homecards.HomeCardsRepository
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.wallet.WalletViewModel
import com.radixdlt.sargon.HomeCard
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class HomeCardsDelegate @Inject constructor(
    private val homeCardsRepository: HomeCardsRepository
) : ViewModelDelegate<WalletViewModel.State>() {

    override operator fun invoke(
        scope: CoroutineScope,
        state: MutableStateFlow<WalletViewModel.State>
    ) {
        super.invoke(scope, state)
        observeCards()
    }

    fun dismissCard(card: HomeCard) {
        viewModelScope.launch { homeCardsRepository.cardDismissed(card) }
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
