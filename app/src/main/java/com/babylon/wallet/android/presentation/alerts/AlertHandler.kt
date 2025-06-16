package com.babylon.wallet.android.presentation.alerts

import com.radixdlt.sargon.BlogPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface AlertHandler {

    fun state(): StateFlow<State>

    fun show(state: State)

    fun reset()

    sealed interface State {

        data object Idle : State

        data class NewBlogPost(
            val post: BlogPost
        ) : State
    }
}

@Singleton
class AlertHandlerImpl @Inject constructor() : AlertHandler {

    private val state = MutableStateFlow<AlertHandler.State>(AlertHandler.State.Idle)

    override fun state(): StateFlow<AlertHandler.State> = state

    override fun show(newState: AlertHandler.State) {
        state.value = newState
    }

    override fun reset() {
        state.value = AlertHandler.State.Idle
    }
}
