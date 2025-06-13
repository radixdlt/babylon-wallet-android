package com.babylon.wallet.android.presentation.discover.blogposts

import com.babylon.wallet.android.data.repository.discover.DiscoverRepository
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.BlogPost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import javax.inject.Inject

@HiltViewModel
class BlogPostsViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository
) : StateViewModel<BlogPostsViewModel.State>() {

    override fun initialState(): State = State(
        blogPosts = discoverRepository.getCachedBlogPosts().toPersistentList()
    )

    data class State(
        val blogPosts: ImmutableList<BlogPost> = persistentListOf()
    ) : UiState
}
