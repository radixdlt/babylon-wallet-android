package com.babylon.wallet.android.presentation.discover

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.discover.DiscoverRepository
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.discover.common.models.SocialLinkType
import com.radixdlt.sargon.BlogPost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_NUMBER_OF_BLOG_POSTS = 3

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository
) : StateViewModel<DiscoverViewModel.State>() {

    init {
        initBlogPosts()
    }

    override fun initialState(): State = State(isBlogPostsLoading = true)

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    private fun initBlogPosts() {
        viewModelScope.launch {
            discoverRepository.getBlogPosts()
                .onSuccess { posts ->
                    _state.update {
                        it.copy(
                            blogPosts = posts.take(MAX_NUMBER_OF_BLOG_POSTS).toPersistentList(),
                            isBlogPostsLoading = false
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            uiMessage = UiMessage.ErrorMessage(error),
                            isBlogPostsLoading = false
                        )
                    }
                }
        }
    }

    data class State(
        val isBlogPostsLoading: Boolean,
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
