package com.babylon.wallet.android.presentation.discover.blogposts

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.discover.DiscoverRepository
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.BlogPost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlogPostsViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository
) : StateViewModel<BlogPostsViewModel.State>() {

    init {
        initBlogPosts()
    }

    override fun initialState(): State = State(isLoading = true)

    fun onMessageShown() {
        _state.update { state -> state.copy(uiMessage = null) }
    }

    private fun initBlogPosts() {
        viewModelScope.launch {
            discoverRepository.getBlogPosts()
                .onFailure {
                    _state.update { state ->
                        state.copy(
                            uiMessage = UiMessage.ErrorMessage(it),
                            isLoading = false
                        )
                    }
                }
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            blogPosts = it.toPersistentList(),
                            isLoading = false
                        )
                    }
                }
        }
    }

    data class State(
        val isLoading: Boolean,
        val blogPosts: ImmutableList<BlogPost> = persistentListOf(),
        val uiMessage: UiMessage? = null
    ) : UiState
}
