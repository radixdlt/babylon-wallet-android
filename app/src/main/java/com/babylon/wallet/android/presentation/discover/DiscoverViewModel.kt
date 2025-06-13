package com.babylon.wallet.android.presentation.discover

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.discover.common.models.SocialLinkType
import com.radixdlt.sargon.BlogPost
import com.radixdlt.sargon.BlogPostsClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_NUMBER_OF_BLOG_POSTS = 3

@HiltViewModel
class DiscoverViewModel @Inject constructor() : StateViewModel<DiscoverViewModel.State>() {

    init {
        viewModelScope.launch {
            runCatching {
                BlogPostsClient().getBlogPosts()
            }.onSuccess { posts ->
                _state.update {
                    it.copy(
                        blogPosts = posts.take(MAX_NUMBER_OF_BLOG_POSTS).toPersistentList()
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
            }
        }
    }

    override fun initialState(): State = State()

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    data class State(
        val isRefreshing: Boolean = false,
        val uiMessage: UiMessage? = null,
        val glossaryItems: ImmutableList<GlossaryItem> = persistentListOf(
            GlossaryItem.radixnetwork,
            GlossaryItem.guarantees,
            GlossaryItem.nfts
        ),
        val socialLinks: ImmutableList<SocialLinkType> = SocialLinkType.entries.toPersistentList(),
        val blogPosts: ImmutableList<BlogPost> = persistentListOf()
    ) : UiState
}
